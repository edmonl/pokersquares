
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import util.Linear;

/**
 *
 * @author Meng
 */
public final class MengYaXiPlayer implements PokerSquaresPlayer {

    public boolean verbose = false;
    private PokerSquaresPointSystem pointSystem;
    private final Board board = new Board();
    private final DeckTracker deckTracker = new DeckTracker();
    private final Strategy strategy = new Strategy(board, deckTracker);
    private final CellCandidateEvaluator candidateEvaluator = new CellCandidateEvaluator(board, deckTracker, strategy);
    private final ExecutorCompletionService executor = new ExecutorCompletionService(Executors.newWorkStealingPool());
    private final List<CellCandidateEvaluator> workers = new ArrayList<>();

    @Override
    public void setPointSystem(final PokerSquaresPointSystem system, final long millis) {
        pointSystem = system; // American system is guaranteed.
        candidateEvaluator.setPointSystem(system);
    }

    @Override
    public void init() {
        candidateEvaluator.clear();
        strategy.verbose = this.verbose;
        workers.clear();
        int n = Runtime.getRuntime().availableProcessors();
        n = n > 2 ? n - 1 : 0;
        while (workers.size() < n) {
            final CellCandidateEvaluator worker = new CellCandidateEvaluator();
            worker.setPointSystem(pointSystem);
            workers.add(worker);
        }
        System.gc();
    }

    @Override
    public int[] getPlay(final Card card, long millisRemaining) {
        deckTracker.deal(card);
        if (verbose) {
            System.out.println(String.format("Get card \"%s\". Remaining seconds: %.2f", card, millisRemaining / 1000.0));
        }
        if (strategy.play(card)) {
            final Board.Play p = board.getLastPlay();
            if (verbose) {
                System.out.println(String.format("Play \"%s\" at (%d, %d)", card, p.row + 1, p.col + 1));
            }
            return new int[]{p.row, p.col};
        }
        final List<CellCandidate> candidates = strategy.getCandidates();
        if (verbose) {
            System.out.print(candidates.size() + " candidates: ");
            candidates.stream().forEach((c) -> {
                System.out.print(String.format(" (%d,%d: q=%.2f)", c.row + 1, c.col + 1, c.quality));
            });
            System.out.println();
        }
        CellCandidate winner;
        final int contingency = 50 * board.numberOfEmptyCells() - 10;
        strategy.verbose = false;
        if (millisRemaining <= contingency + 10) {
            if (verbose) {
                System.out.println("No time for trials.");
            }
            winner = candidates.get(0);
        } else {
            millisRemaining -= contingency;
            winner = monteCarloGuess(card, candidates,
                Math.max((int) Math.floor(millisRemaining * (-0.04 * board.numberOfEmptyCells() + 1.08)), 1));
        }
        strategy.verbose = this.verbose;
        board.putCard(card, winner.row, winner.col);
        if (verbose) {
            System.out.println(String.format("Play \"%s\" at (%d, %d)", card, winner.row + 1, winner.col + 1));
        }
        return new int[]{winner.row, winner.col};
    }

    @Override
    public String getName() {
        return "MengYaXi Player";
    }

    public static void main(final String[] args) {
        final MengYaXiPlayer player = new MengYaXiPlayer();
        player.verbose = true;
        final PokerSquares game = new PokerSquares(player, PokerSquaresPointSystem.getAmericanPointSystem());
        game.play(new Scanner(System.in));
    }

    private CellCandidate monteCarloGuess(final Card card, final List<CellCandidate> candidates, final long millisRemaining) {
        if (verbose) {
            System.out.println(String.format("Time Quota: %.2f seconds", millisRemaining / 1000.0));
        }
        final long startMillis = System.currentTimeMillis();
        int shuffles;
        if (workers.isEmpty()) {
            shuffles = singleThreadMonteCarlo(card, candidates, startMillis, millisRemaining);
        } else {
            shuffles = multiThreadMonteCarlo(card, candidates, startMillis, millisRemaining);
        }
        for (final CellCandidate c : candidates) {
            c.score /= shuffles;
        }
        final CellCandidate scoreWinner = Collections.max(candidates, CellCandidate.SCORE_COMPARATOR);
        final CellCandidate qualityWinner = Collections.max(candidates, CellCandidate.QUALITY_COMPARATOR);
        CellCandidate winner = scoreWinner;
        if (qualityWinner != scoreWinner && qualityWinner.quality / scoreWinner.quality >= 2
            && scoreWinner.score - qualityWinner.score < 2) {
            winner = qualityWinner;
        }
        if (verbose) {
            System.out.println(String.format("%d shuffles completed within %.2f seconds",
                shuffles, (System.currentTimeMillis() - startMillis) / 1000.0));
            System.out.print(candidates.size() + " candidates left:");
            candidates.stream().forEach((c) -> {
                System.out.print(String.format(" (%d,%d: q=%.2f, s=%.2f)", c.row + 1, c.col + 1, c.quality, c.score));
            });
            System.out.println();
        }
        return winner;
    }

    private int singleThreadMonteCarlo(final Card card, final List<CellCandidate> candidates, final long startMillis, final long millisRemaining) {
        final List<Card> cards = deckTracker.getCards();
        int shuffles = 0;
        do {
            final List<CellCandidate> resultCandidates = new ArrayList<>(candidates.size());
            for (final CellCandidate c : candidates) {
                resultCandidates.add(new CellCandidate(c.row, c.col));
            }
            candidateEvaluator.evaluate(card, resultCandidates, cards,
                shuffles < 500
                    ? millisRemaining - (System.currentTimeMillis() - startMillis) / (500 - shuffles)
                    : millisRemaining - (System.currentTimeMillis() - startMillis)
            );
            ++shuffles;
            prepareNextShuffle(candidates, resultCandidates);
        } while (System.currentTimeMillis() - startMillis < millisRemaining && candidates.size() > 1);
        return shuffles;
    }

    private int multiThreadMonteCarlo(final Card card, List<CellCandidate> candidates, final long startMillis, final long millisRemaining) {
        final List<Card> cards = deckTracker.getCards();
        int shuffles = 0;
        do {
            candidateEvaluator.evaluate(card, candidates, cards.subList(0, board.numberOfEmptyCells() - 1),
                shuffles < 500 ? millisRemaining - (System.currentTimeMillis() - startMillis) / (500 - shuffles) : millisRemaining - (System.currentTimeMillis() - startMillis));
            ++shuffles;
            try {
                prepareNextShuffle(candidates, candidateEvaluator.call());
            } catch (final Exception ex) {
                ex.printStackTrace(System.out);
                System.exit(-1);
            }
        } while (System.currentTimeMillis() - startMillis < millisRemaining && candidates.size() > 1);
        return shuffles;
    }

    private void prepareNextShuffle(final List<CellCandidate> candidates, final List<CellCandidate> resultCandidates) {
        double maxRoundScore = resultCandidates.get(0).score;
        double minRoundScore = maxRoundScore;
        for (int i = 0; i < candidates.size(); ++i) {
            final double score = resultCandidates.get(i).score;
            candidates.get(i).score += score;
            if (score > maxRoundScore) {
                maxRoundScore = score;
            } else if (score < minRoundScore) {
                minRoundScore = score;
            }
        }
        if (maxRoundScore > minRoundScore) {
            final Linear awardFactor = new Linear(2, 0.0005, 5, 0.005);
            final double award = awardFactor.apply((double) candidates.size());
            final Linear linear = new Linear(minRoundScore, -award, maxRoundScore, award);
            for (int i = 0; i < candidates.size(); ++i) {
                candidates.get(i).quality += linear.apply(resultCandidates.get(i).score);
            }
            final double maxQuality = Collections.max(candidates, CellCandidate.QUALITY_COMPARATOR).quality;
            candidates.stream().forEach((c) -> {
                c.quality /= maxQuality;
            });
            candidates.sort(CellCandidate.REVERSE_QUALITY_COMPARATOR);
            while (candidates.size() > 1 && candidates.get(candidates.size() - 1).quality <= 0) {
                candidates.remove(candidates.size() - 1);
            }
        }
    }
}
