
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 *
 * @author Meng
 */
public final class MengYaXiPlayer implements PokerSquaresPlayer {

    private final class Strategy {

        private int stage = 0;
        private final List<Board.Cell> candidates = new ArrayList<>();

        public int getStage() {
            return stage;
        }

        public List<Board.Cell> getCandidates() {
            return Collections.unmodifiableList(candidates);
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
                    Board.RowCol col = board.findFirstCol(c -> PokerHandAnalyzer.isRoyalFlush(c, card));
                    if (col != null) {
                        board.putCard(card, col.findFirstEmptyPosition(), col.index);
                        return true;
                    }
                    Board.RowCol row = board.findFirstRow(r -> PokerHandAnalyzer.isRoyalFlush(r, card));
                    if (row != null) {
                        board.putCard(card, row.index, row.findFirstEmptyPosition());
                        return true;
                    }
                    List<Board.RowCol> cols = board.findCols(c -> PokerHandAnalyzer.isStraightFlush(c, card));
                    if (!cols.isEmpty()) {
                        if (cols.size() == 1) {
                            col = cols.get(0);
                            board.putCard(card, col.findFirstEmptyPosition(), col.index);
                            return true;
                        }
                        cols.stream().forEach((c) -> {
                            candidates.add(new Board.Cell(c.findFirstEmptyPosition(), c.index));
                        });
                        return false;
                    }
                    List<Board.RowCol> rows = board.findRows(r -> PokerHandAnalyzer.isStraightFlush(r, card));
                    if (!rows.isEmpty()) {
                        if (rows.size() == 1) {
                            row = rows.get(0);
                            board.putCard(card, row.index, row.findFirstEmptyPosition());
                            return true;
                        }
                        rows.stream().forEach((r) -> {
                            candidates.add(new Board.Cell(r.index, r.findFirstEmptyPosition()));
                        });
                        return false;
                    }
                    rows = board.findRows(r -> r.hasRank(card.getRank()));
                    if (!rows.isEmpty()) {
                        if (rows.size() == 1) {
                            final Board.RowCol targetRow = rows.get(0);
                            if (targetRow.numberOfCards <= 2 || targetRow.countRanks() <= 1
                                || (!targetRow.isFull()
                                && !PokerHandAnalyzer.hasStraightPotential(targetRow)
                                && !PokerHandAnalyzer.hasFlushPotential(targetRow))) {
                                if (!board.hasPlayedSuit(card.getSuit())) {
                                    col = board.findFirstCol(c -> c.isEmpty() && targetRow.isEmpty(c.index));
                                    if (col != null) {
                                        board.putCard(card, targetRow.index, col.index);
                                        return true;
                                    }
                                    return false;
                                }
                                cols = board.findCols(c -> targetRow.isEmpty(c.index) && PokerHandAnalyzer.hasFlushPotential(c, card));
                                if (!cols.isEmpty()) {
                                    int max = Collections.max(cols, PokerHandAnalyzer.ROWCOL_COMPARATOR_IN_NUMBER_OF_CARDS).numberOfCards;
                                    cols.removeIf(c -> c.numberOfCards != max);
                                    Optional<Board.RowCol> target = cols.stream().filter(c -> PokerHandAnalyzer.hasRoyalFlushPotential(c, card)).findFirst();
                                    if (!target.isPresent()) {
                                        target = cols.stream().filter(c -> PokerHandAnalyzer.hasStraightFlushPotential(c, card)).findFirst();
                                    }
                                    if (target.isPresent()) {
                                        col = target.get();
                                    } else {
                                        col = cols.get(0);
                                    }
                                    board.putCard(card, targetRow.index, col.index);
                                    return true;
                                }
                                cols = board.findCols(c -> targetRow.isEmpty(c.index) && PokerHandAnalyzer.hasStraightPotential(c, card));
                                if (!cols.isEmpty()) {
                                    col = Collections.max(cols, PokerHandAnalyzer.ROWCOL_COMPARATOR_IN_NUMBER_OF_CARDS);
                                    board.putCard(card, targetRow.index, col.index);
                                    return true;
                                }
                                cols = board.findCols(c
                                    -> targetRow.isEmpty(c.index)
                                    && c.numberOfCards == c.countRanks()
                                    && !PokerHandAnalyzer.hasStraightPotential(c)
                                    && !PokerHandAnalyzer.hasFlushPotential(c)
                                );
                                if (!cols.isEmpty()) {
                                    col = Collections.max(cols, PokerHandAnalyzer.ROWCOL_COMPARATOR_IN_NUMBER_OF_CARDS);
                                    board.putCard(card, targetRow.index, col.index);
                                    return true;
                                }
                            }
                        }
                    } else {
                        if (!board.hasPlayedSuit(card.getSuit())) {
                            final Board.RowCol targetCol = board.findFirstCol(c -> c.isEmpty());
                            if (targetCol != null) {
                                rows = board.findRows(r -> r.countRanks() == 1 && r.isEmpty(targetCol.index));
                                if (!rows.isEmpty()) {
                                    row = Collections.min(rows, PokerHandAnalyzer.ROWCOL_COMPARATOR_IN_NUMBER_OF_CARDS);
                                    board.putCard(card, row.index, targetCol.index);
                                    return true;
                                }
                            }
                            return false;
                        }
                        cols = board.findCols(c
                            -> PokerHandAnalyzer.hasFlushPotential(c, card)
                            && (c.numberOfCards < 4 || !PokerHandAnalyzer.hasStraightPotential(c)
                            || PokerHandAnalyzer.hasStraightPotential(c, card))
                        );
                        if (!cols.isEmpty()) {
                            cols.sort(PokerHandAnalyzer.REVERSE_ROWCOL_COMPARATOR_IN_NUMBER_OF_CARDS);
                            for (final Board.RowCol targetCol : cols) {
                                rows = board.findRows(r -> r.countRanks() <= 1 && r.isEmpty(targetCol.index));
                                if (!rows.isEmpty()) {
                                    row = Collections.min(rows, PokerHandAnalyzer.ROWCOL_COMPARATOR_IN_NUMBER_OF_CARDS);
                                    board.putCard(card, row.index, targetCol.index);
                                    return true;
                                }
                            }
                            if (board.allRowsMatch(r -> r.countRanks() >= 2)) {
                                for (final Board.RowCol targetCol : cols) {
                                    rows = board.findRows(r -> r.isEmpty(targetCol.index) && PokerHandAnalyzer.hasStraightPotential(r, card));
                                    if (!rows.isEmpty()) {
                                        row = Collections.max(rows, PokerHandAnalyzer.ROWCOL_COMPARATOR_IN_NUMBER_OF_CARDS);
                                        board.putCard(card, row.index, targetCol.index);
                                        return true;
                                    }
                                }
                                for (final Board.RowCol targetCol : cols) {
                                    rows = board.findRows(r
                                        -> r.isEmpty(targetCol.index)
                                        && r.numberOfCards >= 3
                                        && r.numberOfCards == r.countRanks()
                                        && !PokerHandAnalyzer.hasStraightPotential(r)
                                    );
                                    if (!rows.isEmpty()) {
                                        row = Collections.max(rows, PokerHandAnalyzer.ROWCOL_COMPARATOR_IN_NUMBER_OF_CARDS);
                                        board.putCard(card, row.index, targetCol.index);
                                        return true;
                                    }
                                }
                                if (board.anyRowsMatch(r -> r.numberOfCards == 2)) {
                                    for (final Board.RowCol targetCol : cols) {
                                        row = board.findFirstRow(r -> r.numberOfCards == 2 && r.isEmpty(targetCol.index));
                                        if (row != null) {
                                            board.putCard(card, row.index, targetCol.index);
                                            return true;
                                        }
                                    }
                                }
                            }
                            return false;
                        }
                        cols = board.findCols(c
                            -> !c.isFull()
                            && c.numberOfCards == c.countRanks()
                            && (!PokerHandAnalyzer.hasFlushPotential(c)
                            || c.numberOfCards <= 1 && c.isLast())
                            && (!PokerHandAnalyzer.hasStraightPotential(c)
                            || c.numberOfCards <= 2 && c.isLast())
                        );
                        if (!cols.isEmpty()) {
                            if (cols.stream().anyMatch(c -> c.isLast())) {
                                cols.removeIf(c -> !c.isLast() && c.numberOfCards <= 1);
                            }
                            if (cols.size() == 1) {
                                final Board.RowCol targetCol = cols.get(0);
                                rows = board.findRows(r -> r.countRanks() <= 1 && r.isEmpty(targetCol.index));
                                if (!rows.isEmpty()) {
                                    row = Collections.min(rows, PokerHandAnalyzer.ROWCOL_COMPARATOR_IN_NUMBER_OF_CARDS);
                                    board.putCard(card, row.index, targetCol.index);
                                    return true;
                                }
                            }
                        }
                    }
                    break;
                }
            }
            return false;
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
    }

    @Override
    public int[] getPlay(final Card card, long millisRemaining) {
        deckTracker.deal(card);
        if (verbose) {
            System.out.println("Get card \"" + card + "\"");
        }
        if (strategy.play(card)) {
            final Board.Play p = board.getLastPlay();
            if (verbose) {
                System.out.println(String.format("Play %s at row %d, column %d", card, p.row + 1, p.col + 1));
            }
            return new int[]{p.row, p.col};
        }
        List<Board.Cell> candidates = Collections.unmodifiableList(strategy.getCandidates());
        if (candidates.isEmpty()) {
            candidates = Collections.unmodifiableList(board.getEmptyCells());
        }
        if (millisRemaining < 100) {
            millisRemaining = 0;
        }
        final Board.Cell winner = monteCarloGuess(card, candidates, millisRemaining / 2);
        board.putCard(card, winner.row, winner.col);
        if (verbose) {
            System.out.println(String.format("Play %s at row %d, column %d", card, winner.row + 1, winner.col + 1));
        }
        return new int[]{winner.row, winner.col};
        /*
        final Scanner in = new Scanner(System.in);
        System.out.println(String.format("Where do you play card \"" + card + "\"?"));
        final int[] pos = new int[2];
        while (true) {
            System.out.print("> ");
            for (int i = 0; i < pos.length; ++i) {
                while (true) {
                    try {
                        if (in.hasNext()) {
                            in.skip("[,\\s]*");
                        }
                        pos[i] = in.nextInt() - 1;
                        if (pos[i] >= 0 && pos[i] < 5) {
                            break;
                        }
                        throw new InputMismatchException();
                    } catch (final InputMismatchException ex) {
                        System.out.println("Not a valid input. Try again.");
                    }
                }
            }
            if (board.isEmpty(pos[0], pos[1])) {
                break;
            }
            System.out.println("The cell is not empty. Try again.");
        }
        board.putCard(card, pos[0], pos[1]);
        return pos;
         */
    }

    @Override
    public String getName() {
        return "MengYaXi Player";
    }

    public static void main(final String[] args) {
        final MengYaXiPlayer player = new MengYaXiPlayer();
        player.verbose = true;
        final PokerSquares game = new PokerSquares(player, PokerSquaresPointSystem.getAmericanPointSystem());
        PokerSquares.GAME_MILLIS = 3000000;
        game.play(new Scanner(System.in));
    }

    private Board.Cell monteCarloGuess(final Card card, final List<Board.Cell> candidates, final long millisRemaining) {
        if (verbose) {
            System.out.println(String.format("Start Monte Carlo. Time Quota: %.2f seconds", millisRemaining / 1000.0));
        }
        if (millisRemaining <= 0) {
            if (verbose) {
                System.out.println("No time for trials. Randomly guess one.");
            }
            return candidates.get((int) Math.floor(Math.random() * candidates.size()));
        }

        final double[] scores = new double[candidates.size()];
        Arrays.fill(scores, Double.MIN_VALUE);

        final long start = System.currentTimeMillis();
        int totalTrials = 0;

        List<Card> cards = deckTracker.shuffle();
        int shuffles = 1;

        long elapsed = 0;
        do {
            testCandidates(card, candidates, scores, cards);
            ++totalTrials;
            elapsed = System.currentTimeMillis() - start;
        } while (elapsed <= 1);
        double millisPerTrial = (double) elapsed / totalTrials;
        final long time = millisRemaining - elapsed;

        if (time > 0) {
            for (int rounds = 0; rounds < Math.sqrt(millisRemaining / millisPerTrial); ++rounds) {
                cards = deckTracker.shuffle();
                ++shuffles;
                final double[] roundScores = new double[candidates.size()];
                Arrays.fill(roundScores, Double.MIN_VALUE);
                for (int trials = 0; trials < Math.sqrt(millisRemaining / millisPerTrial); ++trials) {
                    testCandidates(card, candidates, roundScores, cards);
                    ++totalTrials;
                    millisPerTrial = (double) (System.currentTimeMillis() - start) / totalTrials;
                }
                for (int i = 0; i < scores.length; ++i) {
                    scores[i] += roundScores[i];
                }
            }
        }
        if (verbose) {
            System.out.println(String.format("%d shuffles, %d trials completed with %.2f seconds",
                shuffles, totalTrials, (System.currentTimeMillis() - start) / 1000.0));
        }
        scores[0] /= shuffles;
        double maxScore = scores[0];
        int max = 0;
        double minScore = scores[0];
        double totalScore = scores[0];
        for (int i = 1; i < scores.length; ++i) {
            scores[i] /= shuffles;
            totalScore += scores[i];
            if (maxScore < scores[i]) {
                maxScore = scores[i];
                max = i;
            } else if (minScore > scores[i]) {
                minScore = scores[i];
            }
        }
        if (verbose) {
            System.out.println(String.format("Scores: max %.2f, min: %.2f, avg: %.2f", maxScore, minScore, totalScore / scores.length));
        }
        return candidates.get(max);
    }

    private void testCandidates(final Card card, final List<Board.Cell> candidates, final double[] scores, final List<Card> cards) {
        for (int i = 0; i < candidates.size(); ++i) {
            final Board.Cell cell = candidates.get(i);
            board.putCard(card, cell.row, cell.col);
            final int score = fakePlay(cards);
            if (score > scores[i]) {
                scores[i] = score;
            }
            board.retractLastPlay();
        }
    }

    private int fakePlay(final List<Card> cards) {
        final int numberOfCards = board.getNumberOfEmptyCells();
        for (int i = 0; i < numberOfCards; ++i) {
            final Card card = cards.get(i);
            deckTracker.deal(card);
            if (!strategy.play(card)) {
                List<Board.Cell> candidates = new ArrayList<>(strategy.getCandidates());
                if (candidates.isEmpty()) {
                    candidates = board.getEmptyCells();
                }
                final Board.Cell cell = candidates.get((int) Math.floor(Math.random() * candidates.size()));
                board.putCard(card, cell.row, cell.col);
            }
        }
        final int score = board.getScore(pointSystem);
        retract(numberOfCards);
        return score;
    }

    private void retract(int steps) {
        for (; steps > 0; --steps) {
            deckTracker.putBack(board.retractLastPlay().card);
        }
    }
}
