
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import util.Linear;

/**
 *
 * @author Meng
 */
public final class MengYaXiPlayer implements PokerSquaresPlayer {

    private static final Linear QUOTA = new Linear(2, 1, 15, 0.6);

    public boolean verbose = false;
    private PokerSquaresPointSystem pointSystem;
    private final Board board = new Board();
    private final DeckTracker deckTracker = new DeckTracker();
    private final Strategy strategy = new Strategy(board, deckTracker);
    private final CellCandidateEvaluator candidateEvaluator = new CellCandidateEvaluator(board, deckTracker, strategy);
    private final CompletionService<CellCandidateEvaluator> executor = new ExecutorCompletionService<>(Executors.newWorkStealingPool());
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
        final int contingency = 40 * (board.numberOfEmptyCells() - 1);
        strategy.verbose = false;
        if (millisRemaining < contingency) {
            if (verbose) {
                System.out.println("No time for trials.");
            }
            winner = candidates.get(0);
        } else {
            millisRemaining -= contingency;
            final long quota = Math.max((long) Math.floor(millisRemaining * QUOTA.apply((double) board.numberOfEmptyCells())), 1);
            winner = monteCarloGuess(card, candidates, quota);
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
        final long deadline = startMillis + millisRemaining;
        int shuffles;
        if (workers.isEmpty()) {
            shuffles = singleThreadMonteCarlo(card, candidates, deadline);
        } else {
            shuffles = multiThreadMonteCarlo(card, candidates, deadline);
        }
        candidates.removeIf(c -> c == null);
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

    private int singleThreadMonteCarlo(final Card card, final List<CellCandidate> candidates, final long deadline) {
        final List<Card> cards = deckTracker.getCards();
        int shuffles = 0;
        int numberOfCandidates;
        candidateEvaluator.setCandidates(candidates);
        do {
            candidateEvaluator.evaluate(card, cards,
                shuffles < 500
                    ? (deadline - System.currentTimeMillis()) / (500 - shuffles)
                    : deadline - System.currentTimeMillis()
            );
            ++shuffles;
            numberOfCandidates = prepareNextShuffle(candidates, candidateEvaluator.getCandidates());
        } while (System.currentTimeMillis() < deadline && numberOfCandidates > 1);
        return shuffles;
    }

    private int multiThreadMonteCarlo(final Card card, List<CellCandidate> candidates, final long deadline) {
        final List<Card> cards = deckTracker.getCards();
        int shuffles = 0;
        for (final CellCandidateEvaluator worker : workers) {
            worker.copyStateFrom(board, deckTracker, card, candidates, cards);
            worker.setTimeQuota(shuffles < 500
                ? (deadline - System.currentTimeMillis()) * workers.size() / (500 - shuffles)
                : deadline - System.currentTimeMillis());
            executor.submit(worker);
        }
        int numberOfWorkers = workers.size();
        try {
            int numberOfCandidates;
            while (numberOfWorkers > 0) {
                final CellCandidateEvaluator worker = executor.take().get();
                --numberOfWorkers;
                ++shuffles;
                numberOfCandidates = prepareNextShuffle(candidates, worker.getCandidates());
                if (numberOfCandidates <= 1 || System.currentTimeMillis() >= deadline) {
                    continue;
                }
                worker.setTimeQuota(shuffles < 500
                    ? (deadline - System.currentTimeMillis()) * workers.size() / (500 - shuffles)
                    : deadline - System.currentTimeMillis());
                executor.submit(worker);
                ++numberOfWorkers;
            }
        } catch (final InterruptedException | ExecutionException ex) {
            ex.printStackTrace(System.out);
            System.exit(-1);
        }
        return shuffles;
    }

    private int prepareNextShuffle(final List<CellCandidate> candidates, final List<CellCandidate> resultCandidates) {
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;
        int numberOfCandidates = 0;
        for (int i = 0; i < candidates.size(); ++i) {
            final CellCandidate c = candidates.get(i);
            if (c == null) {
                resultCandidates.set(i, null);
            } else {
                final double score = resultCandidates.get(i).score;
                c.score += score;
                max = Double.max(max, score);
                min = Double.min(min, score);
                ++numberOfCandidates;
            }
        }
        if (max > min) {
            final Linear awardFactor = new Linear(2, 0.002, 5, 0.01);
            final double award = awardFactor.apply((double) numberOfCandidates);
            final Linear linear = new Linear(min, -award, max, award);
            max = Double.MIN_VALUE;
            for (int i = 0; i < candidates.size(); ++i) {
                final CellCandidate c = candidates.get(i);
                if (c != null) {
                    c.quality += linear.apply(resultCandidates.get(i).score);
                    if (c.quality < 0) {
                        candidates.set(i, null);
                        resultCandidates.set(i, null);
                        --numberOfCandidates;
                    } else {
                        max = Double.max(max, c.quality);
                    }
                }
            }
            for (final CellCandidate c : candidates) {
                if (c != null) {
                    c.quality /= max;
                }
            }
        }
        return numberOfCandidates;
    }
}
