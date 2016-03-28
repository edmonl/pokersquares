
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

    public int getPokerHandScore(final PokerSquaresPointSystem pointSystem) {
        return pointSystem.getHandScore(getPokerHand());
    }

    @Override
    public void putCard(final Card card, final int pos) {
        super.putCard(card, pos);
    }

    @Override
    public void removeCard(final int pos) {
        super.removeCard(pos);
    }

    private PokerHand getPokerHand() {
        // numberOfCards == SIZE
        switch (rankCount) {
            case 4:
                return PokerHand.ONE_PAIR;
            case 3: {
                final int rank = getAnyCard().getRank();
                switch (ranks[rank]) {
                    case 3:
                        return PokerHand.THREE_OF_A_KIND;
                    case 2:
                        return PokerHand.TWO_PAIR;
                }
                return ranks[getAnotherRank(rank)] == 2 ? PokerHand.TWO_PAIR : PokerHand.THREE_OF_A_KIND;
            }
            case 2: {
                final int rc = ranks[getAnyCard().getRank()];
                return rc == 2 || rc == 3 ? PokerHand.FULL_HOUSE : PokerHand.FOUR_OF_A_KIND;
            }
        }
        final boolean isFlush = suitCount == 1;
        final boolean isStraight = ranks[0] > 0 && (rankRange[1] < SIZE || rankRange[0] > Card.NUM_RANKS - SIZE)
            || ranks[0] == 0 && (rankRange[1] - rankRange[0] < SIZE);
        if (isStraight) {
            if (isFlush) {
                return ranks[0] > 0 ? PokerHand.ROYAL_FLUSH : PokerHand.STRAIGHT_FLUSH;
            }
            return PokerHand.STRAIGHT;
        }
        return isFlush ? PokerHand.FLUSH : PokerHand.HIGH_CARD;
    }
}
