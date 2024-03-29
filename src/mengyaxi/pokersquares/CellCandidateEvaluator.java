package mengyaxi.pokersquares;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import mengyaxi.pokersquares.board.Board;
import mengyaxi.util.Linear;

/**
 *
 * @author Meng
 */
final class CellCandidateEvaluator implements Callable<Integer> {

    private static final Linear AWARD_FACTOR = new Linear(2, 0.001, 6, 0.01);

    private final Board board;
    private final DeckTracker deck;
    private final Strategy strategy;
    private Card card;
    private List<CellCandidate> candidates;
    private List<Card> cards;
    private long workerDeadline;
    private int shuffles;
    private final CellCandidate[] candidateTable = new CellCandidate[CellCandidate.MAX_NUMBER];
    private final boolean workerMode;
    private boolean stopped;

    public CellCandidateEvaluator(final Board board, final DeckTracker deck) {
        this.board = board;
        this.deck = deck;
        strategy = new Strategy(board, deck);
        workerMode = false;
    }

    public CellCandidateEvaluator() {
        board = new Board();
        deck = new DeckTracker();
        strategy = new Strategy(board, deck);
        workerMode = true;
    }

    public void clear() {
        board.clear();
        deck.clear();
        strategy.clear();
        card = null;
        candidates = null;
        cards = null;
        workerDeadline = 0;
        shuffles = 0;
        Arrays.fill(candidateTable, null);
        stopped = false;
    }

    public void setCandidates(final List<CellCandidate> candidates) {
        this.candidates = candidates;
    }

    public List<CellCandidate> getCandidates() {
        return candidates;
    }

    public int getShuffles() {
        return shuffles;
    }

    public void resetShuffles() {
        shuffles = 0;
    }

    public void setStop() {
        stopped = true;
    }

    public void initWorker(final Board board, final DeckTracker deck,
        final Card card, final List<CellCandidate> candidates, final List<Card> cards, final long deadline) {
        this.board.copyFrom(board);
        this.deck.copyFrom(deck);
        this.card = card;
        this.candidates = new ArrayList<>(candidates.size());
        for (final CellCandidate c : candidates) {
            this.candidates.add(new CellCandidate(c.row, c.col));
        }
        this.cards = new ArrayList<>(cards);
        workerDeadline = deadline;
        shuffles = 0;
        stopped = false;
    }

    public void evaluate(final Card card, final List<Card> cards) {
        Collections.shuffle(cards);
        final List<Card> remainingCards = cards.subList(0, board.numberOfEmptyCells() - 1);
        deck.deal(card);
        synchronized (this) {
            if (candidates.size() > 1) {
                for (final CellCandidate c : candidates) {
                    board.putCard(card, c.row, c.col);
                    c.score = finishPlay(remainingCards);
                    board.retractLastPlay();
                }
                ++shuffles;
                finishShuffle();
            }
        }
        deck.putBack(card);
    }

    @Override
    public Integer call() throws Exception {
        do {
            evaluate(card, cards);
        } while (System.currentTimeMillis() < workerDeadline && candidates.size() > 1 && !stopped);
        return shuffles;
    }

    public void syncCandidates(final List<CellCandidate> sumCans) {
        Arrays.fill(candidateTable, null);
        for (final CellCandidate c : sumCans) {
            candidateTable[c.id] = c;
        }
        if (sumCans.size() < candidates.size()) {
            final List<CellCandidate> newCans = new ArrayList<>(sumCans.size());
            synchronized (this) {
                for (final CellCandidate c : candidates) {
                    final CellCandidate tc = candidateTable[c.id];
                    if (tc != null) {
                        tc.totalScore += c.totalScore;
                        c.totalScore = 0;
                        tc.quality += c.quality;
                        c.quality = 0.0;
                        newCans.add(c);
                    }
                }
                candidates = newCans;
            }
        } else {
            synchronized (this) {
                for (final CellCandidate c : candidates) {
                    final CellCandidate tc = candidateTable[c.id];
                    tc.totalScore += c.totalScore;
                    c.totalScore = 0;
                    tc.quality += c.quality;
                    c.quality = 0.0;
                }
            }
        }
    }

    private void finishShuffle() {
        int total = 0;
        for (final CellCandidate c : candidates) {
            c.totalScore += c.score;
            c.score = 0;
            total += c.totalScore;
        }
        final double avg = (double) total / candidates.size();
        final double award = AWARD_FACTOR.apply((double) candidates.size());
        if (workerMode) {
            for (final CellCandidate c : candidates) {
                if (c.totalScore > avg) {
                    c.quality += award;
                } else if (c.totalScore < avg) {
                    c.quality -= award;
                }
            }
        } else {
            double maxQuality = -Double.MAX_VALUE;
            for (final CellCandidate c : candidates) {
                if (c.totalScore > avg) {
                    c.quality += award;
                } else if (c.totalScore < avg) {
                    c.quality -= award;
                }
                maxQuality = Double.max(maxQuality, c.quality);
            }
            for (final CellCandidate c : candidates) {
                c.quality /= maxQuality;
            }
            if (candidates.size() > 1) {
                candidates.removeIf(c -> c.quality <= 0.01);
            }
        }
    }

    private int finishCandidates(final Card card, final List<CellCandidate> candidates, final List<Card> cards) {
        int maxScore = 0;
        deck.deal(card);
        for (final CellCandidate c : candidates) {
            board.putCard(card, c.row, c.col);
            maxScore = Integer.max(maxScore, finishPlay(cards));
            board.retractLastPlay();
        }
        deck.putBack(card);
        return maxScore;
    }

    private int finishPlay(final List<Card> cards) {
        for (int i = 0; i < cards.size(); ++i) {
            final Card c = cards.get(i);
            strategy.play(c);
            List<CellCandidate> cans = strategy.getCandidates();
            if (cans.size() == 1) {
                final CellCandidate can = cans.get(0);
                deck.deal(c);
                board.putCard(c, can.row, can.col);
                continue;
            }
            final int remainingCards = cards.size() - i;
            if (remainingCards >= 6) {
                if (cans.get(1).quality < 0.97) {
                    final CellCandidate can = cans.get(0);
                    deck.deal(c);
                    board.putCard(c, can.row, can.col);
                    continue;
                }
                if (remainingCards >= 7) {
                    if (cans.size() > 2) {
                        cans = new ArrayList<>(cans.subList(0, 2));
                    }
                } else {
                    int size = 4;
                    if (cans.size() >= 3 && cans.get(2).quality < 0.97) {
                        size = 2;
                    } else if (cans.size() >= 4 && cans.get(3).quality < 0.97) {
                        size = 3;
                    }
                    if (size < cans.size()) {
                        cans = new ArrayList<>(cans.subList(0, size));
                    }
                }
            }
            final int score = finishCandidates(c, cans, cards.subList(i + 1, cards.size()));
            retract(i);
            return score;
        }
        final int score = board.getPokerHandScore();
        retract(cards.size());
        return score;
    }

    private void retract(int steps) {
        for (; steps > 0; --steps) {
            deck.putBack(board.retractLastPlay().card);
        }
    }
}
