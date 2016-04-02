package mengyaxi.pokersquares;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import mengyaxi.pokersquares.board.Board;
import mengyaxi.util.Linear;

/**
 *
 * @author Meng
 */
public class PokerSquaresPlayer {

    private static final Linear QUOTA = new Linear(2, 1, 15, 0.4);
    private static final int MAX_SHUFFLES = 10000;

    public boolean verbose = false;
    public boolean parallel = true;

    private final Board board = new Board();
    private final DeckTracker deckTracker = new DeckTracker();
    private final Strategy strategy = new Strategy(board, deckTracker);
    private final CellCandidateEvaluator candidateEvaluator = new CellCandidateEvaluator(board, deckTracker);
    private final ExecutorService executor = Executors.newWorkStealingPool();
    private final List<CellCandidateEvaluator> workers = new ArrayList<>();

    public final void init() {
        candidateEvaluator.clear();
        strategy.clear();
        strategy.verbose = this.verbose;
        workers.clear();
        if (parallel) {
            int n = Runtime.getRuntime().availableProcessors();
            n = n > 2 ? n - 1 : 0;
            while (workers.size() < n) {
                final CellCandidateEvaluator worker = new CellCandidateEvaluator();
                workers.add(worker);
            }
        }
        System.gc();
    }

    public final String getName() {
        return "MengYaXi Poker Squares Player";
    }

    protected final int[] getPlay(final Card card, long millisRemaining) {
        if (verbose) {
            System.out.println(String.format("Get card \"%s\". Remaining seconds: %.2f", card, millisRemaining / 1000.0));
        }
        strategy.play(card);
        final List<CellCandidate> cans = strategy.getCandidates();
        CellCandidate winner;
        if (cans.size() == 1) {
            winner = cans.get(0);
        } else {
            if (verbose) {
                System.out.print(cans.size() + " candidates: ");
                cans.stream().forEach((c) -> {
                    System.out.print(String.format(" (%d,%d: q=%.2f)", c.row + 1, c.col + 1, c.quality));
                });
                System.out.println();
            }
            final int contingency = 50 * board.numberOfEmptyCells() - 90;
            strategy.verbose = false;
            if (millisRemaining < contingency) {
                if (verbose) {
                    System.out.println("No time for trials.");
                }
                winner = cans.get(0);
            } else {
                millisRemaining -= contingency;
                final long quota = Math.max((long) Math.floor(millisRemaining * QUOTA.apply((double) board.numberOfEmptyCells())), 1);
                winner = monteCarloGuess(card, cans, quota);
            }
            strategy.verbose = this.verbose;
        }
        deckTracker.deal(card);
        board.putCard(card, winner.row, winner.col);
        if (verbose) {
            System.out.println(String.format("Play \"%s\" at (%d, %d)", card, winner.row + 1, winner.col + 1));
        }
        return new int[]{winner.row, winner.col};
    }

    private CellCandidate monteCarloGuess(final Card card, final List<CellCandidate> candidates, final long millisRemaining) {
        if (verbose) {
            System.out.println(String.format("Time Quota: %.2f seconds", millisRemaining / 1000.0));
        }
        final long startMillis = System.currentTimeMillis();
        final long deadline = startMillis + millisRemaining;
        int shuffles;
        deckTracker.deal(card);
        final List<Card> cards = deckTracker.getCards();
        deckTracker.putBack(card);
        if (parallel && workers.size() > 1 && board.numberOfEmptyCells() > 5) {
            shuffles = multiThreadMonteCarlo(card, cards, candidates, deadline);
        } else {
            shuffles = singleThreadMonteCarlo(card, cards, candidates, deadline);
        }
        final CellCandidate winner = Collections.max(candidates, CellCandidate.TOTAL_SCORE_COMPARATOR);
        if (verbose) {
            System.out.println(String.format("%d shuffles completed within %.2f seconds",
                shuffles, (System.currentTimeMillis() - startMillis) / 1000.0));
            System.out.print(candidates.size() + " candidates left:");
            candidates.stream().forEach((c) -> {
                System.out.print(String.format(" (%d,%d: q=%.2f, s=%.2f)", c.row + 1, c.col + 1, c.quality, (double) c.totalScore / shuffles));
            });
            System.out.println();
        }
        return winner;
    }

    private int singleThreadMonteCarlo(final Card card, final List<Card> cards, final List<CellCandidate> candidates, final long deadline) {
        candidateEvaluator.resetShuffles();
        candidateEvaluator.setCandidates(candidates);
        do {
            candidateEvaluator.evaluate(card, cards);
        } while (candidateEvaluator.getShuffles() < MAX_SHUFFLES && System.currentTimeMillis() < deadline && candidates.size() > 1);
        return candidateEvaluator.getShuffles();
    }

    private int multiThreadMonteCarlo(final Card card, final List<Card> cards, final List<CellCandidate> candidates, final long deadline) {
        if (verbose) {
            System.out.println(String.format("%d workers are working", workers.size()));
        }
        final List<Future<Integer>> results = new ArrayList<>(workers.size());
        for (final CellCandidateEvaluator worker : workers) {
            worker.initWorker(board, deckTracker, card, candidates, cards, deadline);
            results.add(executor.submit(worker));
        }
        int shuffles;
        do {
            for (final CellCandidateEvaluator worker : workers) {
                worker.syncCandidates(candidates);
            }
            shuffles = 0;
            for (final CellCandidateEvaluator worker : workers) {
                shuffles += worker.getShuffles();
            }
            final double maxQuality = Collections.max(candidates, CellCandidate.QUALITY_COMPARATOR).quality;
            for (final CellCandidate c : candidates) {
                c.quality /= maxQuality;
            }
            if (candidates.size() > 1) {
                candidates.removeIf(c -> c.quality <= 0.01);
            }
        } while (shuffles < MAX_SHUFFLES && System.currentTimeMillis() < deadline && candidates.size() > 1);
        try {
            for (final CellCandidateEvaluator worker : workers) {
                worker.setStop();
            }
            shuffles = 0;
            for (final Future<Integer> f : results) {
                shuffles += f.get();
            }
            for (final CellCandidateEvaluator worker : workers) {
                worker.syncCandidates(candidates);
            }
        } catch (final InterruptedException | ExecutionException ex) {
            ex.printStackTrace(System.out);
            System.exit(-1);
        }
        return shuffles;
    }
}
