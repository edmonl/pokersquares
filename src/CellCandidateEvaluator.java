
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import util.Linear;

/**
 *
 * @author Meng
 */
final class CellCandidateEvaluator implements Callable<CellCandidateEvaluator> {

    private static final Linear SAMPLE_TIME = new Linear(6, 200, 20, 1000);

    private final Board board;
    private final DeckTracker deck;
    private final Strategy strategy;
    private Card card;
    private List<CellCandidate> candidates;
    private List<Card> cards;
    private long millis;

    public CellCandidateEvaluator(final Board board, final DeckTracker deck, final Strategy strategy) {
        this.board = board;
        this.deck = deck;
        this.strategy = strategy;
    }

    public CellCandidateEvaluator() {
        board = new Board();
        deck = new DeckTracker();
        strategy = new Strategy(board, deck);
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
        millis = 0;
    }

    public void setCandidates(final List<CellCandidate> candidates) {
        this.candidates = new ArrayList<>(candidates.size());
        for (final CellCandidate c : candidates) {
            this.candidates.add(new CellCandidate(c.row, c.col));
        }
    }

    public List<CellCandidate> getCandidates() {
        return candidates;
    }

    public void setTimeQuota(final long millisRemaining) {
        millis = millisRemaining;
    }

    public void copyStateFrom(final Board board, final DeckTracker deck,
        final Card card, final List<CellCandidate> candidates, final List<Card> cards) {
        this.board.copyFrom(board);
        this.deck.copyFrom(deck);
        this.card = card;
        setCandidates(candidates);
        this.cards = new ArrayList<>(cards);
    }

    public void evaluate(final Card card, final List<Card> cards, final long millisRemaining) {
        final long deadline = System.currentTimeMillis() + millisRemaining;
        shuffle(cards);
        int numberOfCandidates = 0;
        for (final CellCandidate c : candidates) {
            if (c != null) {
                ++numberOfCandidates;
            }
        }
        int numberOfProcessed = 0;
        final List<Card> remainingCards = cards.subList(0, board.numberOfEmptyCells() - 1);
        for (int i = 0; i < candidates.size(); ++i) {
            final CellCandidate c = candidates.get(i);
            if (c != null) {
                board.putCard(card, c.row, c.col);
                c.score = fakePlay(remainingCards, (deadline - System.currentTimeMillis()) / (numberOfCandidates - numberOfProcessed));
                board.retractLastPlay();
                ++numberOfProcessed;
            }
        }
    }

    @Override
    public CellCandidateEvaluator call() throws Exception {
        evaluate(card, cards, millis);
        return this;
    }

    private void testCandidates(final Card card, final List<CellCandidate> candidates,
        final List<Card> cards, final long millisRemaining) {
        final long deadline = System.currentTimeMillis() + millisRemaining;
        for (int i = 0; i < candidates.size(); ++i) {
            final CellCandidate c = candidates.get(i);
            board.putCard(card, c.row, c.col);
            c.score = fakePlay(cards, (deadline - System.currentTimeMillis()) / (candidates.size() - i));
            board.retractLastPlay();
        }
    }

    private double fakePlay(final List<Card> cards, final long millisRemaining) {
        final long deadline = System.currentTimeMillis() + millisRemaining;
        for (int i = 0; i < cards.size(); ++i) {
            final Card c = cards.get(i);
            deck.deal(c);
            if (!strategy.play(c)) {
                final List<CellCandidate> cans = strategy.getCandidates();
                final int remainingCards = cards.size() - i - 1;
                if (remainingCards > 5 && System.currentTimeMillis() + SAMPLE_TIME.apply((double) remainingCards) > deadline) {
                    sampleCandidates(c, cans, cards.subList(i + 1, cards.size()), deadline - System.currentTimeMillis());
                } else {
                    testCandidates(c, cans, cards.subList(i + 1, cards.size()), deadline - System.currentTimeMillis());
                }
                deck.putBack(c);
                retract(i);
                return Collections.max(cans, CellCandidate.SCORE_COMPARATOR).score;
            }
        }
        final int score = board.getScore(strategy.getPointSystem());
        retract(cards.size());
        return score;
    }

    private void sampleCandidates(final Card card, final List<CellCandidate> candidates,
        final List<Card> cards, final long millisRemaining) {
        final long deadline = System.currentTimeMillis() + millisRemaining;
        int numberOfSamples = 0;
        do {
            for (final CellCandidate c : candidates) {
                board.putCard(card, c.row, c.col);
                final int score = randomPlay(cards);
                if (score > c.score) {
                    c.score = score;
                }
                board.retractLastPlay();
            }
            ++numberOfSamples;
        } while (numberOfSamples < 100 || System.currentTimeMillis() < deadline);
    }

    private int randomPlay(final List<Card> cards) {
        for (final Card c : cards) {
            deck.deal(c);
            if (!strategy.play(c)) {
                final CellCandidate can = selectCandidateRandomly(strategy.getCandidates());
                board.putCard(c, can.row, can.col);
            }
        }
        final int score = board.getScore(strategy.getPointSystem());
        retract(cards.size());
        return score;
    }

    private void retract(int steps) {
        for (; steps > 0; --steps) {
            deck.putBack(board.retractLastPlay().card);
        }
    }

    private static CellCandidate selectCandidateRandomly(final List<CellCandidate> candidates) {
        // bias to better ones
        double sum = 0.0;
        for (final CellCandidate c : candidates) {
            sum += c.quality;
        }
        final double r = Math.random() * sum;
        sum = 0.0;
        for (final CellCandidate c : candidates) {
            sum += c.quality;
            if (r <= sum) {
                return c;
            }
        }
        return candidates.get(candidates.size() - 1);
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
