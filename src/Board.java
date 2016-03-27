
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 *
 * @author Meng
 */
final class Board {

    public static final class Cell {

        public final int row;
        public final int col;

        public Cell(final int row, final int col) {
            this.row = row;
            this.col = col;
        }
    }

    public static final class Play {

        public final int row;
        public final int col;
        public final Card card;

        public Play(final int row, final int col, final Card card) {
            this.row = row;
            this.col = col;
            this.card = card;
        }
    }

    public static final int SIZE = 5;
    public static final int NUMBER_OF_CELLS = SIZE * SIZE;

    private final List<Play> plays = new ArrayList<>(NUMBER_OF_CELLS);
    private final RowColRecord[] rows = new RowColRecord[SIZE];
    private final RowColRecord[] cols = new RowColRecord[SIZE];

    public Board() {
        for (int i = 0; i < SIZE; ++i) {
            rows[i] = new RowColRecord(i);
            cols[i] = new RowColRecord(i);
        }
    }

    public void copyFrom(final Board board) {
        Collections.copy(plays, board.plays);
        for (int i = 0; i < SIZE; ++i) {
            rows[i].copyFrom(board.rows[i]);
            cols[i].copyFrom(board.cols[i]);
        }
    }

    public void clear() {
        plays.clear();
        for (int i = 0; i < SIZE; ++i) {
            rows[i].clear();
            cols[i].clear();
        }
    }

    public boolean isEmpty() {
        return plays.isEmpty();
    }

    public boolean isEmpty(final int row, final int col) {
        return rows[row].isEmpty(col);
    }

    public int numberOfCards() {
        return plays.size();
    }

    public int numberOfEmptyCells() {
        return NUMBER_OF_CELLS - plays.size();
    }

    public List<Cell> getEmptyCells() {
        final List<Cell> cells = new ArrayList<>(numberOfEmptyCells());
        for (final RowCol r : rows) {
            for (int c = 0; c < SIZE; ++c) {
                if (r.isEmpty(c)) {
                    cells.add(new Cell(r.index, c));
                }
            }
        }
        return cells;
    }

    public int getScore(final PokerSquaresPointSystem pointSystem) {
        int score = 0;
        for (final RowCol r : rows) {
            score += r.getPokerHandScore(pointSystem);
        }
        for (final RowCol c : cols) {
            score += c.getPokerHandScore(pointSystem);
        }
        return score;
    }

    public Cell findFirstEmptyCell() {
        for (final RowCol r : rows) {
            if (!r.isFull()) {
                return new Cell(r.index, r.findFirstEmptyPosition());
            }
        }
        return null;
    }

    public RowCol getRow(final int row) {
        return rows[row];
    }

    public RowCol getCol(final int col) {
        return cols[col];
    }

    public double progress() {
        return (double) plays.size() / NUMBER_OF_CELLS;
    }

    public void putCard(final Card c, final int row, final int col) {
        rows[row].putCard(c, col);
        cols[col].putCard(c, row);
        plays.add(new Play(row, col, c));
    }

    public void putCard(final Card c, final RowCol row, final RowCol col) {
        rows[row.index].putCard(c, col.index);
        cols[col.index].putCard(c, row.index);
    }

    public Play retractLastPlay() {
        final Play lastPlay = plays.remove(plays.size() - 1);
        rows[lastPlay.row].removeCard(lastPlay.col);
        cols[lastPlay.col].removeCard(lastPlay.row);
        return lastPlay;
    }

    public Play getLastPlay() {
        return plays.get(plays.size() - 1);
    }

    public boolean hasPlayedSuit(final int suit) {
        for (final RowCol r : rows) {
            if (r.hasSuit(suit)) {
                return true;
            }
        }
        return false;
    }

    public List<Play> getPastPlays() {
        return Collections.unmodifiableList(plays);
    }

    public RowCol findFirstEmptyRow() {
        for (final RowCol r : rows) {
            if (r.isEmpty()) {
                return r;
            }
        }
        return null;
    }

    public RowCol findFirstRow(final Predicate<RowCol> p) {
        for (final RowCol r : rows) {
            if (p.test(r)) {
                return r;
            }
        }
        return null;
    }

    public RowCol findFirstEmptyCol() {
        for (final RowCol c : cols) {
            if (c.isEmpty()) {
                return c;
            }
        }
        return null;
    }

    public List<RowCol> findRows(final Predicate<RowCol> p) {
        final List<RowCol> results = new ArrayList<>(SIZE);
        for (final RowCol r : rows) {
            if (p.test(r)) {
                results.add(r);
            }
        }
        return results;
    }

    public List<RowCol> findCols(final Predicate<RowCol> p) {
        final List<RowCol> results = new ArrayList<>(SIZE);
        for (final RowCol c : cols) {
            if (p.test(c)) {
                results.add(c);
            }
        }
        return results;
    }

    public boolean allRowsMatch(final Predicate<RowCol> p) {
        for (final RowCol r : rows) {
            if (!p.test(r)) {
                return false;
            }
        }
        return true;
    }

    public boolean allColsMatch(final Predicate<RowCol> p) {
        for (final RowCol c : cols) {
            if (!p.test(c)) {
                return false;
            }
        }
        return true;
    }
}
