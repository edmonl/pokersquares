
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import util.Linear;

/**
 *
 * @author Meng
 */
final class CellCandidateEvaluator implements Callable<Integer> {

    private static final Linear AWARD_FACTOR = new Linear(2, 0.0015, 6, 0.02);
    private static final int MIN_SAMPLING_TIMES = 100;

    private final Board board;
    private final DeckTracker deck;
    private final Strategy strategy;
    private Card card;
    private List<CellCandidate> candidates;
    private List<Card> cards;
    private int targetShuffles;
    private long workerDeadline;
    private int shuffles;
    private final CellCandidate[] candidateTable = new CellCandidate[CellCandidate.MAX_NUMBER];
    private final boolean workerMode;

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

    public void setPointSystem(final PokerSquaresPointSystem pointSystem) {
        strategy.setPointSystem(pointSystem);
    }

    public void clear() {
        board.clear();
        deck.clear();
        strategy.clear();
        card = null;
        candidates = null;
        cards = null;
        targetShuffles = 0;
        workerDeadline = 0;
        shuffles = 0;
        Arrays.fill(candidateTable, null);
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

    public void initWorker(final Board board, final DeckTracker deck,
        final Card card, final List<CellCandidate> candidates, final List<Card> cards,
        final int targetShuffles, final long deadline) {
        this.board.copyFrom(board);
        this.deck.copyFrom(deck);
        this.card = card;
        this.candidates = new ArrayList<>(candidates.size());
        for (final CellCandidate c : candidates) {
            this.candidates.add(new CellCandidate(c.row, c.col));
        }
        this.cards = new ArrayList<>(cards);
        workerDeadline = deadline;
        this.targetShuffles = targetShuffles;
        shuffles = 0;
    }

    public void evaluate(final Card card, final List<Card> cards, final long deadline) {
        shuffle(cards);
        final List<Card> remainingCards = cards.subList(0, board.numberOfEmptyCells() - 1);
        synchronized (this) {
            if (candidates.size() > 1) {
                deck.deal(card);
                for (int i = 0; i < candidates.size(); ++i) {
                    final CellCandidate c = candidates.get(i);
                    board.putCard(card, c.row, c.col);
                    c.score = finishPlay(remainingCards);
                    board.retractLastPlay();
                }
                ++shuffles;
                deck.putBack(card);
                finishShuffle();
            }
        }
    }

    @Override
    public Integer call() throws Exception {
        do {
            final long now = System.currentTimeMillis();
            evaluate(card, cards,
                shuffles < targetShuffles
                    ? (workerDeadline - now) / (targetShuffles - shuffles) + now
                    : workerDeadline);
        } while (System.currentTimeMillis() < workerDeadline && candidates.size() > 1);
        return shuffles;
    }

    public void syncCandidates(final List<CellCandidate> sumCans) {
        Arrays.fill(candidateTable, null);
        for (final CellCandidate c : sumCans) {
            candidateTable[c.getId()] = c;
        }
        if (sumCans.size() < candidates.size()) {
            final List<CellCandidate> newCans = new ArrayList<>(sumCans.size());
            synchronized (this) {
                for (final CellCandidate c : candidates) {
                    final CellCandidate tc = candidateTable[c.getId()];
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
                    final CellCandidate tc = candidateTable[c.getId()];
                    if (tc != null) {
                        tc.totalScore += c.totalScore;
                        c.totalScore = 0;
                        tc.quality += c.quality;
                        c.quality = 0.0;
                    }
                }
            }
        }
    }

    private void finishShuffle() {
        int total = 0;
        for (final CellCandidate c : candidates) {
            c.totalScore += c.score;
            total += c.score;
        }
        final double avg = (double) total / candidates.size();
        final double award = AWARD_FACTOR.apply((double) candidates.size());
        if (workerMode) {
            for (final CellCandidate c : candidates) {
                if (c.score > avg + 1e-6) {
                    c.quality += award;
                } else if (c.score < avg - 1e-6) {
                    c.quality -= award;
                }
                c.score = 0;
            }
        } else {
            double max = Double.MIN_VALUE;
            for (final CellCandidate c : candidates) {
                if (c.score > avg + 1e-6) {
                    c.quality += award;
                } else if (c.score < avg - 1e-6) {
                    c.quality -= award;
                }
                c.score = 0;
                max = Double.max(max, c.quality);
            }
            Collections.sort(candidates, CellCandidate.REVERSE_QUALITY_COMPARATOR);
            while (candidates.get(candidates.size() - 1).quality <= 0.1) {
                candidates.remove(candidates.size() - 1);
            }
            for (final CellCandidate c : candidates) {
                if (c != null) {
                    c.quality /= max;
                }
            }
        }
    }

    private int finishCandidates(final Card card, final List<CellCandidate> candidates, final List<Card> cards) {
        deck.deal(card);
        for (int i = 0; i < candidates.size(); ++i) {
            final CellCandidate c = candidates.get(i);
            board.putCard(card, c.row, c.col);
            c.score = finishPlay(cards);
            board.retractLastPlay();
        }
        deck.putBack(card);
        return Collections.max(candidates, CellCandidate.SCORE_COMPARATOR).score;
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
            if (cards.size() - i > 5) {
                if (cans.get(1).quality < 0.98) {
                    final CellCandidate can = cans.get(0);
                    deck.deal(c);
                    board.putCard(c, can.row, can.col);
                    continue;
                }
                cans = new ArrayList<>(cans.subList(0, 2));
            }
            final int score = finishCandidates(c, cans, cards.subList(i + 1, cards.size()));
            retract(i);
            return score;
        }
        final int score = board.getPokerHandScore(strategy.getPointSystem());
        retract(cards.size());
        return score;
    }

    private void retract(int steps) {
        for (; steps > 0; --steps) {
            deck.putBack(board.retractLastPlay().card);
        }
    }

    private static void shuffle(final List<Card> cards) {
        for (int i = cards.size() - 1; i > 0; --i) {
            final int n = (int) Math.floor(Math.random() * (i + 1));
            final Card tmpi = cards.get(i);
            cards.set(i, cards.get(n));
            cards.set(n, tmpi);
        }
    }
}
