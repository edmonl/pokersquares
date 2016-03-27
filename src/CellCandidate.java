
import java.util.Comparator;

/**
 *
 * @author Meng
 */
final class CellCandidate {

    public static final Comparator<CellCandidate> SCORE_COMPARATOR = (c0, c1) -> {
        if (c0.score == c1.score) {
            return 0;
        }
        return c0.score > c1.score ? 1 : -1;
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
    public double quality = 0.0;
    public double score = 0; // we don't have negtive score

    public CellCandidate(final int row, final int col) {
        this.row = row;
        this.col = col;
    }
}
