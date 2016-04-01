package mengyaxi.pokersquares;

import java.util.ArrayList;
import java.util.List;
import mengyaxi.pokersquares.board.Board;
import mengyaxi.pokersquares.board.RowCol;
import mengyaxi.pokersquares.util.Pokers;

/**
 *
 * @author Meng
 */
final class Strategy {

    public boolean verbose = false;
    public int candidatesLimit = 7;
    public double maxQualityDifference = 10.0;

    private final List<CellCandidate> candidates = new ArrayList<>(candidatesLimit);
    private final Board board;
    private final DeckTracker deckTracker;

    public Strategy(final Board board, final DeckTracker deckTracker) {
        this.board = board;
        this.deckTracker = deckTracker;
    }

    public void clear() {
        candidates.clear();
    }

    public List<CellCandidate> getCandidates() {
        return new ArrayList<>(candidates);
    }

    /**
     *
     * @param card
     */
    public void play(final Card card) {
        candidates.clear();
        if (board.isEmpty()) {
            candidates.add(new CellCandidate(0, card.suit));
            return;
        }
        if (board.numberOfEmptyCells() == 1) {
            final Board.Cell cell = board.findFirstEmptyCell();
            candidates.add(new CellCandidate(cell.row, cell.col));
            return;
        }
        if (board.getRow(Board.SIZE - 1).isEmpty()) {
            if (board.hasRank(card.rank)) {
                final RowCol targetRow = board.findFirstRow(r -> r.hasRank(card.rank));
                candidates.add(new CellCandidate(targetRow.index, card.suit));
                return;
            }
            final RowCol targetRow = board.findFirstEmptyRow();
            final RowCol col = board.getCol(card.suit);
            if (col.numberOfCards() < 3
                || !col.hasStraightPotential()
                || col.hasStraightPotential(card)) {
                candidates.add(new CellCandidate(targetRow.index, col.index));
            } else {
                candidates.add(new CellCandidate(targetRow.index, targetRow.lastPosition()));
            }
            return;
        }
        final int rankCount = board.countRank(card.rank);
        if (rankCount > 0) {
            final RowCol targetRow = board.findFirstRow(r -> r.countRank(card.rank) == rankCount);
            if (targetRow != null) {
                if (!targetRow.isFull() && (targetRow.numberOfCards() <= 2 || targetRow.countRank(card.rank) >= 2 || !targetRow.hasStraightPotential())
                    && board.allRowsMatch(r -> r.index == targetRow.index || r.countRanks() <= 2
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
                                candidates.add(new CellCandidate(targetRow.index, targetCol.index));
                                return;
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
                        qualifyCandidates(card);
                        return;
                    }
                    if (targetRow.countRank(card.rank) > 1) {
                        boolean hasEmptyColumn = false;
                        for (int i = 0; i < RowCol.SIZE; ++i) {
                            if (targetRow.isEmpty(i)) {
                                final RowCol col = board.getCol(i);
                                if (hasEmptyColumn && col.isEmpty()) {
                                    continue;
                                }
                                hasEmptyColumn = hasEmptyColumn || col.isEmpty();
                                candidates.add(new CellCandidate(targetRow.index, i));
                            }
                        }
                        qualifyCandidates(card);
                        return;
                    }
                }
            }
        } else if (rankCount == 0) {
            if (board.allRowsMatch(r -> r.countRanks() <= 2 || !r.hasStraightPotential(card) && !r.hasFlushPotential(card))
                && board.allColsMatch(c -> c.countSuits() <= 1 || !c.hasStraightPotential(card))) {
                List<RowCol> cols = board.findCols(c -> c.hasFlushPotential(card));
                if (!cols.isEmpty()) {
                    cols.sort(RowCol.REVERSE_NUMBER_OF_CARDS_COMPARATOR);
                    while (cols.size() > 1 && cols.get(cols.size() - 1).numberOfCards() == 0
                        && cols.get(cols.size() - 2).numberOfCards() == 0) {
                        cols.remove(cols.size() - 1);
                    }
                    final RowCol targetCol = cols.get(0);
                    if ((cols.size() == 1 || cols.get(1).numberOfCards() < targetCol.numberOfCards()
                        || targetCol.numberOfCards() == 0)
                        && (targetCol.numberOfCards() <= 2
                        || targetCol.hasStraightPotential(card)
                        || !targetCol.hasStraightPotential()
                        && cols.stream().allMatch(c -> c.index == targetCol.index || c.numberOfCards() <= 2 || !c.hasStraightPotential(card)))) {
                        List<RowCol> rows = board.findRows(r -> r.numberOfCards() == 1 && r.isEmpty(targetCol.index));
                        if (!rows.isEmpty()) {
                            rows.sort((r0, r1)
                                -> Pokers.rankDistance(r0.getAnyCard().rank, card.rank)
                                - Pokers.rankDistance(r1.getAnyCard().rank, card.rank)
                            );
                            final RowCol row = rows.get(0);
                            final int minDist = Pokers.rankDistance(row.getAnyCard().rank, card.rank);
                            if (minDist < RowCol.SIZE) {
                                while (rows.size() > 1
                                    && Pokers.rankDistance(rows.get(rows.size() - 1).getAnyCard().rank, card.rank) != minDist) {
                                    rows.remove(rows.size() - 1);
                                }
                            }
                            if (rows.size() > 1 && board.numberOfCards() < 7) {
                                final RowCol targetRow = rows.get((int) Math.floor(Math.random() * rows.size()));
                                candidates.add(new CellCandidate(targetRow.index, targetCol.index));
                                return;
                            }
                            for (final RowCol r : rows) {
                                candidates.add(new CellCandidate(r.index, targetCol.index));
                            }
                            qualifyCandidates(card);
                            return;
                        }
                        rows = board.findRows(r -> r.isEmpty(targetCol.index) && r.countRanks() == 1);
                        if (!rows.isEmpty()) {
                            if (rows.size() == 1) {
                                final RowCol targetRow = rows.get(0);
                                candidates.add(new CellCandidate(targetRow.index, targetCol.index));
                                return;
                            }
                            for (final RowCol r : rows) {
                                candidates.add(new CellCandidate(r.index, targetCol.index));
                            }
                            qualifyCandidates(card);
                            return;
                        }
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
                        qualifyCandidates(card);
                        return;
                    }
                    for (final RowCol c : cols) {
                        for (int i = 0; i < RowCol.SIZE; ++i) {
                            if (c.isEmpty(i)) {
                                candidates.add(new CellCandidate(i, c.index));
                            }
                        }
                    }
                    qualifyCandidates(card);
                    return;
                }
            }
        }
        qualifyCandidates(card);
    }

    private void qualifyCandidates(final Card card) {
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
        }
        if (candidates.size() == 1) {
            return;
        }
        final double progress = board.progress();
        final double expectedBoardScore = board.updateExpectedScore(deckTracker);
        double maxQuality = -Double.MAX_VALUE;
        for (final CellCandidate c : candidates) {
            c.quality = board.getRow(c.row).calculateCardScore(card, c.col, progress, deckTracker)
                + board.getCol(c.col).calculateCardScore(card, c.row, progress, deckTracker)
                + expectedBoardScore;
            maxQuality = Double.max(maxQuality, c.quality);
        }
        candidates.sort(CellCandidate.REVERSE_QUALITY_COMPARATOR);
        if (verbose) {
            System.out.print(candidates.size() + " raw candidates: ");
            candidates.stream().forEach((c) -> {
                System.out.print(String.format(" (%d,%d: q=%.2f)", c.row + 1, c.col + 1, c.quality));
            });
            System.out.println();
        }
        // remove bad ones
        final double qualified = maxQuality - maxQualityDifference;
        for (int i = candidates.size() - 1; i >= candidatesLimit || candidates.get(i).quality <= qualified; --i) {
            candidates.remove(i);
        }
        if (candidates.size() == 1) {
            return;
        }
        for (final CellCandidate c : candidates) {
            c.quality = c.quality / maxQuality;
        }
    }
}
