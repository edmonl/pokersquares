
import java.util.Arrays;

/**
 *
 * @author Meng
 */
final class RowColRecord extends RowCol {

    public RowColRecord(final int index) {
        super(index);
    }

    public void clear() {
        numberOfCards = 0;
        rankCount = 0;
        suitCount = 0;
        Arrays.fill(positions, null);
        Arrays.fill(ranks, 0);
        Arrays.fill(suits, 0);
        rankRange[0] = Card.NUM_RANKS;
        rankRange[1] = 0;
    }

    @Override
    public void putCard(final Card card, final int pos) {
        super.putCard(card, pos);
    }

    @Override
    public void removeCard(final int pos) {
        super.removeCard(pos);
    }
}
