
import java.util.ArrayList;
import java.util.Arrays;
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

    public static final class RowCol {

        public final int index;
        public final int numberOfCards;
        private final Card[] cards;

        public RowCol(final int index, int numberOfCards, final Card[] cards) {
            this.index = index;
            this.numberOfCards = numberOfCards;
            this.cards = cards;
        }

        public int size() {
            return cards.length;
        }

        public int lastPosition() {
            return cards.length - 1;
        }

        public boolean isLast() {
            return index == Board.SIZE - 1;
        }

        public boolean isEmpty() {
            return numberOfCards == 0;
        }

        public boolean isLastPositionEmpty() {
            return cards[cards.length - 1] == null;
        }

        public boolean isEmpty(final int index) {
            return cards[index] == null;
        }

        public boolean isFull() {
            return numberOfCards >= cards.length;
        }

        public Card getCard(final int index) {
            return cards[index];
        }

        public List<Card> getCards() {
            final List<Card> results = new ArrayList<>(cards.length);
            for (final Card c : cards) {
                if (c != null) {
                    results.add(c);
                }
            }
            return results;
        }

        public int findFirstPosition(final Predicate<Card> p) {
            for (int i = 0; i < SIZE; ++i) {
                if (p.test(cards[i])) {
                    return i;
                }
            }
            return -1;
        }

        public int findFirstEmptyPosition() {
            for (int i = 0; i < SIZE; ++i) {
                if (cards[i] == null) {
                    return i;
                }
            }
            return -1;
        }

        public Card findFirstCard(final Predicate<Card> p) {
            for (final Card c : cards) {
                if (c != null && p.test(c)) {
                    return c;
                }
            }
            return null;
        }

        public boolean anyCardsMatch(final Predicate<Card> p) {
            for (final Card c : cards) {
                if (c != null && p.test(c)) {
                    return true;
                }
            }
            return false;
        }

        public boolean allCardsMatch(final Predicate<Card> p) {
            for (final Card c : cards) {
                if (c != null && !p.test(c)) {
                    return false;
                }
            }
            return true;
        }

        public boolean anyPositionsMatch(final Predicate<Card> p) {
            for (final Card c : cards) {
                if (p.test(c)) {
                    return true;
                }
            }
            return false;
        }

        public boolean allPositionsMatch(final Predicate<Card> p) {
            for (final Card c : cards) {
                if (!p.test(c)) {
                    return false;
                }
            }
            return true;
        }

        public boolean hasRank(final int rank) {
            for (final Card c : cards) {
                if (c != null && rank == c.getRank()) {
                    return true;
                }
            }
            return false;
        }

        public int countCards(final Predicate<Card> p) {
            int n = 0;
            for (final Card c : cards) {
                if (c != null && p.test(c)) {
                    ++n;
                }
            }
            return n;
        }

        public int countPositions(final Predicate<Card> p) {
            int n = 0;
            for (final Card c : cards) {
                if (p.test(c)) {
                    ++n;
                }
            }
            return n;
        }

        public int countCardsOfRank(final int rank) {
            int n = 0;
            for (final Card c : cards) {
                if (c != null && rank == c.getRank()) {
                    ++n;
                }
            }
            return n;
        }

        public int countRanks() {
            int n = 0;
            final boolean[] ranks = new boolean[Card.NUM_RANKS];
            for (final Card c : cards) {
                if (c != null && !ranks[c.getRank()]) {
                    ++n;
                    ranks[c.getRank()] = true;
                }
            }
            return n;
        }

        public int countSuits() {
            int n = 0;
            final boolean[] suits = new boolean[Card.NUM_SUITS];
            for (final Card c : cards) {
                if (c != null && !suits[c.getSuit()]) {
                    ++n;
                    suits[c.getSuit()] = true;
                }
            }
            return n;
        }
    }

    public static final int SIZE = 5;
    public static final int NUMBER_OF_CELLS = SIZE * SIZE;

    private final List<Play> plays = new ArrayList<>(NUMBER_OF_CELLS);
    private final Card[][] rows = new Card[SIZE][SIZE];
    private final Card[][] cols = new Card[SIZE][SIZE];
    private final int[] numbersOfCardsOfRows = new int[SIZE];
    private final int[] numbersOfCardsOfCols = new int[SIZE];

    public boolean isEmpty() {
        return plays.isEmpty();
    }

    public boolean isEmpty(final int row, final int col) {
        return rows[row][col] == null;
    }

    public int getNumberOfCards() {
        return plays.size();
    }

    public int getNumberOfEmptyCells() {
        return NUMBER_OF_CELLS - plays.size();
    }

    public List<Cell> getEmptyCells() {
        final List<Cell> cells = new ArrayList<>(getNumberOfEmptyCells());
        for (int r = 0; r < rows.length; ++r) {
            for (int c = 0; c < cols.length; ++c) {
                if (rows[r][c] == null) {
                    cells.add(new Cell(r, c));
                }
            }
        }
        return cells;
    }

    public int getScore(final PokerSquaresPointSystem pointSystem) {
        if (NUMBER_OF_CELLS != plays.size()) {
            throw new IllegalStateException("The game has not been finished.");
        }
        return pointSystem.getScore(rows);
    }

    public Cell findFirstEmptyCell() {
        final RowCol row = findFirstRow(r -> !r.isFull());
        return new Cell(row.index, row.findFirstEmptyPosition());
    }

    public RowCol getRow(final int row) {
        return new RowCol(row, numbersOfCardsOfRows[row], rows[row]);
    }

    public RowCol getCol(final int col) {
        return new RowCol(col, numbersOfCardsOfCols[col], cols[col]);
    }

    public Card getCard(final int row, final int col) {
        return rows[row][col];
    }

    public void clear() {
        for (final Card[] cards : rows) {
            Arrays.fill(cards, null);
        }
        for (final Card[] cards : cols) {
            Arrays.fill(cards, null);
        }
        Arrays.fill(numbersOfCardsOfRows, 0);
        Arrays.fill(numbersOfCardsOfCols, 0);
        plays.clear();
    }

    public void putCard(final Card c, final int row, final int col) {
        if (rows[row][col] != null || cols[col][row] != null) {
            throw new IllegalStateException("The cell is not empty.");
        }
        plays.add(new Play(row, col, c));
        rows[row][col] = cols[col][row] = c;
        ++numbersOfCardsOfRows[row];
        ++numbersOfCardsOfCols[col];
    }

    public Play retractLastPlay() {
        if (isEmpty()) {
            return null;
        }
        final Play lastPlay = plays.remove(plays.size() - 1);
        if (rows[lastPlay.row][lastPlay.col] == null || cols[lastPlay.col][lastPlay.row] == null) {
            throw new IllegalStateException("Failed integrity check.");
        }
        rows[lastPlay.row][lastPlay.col] = cols[lastPlay.col][lastPlay.row] = null;
        --numbersOfCardsOfRows[lastPlay.row];
        --numbersOfCardsOfCols[lastPlay.col];
        return lastPlay;
    }

    public Play getLastPlay() {
        if (isEmpty()) {
            return null;
        }
        return plays.get(plays.size() - 1);
    }

    public boolean hasPlayedSuit(final int suit) {
        return plays.stream().anyMatch(p -> p.card.getSuit() == suit);
    }

    public List<Play> getPastPlays() {
        return Collections.unmodifiableList(plays);
    }

    public RowCol findFirstRow(final Predicate<RowCol> p) {
        for (int i = 0; i < SIZE; ++i) {
            final RowCol row = getRow(i);
            if (p.test(row)) {
                return row;
            }
        }
        return null;
    }

    public RowCol findFirstCol(final Predicate<RowCol> p) {
        for (int i = 0; i < SIZE; ++i) {
            final RowCol col = getCol(i);
            if (p.test(col)) {
                return col;
            }
        }
        return null;
    }

    public List<RowCol> findRows(final Predicate<RowCol> p) {
        final List<RowCol> results = new ArrayList<>(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            final RowCol row = getRow(i);
            if (p.test(row)) {
                results.add(row);
            }
        }
        return results;
    }

    public List<RowCol> findCols(final Predicate<RowCol> p) {
        final List<RowCol> results = new ArrayList<>(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            final RowCol col = getCol(i);
            if (p.test(col)) {
                results.add(col);
            }
        }
        return results;
    }

    public boolean anyRowsMatch(final Predicate<RowCol> p) {
        for (int i = 0; i < SIZE; ++i) {
            final RowCol row = getRow(i);
            if (p.test(row)) {
                return true;
            }
        }
        return false;
    }

    public boolean anyColsMatch(final Predicate<RowCol> p) {
        for (int i = 0; i < SIZE; ++i) {
            final RowCol col = getCol(i);
            if (p.test(col)) {
                return true;
            }
        }
        return false;
    }

    public boolean allRowsMatch(final Predicate<RowCol> p) {
        for (int i = 0; i < SIZE; ++i) {
            final RowCol row = getRow(i);
            if (!p.test(row)) {
                return false;
            }
        }
        return true;
    }

    public boolean allColsMatch(final Predicate<RowCol> p) {
        for (int i = 0; i < SIZE; ++i) {
            final RowCol col = getCol(i);
            if (!p.test(col)) {
                return false;
            }
        }
        return true;
    }
}
