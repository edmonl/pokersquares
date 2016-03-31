package mengyaxi.pokersquares;

import java.util.Comparator;
import mengyaxi.pokersquares.board.Board;

/**
 *
 * @author Meng
 */
final class CellCandidate {

    public static final int MAX_NUMBER = Board.NUMBER_OF_CELLS;
    public static final Comparator<CellCandidate> SCORE_COMPARATOR = (c0, c1) -> c0.score - c1.score;
    public static final Comparator<CellCandidate> TOTAL_SCORE_COMPARATOR = (c0, c1) -> c0.totalScore - c1.totalScore;
    public static final Comparator<CellCandidate> AVERAGE_SCORE_COMPARATOR = (c0, c1) -> {
        if (c0.averageScore == c1.averageScore) {
            return 0;
        }
        return c0.averageScore > c1.averageScore ? 1 : -1;
    };
    public static final Comparator<CellCandidate> REVERSE_QUALITY_COMPARATOR = (c0, c1) -> {
        if (c0.quality == c1.quality) {
            return 0;
        }
        return c0.quality > c1.quality ? -1 : 1;
    };
    public static final Comparator<CellCandidate> QUALITY_COMPARATOR = (c0, c1) -> {
        if (c0.quality == c1.quality) {
            return 0;
        }
        return c0.quality < c1.quality ? -1 : 1;
    };

    public final int row, col;
    public final int id;
    public double quality = 0.0, p = 0.0;
    public int score = 0; // we don't have negtive score
    public int totalScore = 0;
    public double averageScore = 0.0;

    public CellCandidate(final int row, final int col) {
        this.row = row;
        this.col = col;
        id = row * Board.SIZE + col;
    }
}
