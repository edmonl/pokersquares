
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import util.Linear;
import util.Pokers;

/**
 *
 * @author Meng
 */
public final class MengYaXiPlayer implements PokerSquaresPlayer {

    private static final Comparator<RowCol> NUMBER_OF_CARDS_ROWCOL_COMPARATOR = (rc0, rc1) -> rc0.numberOfCards() - rc1.numberOfCards();
    private static final Comparator<RowCol> NUMBER_OF_CARDS_ROWCOL_REVERSE_COMPARATOR = (rc0, rc1) -> rc1.numberOfCards() - rc0.numberOfCards();

    private static final class Candidate {

        public static final Comparator<Candidate> SCORE_COMPARATOR = (c0, c1) -> {
            if (c0.score == c1.score) {
                return 0;
            }
            return c0.score > c1.score ? 1 : -1;
        };
        public static final Comparator<Candidate> AVERAGE_SCORE_COMPARATOR = (c0, c1) -> {
            if (c0.getAverageScore() == c1.getAverageScore()) {
                return 0;
            }
            return c0.getAverageScore() > c1.getAverageScore() ? 1 : -1;
        };
        public static final Comparator<Candidate> REVERSE_QUALITY_COMPARATOR = (c0, c1) -> {
            if (c0.quality == c1.quality) {
                return 0;
            }
            return c0.quality > c1.quality ? -1 : 1;
        };
        public static final Comparator<Candidate> QUALITY_COMPARATOR = (c0, c1) -> {
            if (c0.quality == c1.quality) {
                return 0;
            }
            return c0.quality < c1.quality ? -1 : 1;
        };

        public final int row, col;
        public double quality = 0.0;
        public int score = 0; // we don't have negtive score

        private double totalScore = 0.0;
        private double averageScore = 0.0;

        public Candidate(final Board.Cell cell) {
            this.row = cell.row;
            this.col = cell.col;
        }

        public Candidate(final int row, final int col) {
            this.row = row;
            this.col = col;
        }

        public double getTotalScore() {
            return totalScore;
        }

        public double getAverageScore() {
            return averageScore;
        }

        public void nextRound() {
            totalScore += score;
            score = 0;
        }

        public void average(final int rounds) {
            averageScore = totalScore / rounds;
        }
    }

    private final class Strategy {

        public boolean verbose = false;

        private final List<Candidate> candidates = new ArrayList<>();

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
            if (board.isEmpty()) {
                board.putCard(card, 0, card.getSuit());
                return true;
            }
            if (board.numberOfEmptyCells() == 1) {
                final Board.Cell cell = board.findFirstEmptyCell();
                board.putCard(card, cell.row, cell.col);
                return true;
            }
            RowCol row = board.findFirstEmptyRow();
            if (row != null) {
                for (final Board.Play play : board.getPastPlays()) {
                    if (play.card.getRank() == card.getRank()) {
                        board.putCard(card, play.row, card.getSuit());
                        return true;
                    }
                }
                RowCol col = board.getCol(card.getSuit());
                if (col.numberOfCards() < 4
                    || !col.hasStraightPotential()
                    || col.hasStraightPotential(card)) {
                    final RowCol targetCol = col;
                    board.putCard(card, row.index, targetCol.index);
                } else {
                    final RowCol targetCol = board.getCol(row.lastPosition());
                    board.putCard(card, row.index, targetCol.index);
                }
                return true;
            }
            List<RowCol> rows = board.findRows(r -> r.hasRank(card.getRank()));
            if (!rows.isEmpty()) {
                if (rows.size() == 1) {
                    final RowCol targetRow = rows.get(0);
                    if (!targetRow.isFull() && (targetRow.numberOfCards() <= 3 || targetRow.countRank(card.getRank()) >= 2)
                        && board.allRowsMatch(r -> r.countRanks() <= 2
                            || !r.hasStraightPotential(card) && !r.hasFlushPotential(card))
                        && board.allColsMatch(c -> c.countSuits() <= 1 || !c.hasStraightPotential(card))) {
                        List<RowCol> cols = board.findCols(c -> c.hasFlushPotential(card));
                        if (!cols.isEmpty()) {
                            cols.sort(NUMBER_OF_CARDS_ROWCOL_REVERSE_COMPARATOR);
                            while (cols.size() > 1 && cols.get(cols.size() - 1).numberOfCards() == 0 && cols.get(cols.size() - 2).numberOfCards() == 0) {
                                cols.remove(cols.size() - 1);
                            }
                            final RowCol targetCol = cols.get(0);
                            if (cols.size() == 1
                                || cols.get(1).numberOfCards() < targetCol.numberOfCards()
                                && (targetCol.numberOfCards() <= 2 || !targetCol.hasStraightPotential())) {
                                if (targetRow.isEmpty(targetCol.index)) {
                                    board.putCard(card, targetRow.index, targetCol.index);
                                    return true;
                                }
                            }
                            if (cols.stream().allMatch(c -> !c.isEmpty(targetRow.index))) {
                                for (int i = 0; i < RowCol.SIZE; ++i) {
                                    if (targetRow.isEmpty(i)) {
                                        candidates.add(new Candidate(targetRow.index, i));
                                    }
                                }
                            }
                            for (final RowCol c : cols) {
                                if (c.isEmpty(targetRow.index)) {
                                    candidates.add(new Candidate(targetRow.index, c.index));
                                } else {
                                    for (int i = 0; i < RowCol.SIZE; ++i) {
                                        if (c.isEmpty(i)) {
                                            candidates.add(new Candidate(i, c.index));
                                        }
                                    }
                                }
                            }
                        } else if (targetRow.countRank(card.getRank()) > 1) {
                            cols = board.findCols(c -> c.countSuits() > 1 && !c.isFull() && !c.hasStraightPotential());
                            if (!cols.isEmpty()) {
                                cols.sort(NUMBER_OF_CARDS_ROWCOL_REVERSE_COMPARATOR);
                                while (cols.get(cols.size() - 1).numberOfCards() < cols.get(0).numberOfCards()) {
                                    cols.remove(cols.size() - 1);
                                }
                                for (final RowCol targetCol : cols) {
                                    if (targetRow.isEmpty(targetCol.index)) {
                                        board.putCard(card, targetRow.index, targetCol.index);
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (board.allRowsMatch(r -> r.countRanks() <= 2 || !r.hasStraightPotential(card) && !r.hasFlushPotential(card))
                && board.allColsMatch(c -> c.countSuits() <= 1 || !c.hasStraightPotential(card))) {
                List<RowCol> cols = board.findCols(c -> c.hasFlushPotential(card));
                if (!cols.isEmpty()) {
                    cols.sort(NUMBER_OF_CARDS_ROWCOL_REVERSE_COMPARATOR);
                    while (cols.size() > 1 && cols.get(cols.size() - 1).numberOfCards() == 0 && cols.get(cols.size() - 2).numberOfCards() == 0) {
                        cols.remove(cols.size() - 1);
                    }
                    final RowCol targetCol = cols.get(0);
                    if (cols.size() == 1 || cols.get(1).numberOfCards() < targetCol.numberOfCards() || targetCol.numberOfCards() == 0) {
                        rows = board.findRows(r -> r.numberOfCards() == 1 && r.isEmpty(targetCol.index));
                        if (!rows.isEmpty()) {
                            rows.sort((r0, r1)
                                -> Pokers.rankDistance(r0.getAnyCard().getRank(), card.getRank())
                                - Pokers.rankDistance(r1.getAnyCard().getRank(), card.getRank())
                            );
                            row = rows.get(0);
                            final int minDist = Pokers.rankDistance(row.getAnyCard().getRank(), card.getRank());
                            if (minDist < RowCol.SIZE) {
                                while (rows.size() > 1
                                    && Pokers.rankDistance(rows.get(rows.size() - 1).getAnyCard().getRank(), card.getRank()) != minDist) {
                                    rows.remove(rows.size() - 1);
                                }
                            }
                            final RowCol targetRow = rows.get((int) Math.floor(Math.random() * rows.size()));
                            board.putCard(card, targetRow.index, targetCol.index);
                            return true;
                        } else {
                            rows = board.findRows(r -> r.isEmpty(targetCol.index) && r.countRanks() == 1);
                            if (!rows.isEmpty()) {
                                if (rows.size() == 1) {
                                    final RowCol targetRow = rows.get(0);
                                    board.putCard(card, targetRow.index, targetCol.index);
                                    return true;
                                }
                                for (final RowCol r : rows) {
                                    candidates.add(new Candidate(r.index, targetCol.index));
                                }
                            } else {
                                for (int i = 0; i < RowCol.SIZE; ++i) {
                                    if (targetCol.isEmpty(i)) {
                                        candidates.add(new Candidate(i, targetCol.index));
                                    }
                                }
                                rows = board.findRows(r -> r.countRanks() == 1);
                                if (!rows.isEmpty()) {
                                    for (final RowCol r : rows) {
                                        for (final RowCol c : cols) {
                                            if (c.isEmpty(r.index)) {
                                                candidates.add(new Candidate(r.index, c.index));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        for (final RowCol c : cols) {
                            for (int i = 0; i < RowCol.SIZE; ++i) {
                                if (c.isEmpty(i)) {
                                    candidates.add(new Candidate(i, c.index));
                                }
                            }
                        }
                    }
                }
            }
            if (candidates.isEmpty()) {
                boolean hasEmptyColumn = false; // don't add two empty columns
                for (int c = 0; c < Board.SIZE; ++c) {
                    final RowCol col = board.getCol(c);
                    if (hasEmptyColumn && col.isEmpty()) {
                        continue;
                    }
                    hasEmptyColumn = hasEmptyColumn || col.isEmpty();
                    for (int r = 0; r < Board.SIZE; ++r) {
                        if (board.isEmpty(r, c)) {
                            candidates.add(new Candidate(r, c));
                        }
                    }
                }
            } else if (candidates.size() == 1) {
                final Candidate c = candidates.get(0);
                board.putCard(card, c.row, c.col);
                candidates.clear();
                return true;
            }
            candidates.stream().forEach((c) -> {
                c.quality = board.getRow(c.row).scoreCard(card, c.col, pointSystem, board, deckTracker)
                    + board.getCol(c.col).scoreCard(card, c.row, pointSystem, board, deckTracker);
            });
            candidates.sort(Candidate.REVERSE_QUALITY_COMPARATOR);
            if (verbose) {
                System.out.print(candidates.size() + " raw candidates: ");
                candidates.stream().forEach((c) -> {
                    System.out.print(String.format(" (%d,%d: q=%.2f)", c.row + 1, c.col + 1, c.quality));
                });
                System.out.println();
            }
            // remove very bad ones
            final double max = candidates.get(0).quality;
            while (candidates.size() > 5 || candidates.get(candidates.size() - 1).quality + 10 < max) {
                candidates.remove(candidates.size() - 1);
            }
            if (candidates.size() == 1) {
                final Candidate c = candidates.get(0);
                board.putCard(card, c.row, c.col);
                candidates.clear();
                return true;
            }
            final double min = candidates.get(candidates.size() - 1).quality;
            if (max - min > 1e-5) { // gently bias to better candidates
                final Linear linear = new Linear(min, 1.0, max, 2.0);
                candidates.stream().forEach((c) -> {
                    c.quality = linear.apply(c.quality) / 2.0;
                });
            } else {
                candidates.stream().forEach((c) -> {
                    c.quality = 0.2;
                });
            }
            return false;
        }

        public void reset() {
            candidates.clear();
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
        final List<Candidate> candidates = strategy.getCandidates();
        if (verbose) {
            System.out.print(candidates.size() + " candidates: ");
            candidates.stream().forEach((c) -> {
                System.out.print(String.format(" (%d,%d: q=%.2f)", c.row + 1, c.col + 1, c.quality));
            });
            System.out.println();
        }
        Candidate winner;
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

    private Candidate monteCarloGuess(final Card card, final List<Candidate> candidates, final long millisRemaining) {
        if (verbose) {
            System.out.println(String.format("Time Quota: %.2f seconds", millisRemaining / 1000.0));
        }

        final long startMillis = System.currentTimeMillis();
        final List<Card> cards = deckTracker.getCards();
        int shuffles = 0;
        while (System.currentTimeMillis() - startMillis < millisRemaining && candidates.size() > 1) {
            shuffle(cards);
            ++shuffles;
            testCandidates(card, candidates, cards.subList(0, board.numberOfEmptyCells() - 1),
                shuffles < 500 ? millisRemaining - (System.currentTimeMillis() - startMillis) / (500 - shuffles) : millisRemaining - (System.currentTimeMillis() - startMillis));
            prepareNextShuffle(candidates);
        }
        final int rounds = shuffles;
        candidates.stream().forEach((c) -> {
            c.average(rounds);
        });
        final Candidate scoreWinner = Collections.max(candidates, Candidate.AVERAGE_SCORE_COMPARATOR);
        final Candidate qualityWinner = Collections.max(candidates, Candidate.QUALITY_COMPARATOR);
        Candidate winner = scoreWinner;
        if (qualityWinner != scoreWinner && qualityWinner.quality / scoreWinner.quality >= 2
            && scoreWinner.getAverageScore() - qualityWinner.getAverageScore() <= 2) {
            winner = qualityWinner;
        }
        if (verbose) {
            System.out.println(String.format("%d shuffles completed within %.2f seconds",
                shuffles, (System.currentTimeMillis() - startMillis) / 1000.0));
            System.out.print(candidates.size() + " candidates left:");
            candidates.stream().forEach((c) -> {
                System.out.print(String.format(" (%d,%d: q=%.2f, s=%.2f)", c.row + 1, c.col + 1, c.quality, c.getAverageScore()));
            });
            System.out.println();
        }
        return winner;
    }

    private void prepareNextShuffle(final List<Candidate> candidates) {
        int maxRoundScore = candidates.get(0).score;
        int minRoundScore = maxRoundScore;
        for (final Candidate c : candidates) {
            if (c.score > maxRoundScore) {
                maxRoundScore = c.score;
            } else if (c.score < maxRoundScore) {
                minRoundScore = c.score;
            }
        }
        if (maxRoundScore > minRoundScore) {
            final Linear awardFactor = new Linear(2, 0.0005, 5, 0.005);
            final double award = awardFactor.apply((double) candidates.size());
            final Linear linear = new Linear(minRoundScore, -award, maxRoundScore, award);
            candidates.stream().forEach((c) -> {
                c.quality += linear.apply((double) c.score);
                c.nextRound();
            });
            final double maxQuality = Collections.max(candidates, Candidate.QUALITY_COMPARATOR).quality;
            candidates.stream().forEach((c) -> {
                c.quality /= maxQuality;
            });
            candidates.sort(Candidate.REVERSE_QUALITY_COMPARATOR);
            while (candidates.size() > 1 && candidates.get(candidates.size() - 1).quality <= 1e-5) {
                candidates.remove(candidates.size() - 1);
            }
        } else {
            candidates.stream().forEach(c -> c.nextRound());
        }
    }

    private void testCandidates(final Card card, final List<Candidate> candidates, final List<Card> cards, final long millisRemaining) {
        final long startMillis = System.currentTimeMillis();
        for (int i = 0; i < candidates.size(); ++i) {
            final Candidate c = candidates.get(i);
            board.putCard(card, c.row, c.col);
            c.score = fakePlay(cards, (millisRemaining - (System.currentTimeMillis() - startMillis)) / (candidates.size() - i));
            board.retractLastPlay();
        }
    }

    private int fakePlay(final List<Card> cards, final long millisRemaining) {
        final long startMillis = System.currentTimeMillis();
        for (int i = 0; i < cards.size(); ++i) {
            final Card card = cards.get(i);
            deckTracker.deal(card);
            if (!strategy.play(card)) {
                final List<Candidate> candidates = strategy.getCandidates();
                if (System.currentTimeMillis() - startMillis - 100 <= millisRemaining && cards.size() - i > 5) {
                    sampleCandidates(100, card, candidates, cards.subList(i + 1, cards.size()));
                } else {
                    testCandidates(card, candidates, cards.subList(i + 1, cards.size()), millisRemaining - (System.currentTimeMillis() - startMillis));
                }
                deckTracker.putBack(card);
                retract(i);
                return Collections.max(candidates, Candidate.SCORE_COMPARATOR).score;
            }
        }
        final int score = board.getScore(pointSystem);
        retract(cards.size());
        return score;
    }

    private void sampleCandidates(final int times, final Card card, final List<Candidate> candidates, final List<Card> cards) {
        for (int i = 0; i < times; ++i) {
            final Candidate c = selectCandidateRandomly(candidates);
            board.putCard(card, c.row, c.col);
            final int score = randomPlay(cards);
            if (score > c.score) {
                c.score = score;
            }
            board.retractLastPlay();
        }
    }

    private int randomPlay(final List<Card> cards) {
        for (final Card card : cards) {
            deckTracker.deal(card);
            if (!strategy.play(card)) {
                final Candidate c = selectCandidateRandomly(strategy.getCandidates());
                board.putCard(card, c.row, c.col);
            }
        }
        final int score = board.getScore(pointSystem);
        retract(cards.size());
        return score;
    }

    private void retract(int steps) {
        for (; steps > 0; --steps) {
            deckTracker.putBack(board.retractLastPlay().card);
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

    private static Candidate selectCandidateRandomly(final List<Candidate> candidates) {
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
}
