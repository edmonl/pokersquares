package mengyaxi.pokersquares;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import mengyaxi.pokersquares.board.Board;
import mengyaxi.pokersquares.board.RowCol;

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
    private final CellCandidate[] candidateTable = new CellCandidate[CellCandidate.MAX_NUMBER];

    public Strategy(final Board board, final DeckTracker deckTracker) {
        this.board = board;
        this.deckTracker = deckTracker;
    }

    public void clear() {
        candidates.clear();
        Arrays.fill(candidateTable, null);
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
        if (rankCount == 0 && board.allRowsMatch(r -> r.countRanks() <= 2)) {
            boolean areRowsHavingUniqueRanks = true;
            for (int i = 0; i < Board.SIZE; ++i) {
                final RowCol r = board.getRow(i);
                int rank0 = r.getAnyCard().rank;
                if (r.countRank(rank0) != board.countRank(rank0)) {
                    areRowsHavingUniqueRanks = false;
                    break;
                }
                if (r.countRanks() > 1) {
                    rank0 = r.getAnotherRank(rank0);
                    if (r.countRank(rank0) != board.countRank(rank0)) {
                        areRowsHavingUniqueRanks = false;
                        break;
                    }
                }
            }
            if (areRowsHavingUniqueRanks) {
                final List<RowCol> cols = board.findCols(c -> c.hasFlushPotential(card) || c.numberOfCards() >= 2 && c.hasStraightPotential(card));
                List<RowCol> rows = board.findRows(r -> r.countRanks() == 1);
                Arrays.fill(candidateTable, null);
                if (!rows.isEmpty()) {
                    rows.sort(RowCol.NUMBER_OF_CARDS_COMPARATOR);
                    for (int i = 0; i < cols.size(); ++i) {
                        final RowCol c = cols.get(i);
                        int n = rows.get(0).numberOfCards();
                        boolean added = false;
                        for (final RowCol r : rows) {
                            if (r.numberOfCards() > n && added) {
                                break;
                            }
                            n = r.numberOfCards();
                            if (c.isEmpty(r.index)) {
                                final CellCandidate can = new CellCandidate(r.index, c.index);
                                candidateTable[can.id] = can;
                                added = true;
                            }
                        }
                        if (added) {
                            cols.set(i, null);
                        }
                    }
                    cols.removeIf(c -> c == null);
                }
                for (final RowCol c : cols) {
                    for (int i = 0; i < RowCol.SIZE; ++i) {
                        if (c.isEmpty(i)) {
                            final CellCandidate can = new CellCandidate(i, c.index);
                            candidateTable[can.id] = can;
                        }
                    }
                }
                rows = board.findRows(r -> r.hasFlushPotential(card) || r.numberOfCards() >= 2 && r.hasStraightPotential(card));
                for (final RowCol r : rows) {
                    for (int i = 0; i < RowCol.SIZE; ++i) {
                        if (r.isEmpty(i)) {
                            final CellCandidate can = new CellCandidate(r.index, i);
                            candidateTable[can.id] = can;
                        }
                    }
                }
                for (final CellCandidate c : candidateTable) {
                    if (c != null) {
                        candidates.add(c);
                    }
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
