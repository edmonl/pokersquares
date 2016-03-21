
import java.util.Comparator;

/**
 *
 * @author Meng
 */
class PokerHandAnalyzer {

    public static final int POKER_HAND_SIZE = Board.SIZE;
    public static final Comparator<Board.RowCol> ROWCOL_COMPARATOR_IN_NUMBER_OF_CARDS = (rc0, rc1) -> rc0.numberOfCards - rc1.numberOfCards;
    public static final Comparator<Board.RowCol> REVERSE_ROWCOL_COMPARATOR_IN_NUMBER_OF_CARDS = (rc0, rc1) -> rc1.numberOfCards - rc0.numberOfCards;

    public static boolean hasStraightPotential(final Board.RowCol rc) {
        if (rc.numberOfCards <= 1) {
            return true;
        }
        if (rc.numberOfCards != rc.countRanks()) {
            return false;
        }
        return hasStraightPotential(collectRanks(rc), rc.numberOfCards);
    }

    public static boolean hasStraightPotential(final Board.RowCol rc, final Card card) {
        if (rc.isEmpty()) {
            return true;
        }
        if (rc.isFull() || rc.numberOfCards != rc.countRanks()) {
            return false;
        }
        final boolean[] ranks = collectRanks(rc);
        if (ranks[card.getRank()]) {
            return false;
        }
        ranks[card.getRank()] = true;
        return hasStraightPotential(ranks, rc.numberOfCards + 1);
    }

    public static boolean isStraight(final Board.RowCol rc) {
        return rc.isFull() && hasStraightPotential(rc);
    }

    public static boolean isStraight(final Board.RowCol rc, final Card card) {
        return rc.numberOfCards == POKER_HAND_SIZE - 1 && hasStraightPotential(rc, card);
    }

    public static boolean hasFlushPotential(final Board.RowCol rc) {
        return rc.isEmpty() || rc.countSuits() == 1;
    }

    public static boolean hasFlushPotential(final Board.RowCol rc, final Card card) {
        return rc.allCardsMatch(c -> c.getSuit() == card.getSuit());
    }

    public static boolean isFlush(final Board.RowCol rc) {
        return rc.numberOfCards == POKER_HAND_SIZE && rc.countSuits() == 1;
    }

    public static boolean isFlush(final Board.RowCol rc, final Card card) {
        return rc.numberOfCards == POKER_HAND_SIZE - 1 && rc.allCardsMatch(c -> c.getSuit() == card.getSuit());
    }

    public static boolean hasStraightFlushPotential(final Board.RowCol rc) {
        return hasStraightPotential(rc) && hasFlushPotential(rc);
    }

    public static boolean hasStraightFlushPotential(final Board.RowCol rc, final Card card) {
        return hasStraightPotential(rc, card) && hasFlushPotential(rc, card);
    }

    public static boolean isStraightFlush(final Board.RowCol rc) {
        return isStraight(rc) && isFlush(rc);
    }

    public static boolean isStraightFlush(final Board.RowCol rc, final Card card) {
        return isStraight(rc, card) && isFlush(rc, card);
    }

    public static boolean hasRoyalFlushPotential(final Board.RowCol rc) {
        if (!hasStraightPotential(rc) || hasFlushPotential(rc)) {
            return false;
        }
        return rc.allCardsMatch(c -> c.isAce() || c.getRank() >= 9 && c.getRank() <= 12);
    }

    public static boolean hasRoyalFlushPotential(final Board.RowCol rc, final Card card) {
        if (!hasStraightPotential(rc, card) || hasFlushPotential(rc, card)) {
            return false;
        }
        return (card.isAce() || card.getRank() >= 9 && card.getRank() <= 12)
            && rc.allCardsMatch(c -> c.isAce() || c.getRank() >= 9 && c.getRank() <= 12);
    }

    public static boolean isRoyalFlush(final Board.RowCol rc) {
        return isStraightFlush(rc) && rc.hasRank(0) && rc.hasRank(12);
    }

    public static boolean isRoyalFlush(final Board.RowCol rc, final Card card) {
        if (!isStraightFlush(rc, card)) {
            return false;
        }
        if (card.isAce()) {
            return rc.hasRank(12);
        }
        if (card.getRank() == 12) {
            return rc.hasRank(0);
        }
        return rc.hasRank(0) && rc.hasRank(12);
    }

//    public static int maxPotentialScore(final Board.RowCol rc, final Card card, final PokerSquaresPointSystem pointSystem) {
//        if (hasRoyalFlushPotential(rc, card)) {
//            return pointSystem.getHandScore(PokerHand.ROYAL_FLUSH);
//        }
//        if (hasStraightFlushPotential(rc, card)) {
//            return pointSystem.getHandScore(PokerHand.STRAIGHT_FLUSH);
//        }
//        if (hasFlushPotential(rc, card)) {
//            return pointSystem.getHandScore(PokerHand.FLUSH)
//        }
//    }
    private static boolean[] collectRanks(final Board.RowCol rc) {
        final boolean[] ranks = new boolean[Card.NUM_RANKS];
        for (final Card c : rc.getCards()) {
            ranks[c.getRank()] = true;
        }
        return ranks;
    }

    private static boolean hasStraightPotential(final boolean[] ranks, final int numberOfCards) {
        int counts = 1;
        int firstRank = 0;
        int lastRank = POKER_HAND_SIZE;
        if (ranks[0]) {
            for (int i = ranks.length - POKER_HAND_SIZE + 1; i < ranks.length; ++i) {
                if (ranks[i]) {
                    ++counts;
                }
            }
            if (counts == numberOfCards) {
                return true;
            } else if (counts > 1) {
                return false;
            }
        } else {
            do {
                ++firstRank;
            } while (firstRank < ranks.length && !ranks[firstRank]);
            lastRank = firstRank + POKER_HAND_SIZE;
            if (lastRank > ranks.length) {
                lastRank = ranks.length;
            }
        }
        for (++firstRank; firstRank < lastRank; ++firstRank) {
            if (ranks[firstRank]) {
                ++counts;
            }
        }
        return counts == numberOfCards;
    }
}
