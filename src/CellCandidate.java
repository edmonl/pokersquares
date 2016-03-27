
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
    public static final Comparator<CellCandidate> AVERAGE_SCORE_COMPARATOR = (c0, c1) -> {
        if (c0.getAverageScore() == c1.getAverageScore()) {
            return 0;
        }
        return c0.getAverageScore() > c1.getAverageScore() ? 1 : -1;
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
    public int score = 0; // we don't have negtive score

    private double totalScore = 0.0;
    private double averageScore = 0.0;

    public CellCandidate(final Board.Cell cell) {
        this.row = cell.row;
        this.col = cell.col;
    }

    public CellCandidate(final int row, final int col) {
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
