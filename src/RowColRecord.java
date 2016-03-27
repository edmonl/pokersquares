
import java.util.Arrays;

/**
 *
 * @author Meng
 */
final class RowColRecord extends RowCol {

    public RowColRecord(final int index) {
        super(index);
    }

    public void copyFrom(final RowColRecord rc) {
        numberOfCards = rc.numberOfCards;
        rankCount = rc.rankCount;
        suitCount = rc.suitCount;
        System.arraycopy(rc.positions, 0, positions, 0, SIZE);
        System.arraycopy(rc.ranks, 0, ranks, 0, Card.NUM_RANKS);
        System.arraycopy(rc.suits, 0, suits, 0, Card.NUM_SUITS);
        System.arraycopy(rc.rankRange, 0, rankRange, 0, rankRange.length);
        anyCardPosition = rc.anyCardPosition;
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
        anyCardPosition = -1;
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
