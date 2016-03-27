
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

/**
 *
 * @author Meng
 */
final class CellCandidateEvaluator implements Callable<List<CellCandidate>> {

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

    public void evaluateInPlace(final Card card, final List<CellCandidate> candidates,
        final List<Card> cards, final long millisRemaining) {
        this.card = card;
        this.candidates = candidates;
        this.cards = cards;
        this.millis = millisRemaining;
    }

    public void evaluate(final Board board, final DeckTracker deck, final Card card,
        final List<CellCandidate> candidates, final List<Card> cards, final long millisRemaining) {
        this.board.copyFrom(board);
        this.deck.copyFrom(deck);
        this.card = card;
        this.candidates = new ArrayList<>(candidates);
        this.cards = cards;
        this.millis = millisRemaining;
    }

    @Override
    public List<CellCandidate> call() throws Exception {
        testCandidates(card, candidates, cards, millis);
        return candidates;
    }

    private void testCandidates(final Card card, final List<CellCandidate> candidates,
        final List<Card> cards, final long millisRemaining) {
        final long startMillis = System.currentTimeMillis();
        for (int i = 0; i < candidates.size(); ++i) {
            final CellCandidate c = candidates.get(i);
            board.putCard(card, c.row, c.col);
            c.score = fakePlay(cards, (millisRemaining - (System.currentTimeMillis() - startMillis)) / (candidates.size() - i));
            board.retractLastPlay();
        }
    }

    private int fakePlay(final List<Card> cards, final long millisRemaining) {
        final long startMillis = System.currentTimeMillis();
        for (int i = 0; i < cards.size(); ++i) {
            final Card c = cards.get(i);
            deck.deal(c);
            if (!strategy.play(c)) {
                final List<CellCandidate> cans = strategy.getCandidates();
                if (System.currentTimeMillis() - startMillis - 100 <= millisRemaining && cards.size() - i > 5) {
                    sampleCandidates(100, c, cans, cards.subList(i + 1, cards.size()));
                } else {
                    testCandidates(c, cans, cards.subList(i + 1, cards.size()), millisRemaining - (System.currentTimeMillis() - startMillis));
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

    private void sampleCandidates(final int times, final Card card, final List<CellCandidate> candidates, final List<Card> cards) {
        for (int i = 0; i < times; ++i) {
            final CellCandidate c = selectCandidateRandomly(candidates);
            board.putCard(card, c.row, c.col);
            final int score = randomPlay(cards);
            if (score > c.score) {
                c.score = score;
            }
            board.retractLastPlay();
        }
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
        final double r = Math.random();
        double sum = 0.0;
        for (final CellCandidate c : candidates) {
            sum += c.quality;
            if (r <= sum) {
                return c;
            }
        }
        return candidates.isEmpty() ? null : candidates.get(candidates.size() - 1);
    }
}
