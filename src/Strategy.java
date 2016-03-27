
import java.util.ArrayList;
import java.util.List;
import util.Linear;
import util.Pokers;

/**
 *
 * @author Meng
 */
final class Strategy {

    public boolean verbose = false;

    private final List<CellCandidate> candidates = new ArrayList<>();
    private final Board board;
    private final DeckTracker deckTracker;
    private PokerSquaresPointSystem pointSystem;

    public Strategy(final Board board, final DeckTracker deckTracker) {
        this.board = board;
        this.deckTracker = deckTracker;
    }

    public void setPointSystem(final PokerSquaresPointSystem pointSystem) {
        this.pointSystem = pointSystem;
    }

    public PokerSquaresPointSystem getPointSystem() {
        return pointSystem;
    }

    public void clear() {
        candidates.clear();
    }

    public List<CellCandidate> getCandidates() {
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
                        cols.sort(RowCol.REVERSE_NUMBER_OF_CARDS_COMPARATOR);
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
                                    candidates.add(new CellCandidate(targetRow.index, i));
                                }
                            }
                        }
                        for (final RowCol c : cols) {
                            if (c.isEmpty(targetRow.index)) {
                                candidates.add(new CellCandidate(targetRow.index, c.index));
                            } else {
                                for (int i = 0; i < RowCol.SIZE; ++i) {
                                    if (c.isEmpty(i)) {
                                        candidates.add(new CellCandidate(i, c.index));
                                    }
                                }
                            }
                        }
                    } else if (targetRow.countRank(card.getRank()) > 1) {
                        cols = board.findCols(c -> c.countSuits() > 1 && !c.isFull() && !c.hasStraightPotential());
                        if (!cols.isEmpty()) {
                            cols.sort(RowCol.REVERSE_NUMBER_OF_CARDS_COMPARATOR);
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
                cols.sort(RowCol.REVERSE_NUMBER_OF_CARDS_COMPARATOR);
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
                    }
                    rows = board.findRows(r -> r.isEmpty(targetCol.index) && r.countRanks() == 1);
                    if (!rows.isEmpty()) {
                        if (rows.size() == 1) {
                            final RowCol targetRow = rows.get(0);
                            board.putCard(card, targetRow.index, targetCol.index);
                            return true;
                        }
                        for (final RowCol r : rows) {
                            candidates.add(new CellCandidate(r.index, targetCol.index));
                        }
                    } else {
                        int i = targetCol.findFirstEmptyPosition();
                        candidates.add(new CellCandidate(i, targetCol.index));
                        for (++i; i < RowCol.SIZE; ++i) {
                            if (targetCol.isEmpty(i)) {
                                candidates.add(new CellCandidate(i, targetCol.index));
                            }
                        }
                        rows = board.findRows(r -> r.countRanks() == 1);
                        if (!rows.isEmpty()) {
                            for (final RowCol r : rows) {
                                for (final RowCol c : cols) {
                                    if (c.isEmpty(r.index)) {
                                        candidates.add(new CellCandidate(r.index, c.index));
                                    }
                                }
                            }
                        }
                    }
                } else {
                    for (final RowCol c : cols) {
                        for (int i = 0; i < RowCol.SIZE; ++i) {
                            if (c.isEmpty(i)) {
                                candidates.add(new CellCandidate(i, c.index));
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
                        candidates.add(new CellCandidate(r, c));
                    }
                }
            }
        } else if (candidates.size() == 1) {
            final CellCandidate c = candidates.get(0);
            board.putCard(card, c.row, c.col);
            candidates.clear();
            return true;
        }
        candidates.stream().forEach((c) -> {
            c.quality = board.getRow(c.row).scoreCard(card, c.col, pointSystem, board, deckTracker)
                + board.getCol(c.col).scoreCard(card, c.row, pointSystem, board, deckTracker);
        });
        candidates.sort(CellCandidate.REVERSE_QUALITY_COMPARATOR);
        if (verbose) {
            System.out.print(candidates.size() + " raw candidates: ");
            candidates.stream().forEach((c) -> {
                System.out.print(String.format(" (%d,%d: q=%.2f)", c.row + 1, c.col + 1, c.quality));
            });
            System.out.println();
        }
        // remove very bad ones
        final double max = candidates.get(0).quality;
        while (candidates.size() > 6 || candidates.get(candidates.size() - 1).quality + 10 < max) {
            candidates.remove(candidates.size() - 1);
        }
        if (candidates.size() == 1) {
            final CellCandidate c = candidates.get(0);
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
                c.quality = 0.5;
            });
        }
        return false;
    }
}
