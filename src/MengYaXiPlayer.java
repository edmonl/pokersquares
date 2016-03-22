
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 *
 * @author Meng
 */
public final class MengYaXiPlayer implements PokerSquaresPlayer {

    private static final class Candidate {

        public final int row, col;
        public double quality = 0.0;
        public double totalScore = 0.0;
        public double roundScore = Double.MIN_VALUE;

        public Candidate(final Board.Cell cell) {
            this.row = cell.row;
            this.col = cell.col;
        }

        public Candidate(final int row, final int col) {
            this.row = row;
            this.col = col;
        }
    }

    private final class Strategy {

        public boolean verbose = false;

        private int stage = 0;
        private final List<Candidate> candidates = new ArrayList<>();

        public int getStage() {
            return stage;
        }

        public List<Candidate> getCandidates() {
            return new ArrayList<>(candidates);
        }

        public int getNumberOfCandidates() {
            return candidates.size();
        }

        /**
         *
         * @param card
         * @param out The strategic play
         * @return Quality of the strategic play.
         */
        public boolean play(final Card card) {
            candidates.clear();
            if (board.getNumberOfEmptyCells() == 1) {
                final Board.Cell cell = board.findFirstEmptyCell();
                board.putCard(card, cell.row, cell.col);
                return true;
            }
            switch (stage) {
                case 0: // empty board
                    board.putCard(card, 0, card.getSuit());
                    ++stage;
                    return true;
                case 1: { // each row has only one unique rank and there is at least an empty row
                    for (final Board.Play play : board.getPastPlays()) {
                        if (play.card.getRank() == card.getRank()) {
                            board.putCard(card, play.row, card.getSuit());
                            return true;
                        }
                    }
                    final Board.RowCol row = board.findFirstRow(r -> r.isEmpty());
                    final Board.RowCol col = board.getCol(card.getSuit());
                    if (col.numberOfCards < 4
                        || !PokerHandAnalyzer.hasStraightPotential(col)
                        || PokerHandAnalyzer.isStraight(col, card)) {
                        board.putCard(card, row.index, col.index);
                    } else {
                        board.putCard(card, row.index, row.lastPosition());
                    }
                    if (board.findFirstRow(r -> r.isEmpty()) == null) {
                        ++stage;
                    }
                    return true;
                }
                case 2: {
                    List<Board.RowCol> rows = board.findRows(r -> r.hasRank(card.getRank()));
                    if (!rows.isEmpty()) {
                        if (rows.size() == 1) {
                            final Board.RowCol targetRow = rows.get(0);
                            if (!targetRow.isFull() && targetRow.countRanks() <= 2
                                && (targetRow.numberOfCards <= 1 || !PokerHandAnalyzer.hasFlushPotential(targetRow))) {
                                List<Board.RowCol> cols = board.findCols(c
                                    -> !c.isEmpty()
                                    && PokerHandAnalyzer.hasFlushPotential(c, card));
                                if (cols.size() == 1) {
                                    final Board.RowCol targetCol = cols.get(0);
                                    if (targetRow.isEmpty(targetCol.index)
                                        && (!PokerHandAnalyzer.hasStraightPotential(targetRow)
                                        || targetRow.numberOfCards <= targetCol.numberOfCards + 1)) {
                                        cols = board.findCols(c
                                            -> c.index != targetCol.index
                                            && c.numberOfCards > 1
                                            && PokerHandAnalyzer.hasStraightPotential(c, card)
                                        );
                                        if (cols.stream().allMatch(c -> c.numberOfCards <= targetCol.numberOfCards + 1)) {
                                            board.putCard(card, targetRow.index, targetCol.index);
                                            return true;
                                        }
                                    }
                                } else if (cols.isEmpty()) {
                                    final Board.RowCol targetCol = board.findFirstCol(c -> c.isEmpty());
                                    if (targetCol != null) {
                                        board.putCard(card, targetRow.index, targetCol.index);
                                        return true;
                                    }
                                }
                            }
                        }
                    } else {
                        if (!board.hasPlayedSuit(card.getSuit()) && board.allColsMatch(c -> PokerHandAnalyzer.hasFlushPotential(c))) {
                            final Board.RowCol targetCol = board.findFirstCol(c -> c.isEmpty());
                            if (targetCol == null) {
                                break;
                            }
                            rows = board.findRows(r -> r.countRanks() <= 1);
                            if (rows.isEmpty()) {
                                break;
                            }
                            final Board.RowCol targetRow = Collections.min(rows, PokerHandAnalyzer.ROWCOL_COMPARATOR_IN_NUMBER_OF_CARDS);
                            board.putCard(card, targetRow.index, targetCol.index);
                            return true;
                        }
                        List<Board.RowCol> cols = board.findCols(c -> c.numberOfCards > 1 && PokerHandAnalyzer.hasStraightPotential(c, card));
                        if (cols.isEmpty()) {
                            cols = board.findCols(c -> PokerHandAnalyzer.hasFlushPotential(c, card));
                        } else {
                            final int max = Collections.max(cols, PokerHandAnalyzer.ROWCOL_COMPARATOR_IN_NUMBER_OF_CARDS).numberOfCards;
                            cols = board.findCols(c -> c.numberOfCards + 1 >= max && PokerHandAnalyzer.hasFlushPotential(c, card));
                        }
                        if (!cols.isEmpty()) {
                            final Board.RowCol targetCol = Collections.max(cols, PokerHandAnalyzer.ROWCOL_COMPARATOR_IN_NUMBER_OF_CARDS);
                            rows = board.findRows(r -> r.countRanks() <= 1 && r.isEmpty(targetCol.index));
                            if (!rows.isEmpty()) {
                                final Board.RowCol targetRow = Collections.min(rows, PokerHandAnalyzer.ROWCOL_COMPARATOR_IN_NUMBER_OF_CARDS);
                                board.putCard(card, targetRow.index, targetCol.index);
                                return true;
                            }
                        }
                    }
                    break;
                }
            }
            if (candidates.isEmpty()) {
                board.getEmptyCells().stream().forEach((c) -> {
                    candidates.add(new Candidate(c));
                });
            }
            candidates.stream().forEach((c) -> {
                c.quality = PokerHandAnalyzer.score(board.getRow(c.row), card, pointSystem, board, deckTracker)
                    + PokerHandAnalyzer.score(board.getCol(c.col), card, pointSystem, board, deckTracker);
            });
            candidates.sort((c0, c1) -> {
                return c0.quality == c1.quality ? 0 : (c0.quality < c1.quality ? 1 : -1);
            });
            if (verbose) {
                System.out.print("Raw candidates: ");
                candidates.stream().forEach((c) -> {
                    System.out.print(String.format(" (%.2f:%d,%d)", c.quality, c.row + 1, c.col + 1));
                });
                System.out.println();
            }
            final double max = candidates.get(0).quality;
            while (candidates.get(candidates.size() - 1).quality + 7 < max) {
                candidates.remove(candidates.size() - 1);
            }
            if (candidates.size() == 1) {
                final Candidate c = candidates.get(0);
                board.putCard(card, c.row, c.col);
                candidates.clear();
                return true;
            }
            final double min = candidates.get(candidates.size() - 1).quality;
            final double delta = max - min;
            if (delta > 1e-4) {
                final List<Double> qualities = new ArrayList<>(candidates.size());
                qualities.add(max);
                candidates.stream().forEach((c) -> {
                    if (c.quality != qualities.get(qualities.size() - 1)) {
                        qualities.add(c.quality);
                    }
                });
                final Map<Double, Double> qmap = new HashMap<>();
                final int n = qualities.size();
                for (int i = 0; i < n; ++i) {
                    qmap.put(qualities.get(i), 1 + (double) qualities.get(i) / delta - min / delta);
                }
                candidates.stream().forEach((c) -> {
                    c.quality = qmap.get(c.quality);
                });
                final double sum = candidates.stream().map((c) -> c.quality).reduce(0.0, (accumulator, v) -> accumulator + v);
                candidates.stream().forEach((c) -> {
                    c.quality /= sum;
                });
            } else {
                candidates.stream().forEach((c) -> {
                    c.quality = 1.0 / candidates.size();
                });
            }
            return false;
        }

        public Candidate selectCandidateRandomly() {
            // bias to better ones
            final double r = Math.random();
            double sum = 0.0;
            for (final Candidate c : candidates) {
                sum += c.quality;
                if (r <= sum) {
                    return c;
                }
            }
            return candidates.isEmpty() ? null : candidates.get(candidates.size() - 1);
        }

        public void reset() {
            stage = 0;
        }
    }

    public boolean verbose = false;
    private PokerSquaresPointSystem pointSystem;
    private final Board board = new Board();
    private final DeckTracker deckTracker = new DeckTracker();
    private final Strategy strategy = new Strategy();

    @Override
    public void setPointSystem(final PokerSquaresPointSystem system, final long millis) {
        pointSystem = system; // American system is guaranteed.
    }

    @Override
    public void init() {
        board.clear();
        deckTracker.reset();
        strategy.reset();
        strategy.verbose = this.verbose;
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
        final List<Candidate> candidates = strategy.getCandidates();
        if (verbose) {
            System.out.print("Candidates: ");
            candidates.stream().forEach((c) -> {
                System.out.print(String.format(" (%.2f:%d,%d)", c.quality, c.row + 1, c.col + 1));
            });
            System.out.println();
        }
        Candidate winner;
        final int contingency = 40 * board.getNumberOfEmptyCells() - 70;
        strategy.verbose = false;
        if (millisRemaining <= contingency + 10) {
            if (verbose) {
                System.out.println("No time for trials.");
            }
            winner = candidates.get(0);
        } else {
            millisRemaining -= contingency;
            winner = monteCarloGuess(card, candidates,
                Math.max((int) Math.floor(millisRemaining * (-0.04 * board.getNumberOfEmptyCells() + 1.08)), 1));
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

    private void calcWins(final List<Double> roundScores, final List<Integer> wins) {
        final double maxScore = Collections.max(roundScores);
        for (int i = 0; i < roundScores.size(); ++i) {
            if (maxScore == roundScores.get(i)) {
                wins.set(i, wins.get(i) + 1);
            }
        }
    }

    private void sumRoundScores(final List<Double> roundScores, final List<Double> scores) {
        for (int i = 0; i < scores.size(); ++i) {
            scores.set(i, scores.get(i) + roundScores.get(i));
            roundScores.set(i, Double.MIN_VALUE);
        }
    }

    private Candidate monteCarloGuess(final Card card, final List<Candidate> candidates, final long millisRemaining) {
        if (verbose) {
            System.out.println(String.format("Time Quota: %.2f seconds", millisRemaining / 1000.0));
        }

        final List<Double> scores = Arrays.asList(new Double[candidates.size()]);
        Collections.fill(scores, 0.0);

        final long start = System.currentTimeMillis();
        int totalTrials = 0;
        final int maxTrialsPerRound = calcMaxTrialsPerRound();

        final List<Card> cards = deckTracker.getCards();
        shuffle(cards);
        int shuffles = 1;

        long elapsed = 0;
        final List<Double> roundScores = Arrays.asList(new Double[candidates.size()]);
        Collections.fill(roundScores, Double.MIN_VALUE);
        final List<Integer> wins = Arrays.asList(new Integer[candidates.size()]);
        Collections.fill(wins, 0);
        do {
            testCandidates(card, candidates, roundScores, cards);
            ++totalTrials;
            elapsed = System.currentTimeMillis() - start;
        } while (elapsed <= 1);
        calcWins(roundScores, wins);
        sumRoundScores(roundScores, scores);
        double millisPerTrial = (double) elapsed / totalTrials;

        while (elapsed < millisRemaining) {
            shuffle(cards);
            ++shuffles;
            final int estimatedMaxTrails = Math.max((int) Math.round(Math.sqrt(millisRemaining / millisPerTrial)), 1);
            int maxTrails = estimatedMaxTrails;
            boolean tried = false;
            for (int trials = 0; elapsed < millisRemaining && trials < maxTrails; ++trials) {
                maxTrails = (int) Math.round(Math.min(testCandidates(card, candidates, roundScores, cards) * 2, estimatedMaxTrails));
                ++totalTrials;
                elapsed = System.currentTimeMillis() - start;
                millisPerTrial = (double) elapsed / totalTrials;
                tried = true;
            }
            if (tried) {
                calcWins(roundScores, wins);
                sumRoundScores(roundScores, scores);
            }
        }
        for (int i = 0; i < scores.size(); ++i) {
            scores.set(i, scores.get(i) / shuffles);
        }
        final double maxScore = Collections.max(scores);
        final int max = scores.indexOf(maxScore);
        if (verbose) {
            System.out.print("Wins:");
            final int finalRounds = shuffles;
            wins.stream().forEach((w) -> {
                System.out.print(String.format(" %.2f", (double) w / finalRounds));
            });
            System.out.println();
            System.out.print("Scores:");
            scores.stream().forEach((s) -> {
                System.out.print(String.format(" %.2f", s));
            });
            System.out.println();
            final double minScore = Collections.min(scores);
            final double totalScore = scores.stream().reduce(0.0, (acc, v) -> acc + v);
            System.out.println(String.format("Scores: max %.2f, min: %.2f, avg: %.2f", maxScore, minScore, totalScore / scores.size()));
            System.out.println(String.format("%d shuffles, %d trials completed with %.2f seconds",
                shuffles, totalTrials, (System.currentTimeMillis() - start) / 1000.0));
        }
        return candidates.get(max);
    }

    private int calcMaxTrialsPerRound() {
        int result = 1;
        for (int i = board.getNumberOfEmptyCells(); i > 1; i -= 2) {
            result *= i;
        }
        return result;
    }

    /**
     *
     * @param card
     * @param candidates
     * @param scores
     * @param cards
     * @return branching factor
     */
    private double testCandidates(final Card card, final List<Candidate> candidates, final List<Double> scores, final List<Card> cards) {
        int totalBranches = 0;
        for (int i = 0; i < candidates.size(); ++i) {
            final Candidate c = candidates.get(i);
            board.putCard(card, c.row, c.col);
            final int[] results = fakePlay(cards);
            totalBranches += results[1];
            if (results[0] > scores.get(i)) {
                scores.set(i, Double.valueOf(results[0]));
            }
            board.retractLastPlay();
        }
        return (double) totalBranches / candidates.size();
    }

    /**
     *
     * @param cards
     * @return score and branches
     */
    private int[] fakePlay(final List<Card> cards) {
        final int numberOfCards = board.getNumberOfEmptyCells();
        int branches = 1;
        for (int i = 0; i < numberOfCards; ++i) {
            final Card card = cards.get(i);
            deckTracker.deal(card);
            if (!strategy.play(card)) {
                if (branches < 999999) {
                    branches *= strategy.getNumberOfCandidates();
                }
                if (branches > 999999) {
                    branches = 999999;
                }
                final Candidate c = strategy.selectCandidateRandomly();
                board.putCard(card, c.row, c.col);
            }
        }
        final int score = board.getScore(pointSystem);
        retract(numberOfCards);
        return new int[]{score, branches};
    }

    private void retract(int steps) {
        for (; steps > 0; --steps) {
            deckTracker.putBack(board.retractLastPlay().card);
        }
    }

    private void shuffle(final List<Card> cards) {
        for (int i = cards.size() - 1; i > 0; --i) {
            final int n = (int) Math.floor(Math.random() * (i + 1));
            final Card tmpi = cards.get(i);
            cards.set(i, cards.get(n));
            cards.set(n, tmpi);
        }
    }
}
