
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author Meng
 */
class PokerHandAnalyzer {

    public static final int POKER_HAND_SIZE = Board.SIZE;
    public static final Comparator<Board.RowCol> ROWCOL_COMPARATOR_IN_NUMBER_OF_CARDS = (rc0, rc1) -> rc0.numberOfCards - rc1.numberOfCards;
    public static final Comparator<Board.RowCol> REVERSE_ROWCOL_COMPARATOR_IN_NUMBER_OF_CARDS = (rc0, rc1) -> rc1.numberOfCards - rc0.numberOfCards;
    private static final int ROYAL_START = Card.NUM_RANKS - POKER_HAND_SIZE + 1;

    public static boolean hasStraightPotential(final Board.RowCol rc) {
        if (rc.numberOfCards <= 1) {
            return true;
        }
        if (rc.numberOfCards != rc.countRanks()) {
            return false;
        }
        return hasStraightPotential(collectRanks(rc.getCards()), rc.numberOfCards) != null;
    }

    public static boolean hasStraightPotential(final Board.RowCol rc, final Card card) {
        if (rc.isEmpty()) {
            return true;
        }
        if (rc.isFull() || rc.numberOfCards != rc.countRanks()) {
            return false;
        }
        final boolean[] ranks = collectRanks(rc.getCards());
        if (ranks[card.getRank()]) {
            return false;
        }
        ranks[card.getRank()] = true;
        return hasStraightPotential(ranks, rc.numberOfCards + 1) != null;
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

    public static double score(final Board.RowCol rc, final Card card,
        final PokerSquaresPointSystem pointSystem, final Board board, final DeckTracker deck) {
        final List<Card> cards = rc.getCards();
        final double score0 = score(cards, pointSystem, board, deck);
        cards.add(card);
        return score(cards, pointSystem, board, deck) - score0;
    }

    private static double score(final List<Card> cards,
        final PokerSquaresPointSystem pointSystem, final Board board, final DeckTracker deck) {
        final PokerHand hand = PokerHand.getPokerHand(cards.toArray(new Card[POKER_HAND_SIZE]));
        final int handScore = pointSystem.getHandScore(hand);
        final double progress = board.progress();
        if (cards.isEmpty()) {
            return 0.15 + progress;
        }
        if (cards.size() >= POKER_HAND_SIZE) {
            return handScore;
        }
        final int suit0 = cards.get(0).getSuit();
        final int rank0 = cards.get(0).getRank();
        final boolean isFlush = cards.stream().skip(0).allMatch(c -> c.getSuit() == suit0);
        final boolean[] ranks = collectRanks(cards);
        final int[] straightRanges = hasStraightPotential(ranks, cards.size());
        final boolean isStraight = straightRanges != null;
        final boolean isRoyal = isFlush && isStraight && (straightRanges[1] == 0 || straightRanges[0] >= ROYAL_START);
        switch (cards.size()) {
            case 4:
                switch (hand) {
                    case FOUR_OF_A_KIND:
                        return 42;
                    case THREE_OF_A_KIND:
                        return 12;
                    case TWO_PAIR:
                        return 10.1;
                    case ONE_PAIR:
                        return 2.99;
                    default:
                        if (isRoyal) {
                            if (straightRanges[1] == 0) {
                                for (int rank = ROYAL_START; rank < Card.NUM_RANKS; ++rank) {
                                    if (!ranks[rank]) {
                                        return deck.hasCard(rank, suit0) ? 14.9 - 2.9 * progress : 14 - 2 * progress;
                                    }
                                }
                                throw new IllegalStateException();
                            } else {
                                return deck.hasCard(0, suit0) || deck.hasCard(ROYAL_START - 1, suit0)
                                    ? 14.9 - 2.9 * progress
                                    : 14 - 2 * progress;
                            }
                        }
                        if (isFlush && isStraight) {
                            return 14.8 - 2.8 * progress;
                        }
                        if (isFlush) {
                            return 14 - 2 * progress;
                        }
                        return 0.1;
                }
            case 3:
                switch (hand) {
                    case THREE_OF_A_KIND:
                        return deck.hasRank(rank0) ? 14 : 11.9;
                    case ONE_PAIR:
                        return 4.35;
                    default:
                        return isFlush ? (1 - progress) * 7 + 2.8 : (isStraight ? 3.1 - 0.3 * progress : 2.8);
                }
            case 2:
                switch (hand) {
                    case ONE_PAIR:
                        return 4.4;
                    default:
                        return isFlush ? (1 - progress) * 6 + 2.9 : (isStraight ? 3 - 0.2 * progress : 2.9);
                }
        }
        return 0.16 + progress;
    }

    private static boolean[] collectRanks(final List<Card> cards) {
        final boolean[] ranks = new boolean[Card.NUM_RANKS];
        cards.stream().forEach((c) -> {
            ranks[c.getRank()] = true;
        });
        return ranks;
    }

    /**
     *
     * @param ranks
     * @param numberOfCards
     * @return {min rank, max rank} or null
     */
    private static int[] hasStraightPotential(final boolean[] ranks, final int numberOfCards) {
        int counts = 1;
        int firstRank = 0;
        int lastRank = POKER_HAND_SIZE;
        if (ranks[0]) {
            int minRank = 0;
            for (int i = ranks.length - POKER_HAND_SIZE + 1; i < ranks.length; ++i) {
                if (ranks[i]) {
                    if (minRank <= 0) {
                        minRank = i;
                    }
                    ++counts;
                }
            }
            if (counts == numberOfCards) {
                return new int[]{minRank, 0};
            } else if (counts > 1) {
                return null;
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
        int maxRank = firstRank;
        for (int i = firstRank + 1; i < lastRank; ++i) {
            if (ranks[i]) {
                maxRank = i;
                ++counts;
            }
        }
        return counts == numberOfCards ? new int[]{firstRank, maxRank} : null;
    }
}
