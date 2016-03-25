
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 *
 * @author Meng
 */
class RowCol {

    public static final int SIZE = Board.SIZE;

    public final int index;

    protected int numberOfCards = 0;
    protected int rankCount = 0;
    protected int suitCount = 0;
    protected final Card[] positions = new Card[SIZE];
    protected final int[] ranks = new int[Card.NUM_RANKS];
    protected final int[] suits = new int[Card.NUM_SUITS];
    protected final int[] rankRange = new int[2];
    protected int anyCardPosition = -1;

    public RowCol(final int index) {
        this.index = index;
        rankRange[0] = Card.NUM_RANKS;
        rankRange[1] = 0;
    }

    public final Card getCard(final int pos) {
        return positions[pos];
    }

    public final PokerHand getPokerHand() {
        if (numberOfCards <= 1) {
            return PokerHand.HIGH_CARD;
        }
        if (rankCount == 1) {
            switch (numberOfCards) {
                case 2:
                    return PokerHand.ONE_PAIR;
                case 3:
                    return PokerHand.THREE_OF_A_KIND;
                case 4:
                    return PokerHand.FOUR_OF_A_KIND;
            }
            throw new IllegalStateException();
        }
        if (numberOfCards - rankCount == 1) {
            return PokerHand.ONE_PAIR;
        }
        if (rankCount == numberOfCards) {
            if (numberOfCards == SIZE) {
                if (hasStraightPotential()) {
                    if (suitCount == 1) {
                        return ranks[0] > 0 ? PokerHand.ROYAL_FLUSH : PokerHand.STRAIGHT_FLUSH;
                    }
                    return PokerHand.STRAIGHT;
                }
                if (suitCount == 1) {
                    return PokerHand.FLUSH;
                }
            }
            return PokerHand.HIGH_CARD;
        }
        int max = 0;
        int min = SIZE;
        for (final Card c : positions) {
            if (c != null) {
                final int r = ranks[c.getRank()];
                max = Integer.max(r, max);
                min = Integer.min(r, min);
            }
        }
        if (max == 4) {
            return PokerHand.FOUR_OF_A_KIND;
        }
        if (max == 3) {
            return min == 2 ? PokerHand.FULL_HOUSE : PokerHand.THREE_OF_A_KIND;
        }
        return PokerHand.TWO_PAIR;
    }

    public final PokerHand getPokerHand(final Card card) {
        final int p = findFirstEmptyPosition();
        putCard(card, p);
        final PokerHand ph = getPokerHand();
        removeCard(p);
        return ph;
    }

    public final int getPokerHandScore(final PokerSquaresPointSystem pointSystem) {
        return pointSystem.getHandScore(getPokerHand());
    }

    public final int getPokerHandScore(final Card card, final PokerSquaresPointSystem pointSystem) {
        return pointSystem.getHandScore(getPokerHand(card));
    }

    public final double scoreCard(final Card card, final int pos,
        final PokerSquaresPointSystem pointSystem, final Board board, final DeckTracker deck) {
        final double score0 = score(pointSystem, board, deck);
        putCard(card, pos);
        final double score1 = score(pointSystem, board, deck);
        removeCard(pos);
        return score1 - score0;
    }

    public final boolean hasStraightPotential() {
        if (numberOfCards <= 1) {
            return true;
        }
        if (numberOfCards != rankCount) {
            return false;
        }
        return ranks[0] > 0 && (rankRange[1] < SIZE || rankRange[0] > Card.NUM_RANKS - SIZE)
            || ranks[0] == 0 && (rankRange[1] - rankRange[0] < SIZE);
    }

    public final boolean hasStraightPotential(final Card card) {
        if (isFull()) {
            return false;
        }
        final int p = findFirstEmptyPosition();
        putCard(card, p);
        final boolean sp = hasStraightPotential();
        removeCard(p);
        return sp;
    }

    public final boolean hasFlushPotential() {
        return suitCount <= 1;
    }

    public final boolean hasFlushPotential(final Card card) {
        if (isFull()) {
            return false;
        }
        final int p = findFirstEmptyPosition();
        putCard(card, p);
        final boolean fp = hasFlushPotential();
        removeCard(p);
        return fp;
    }

    public final int size() {
        return SIZE;
    }

    public final int numberOfCards() {
        return numberOfCards;
    }

    public final int lastPosition() {
        return SIZE - 1;
    }

    public final boolean isLast() {
        return index == SIZE - 1;
    }

    public final boolean isEmpty() {
        return numberOfCards == 0;
    }

    public final boolean isLastPositionEmpty() {
        return positions[lastPosition()] == null;
    }

    public final boolean isEmpty(final int index
    ) {
        return positions[index] == null;
    }

    public final boolean isFull() {
        return numberOfCards >= SIZE;
    }

    public final List<Card> getCards() {
        final List<Card> results = new ArrayList<>(SIZE);
        for (final Card c : positions) {
            if (c != null) {
                results.add(c);
            }
        }
        return results;
    }

    public final int findFirstEmptyPosition() {
        for (int i = 0; i < SIZE; ++i) {
            if (positions[i] == null) {
                return i;
            }
        }
        return -1;
    }

    public final boolean allCardsMatch(final Predicate<Card> p
    ) {
        for (final Card c : positions) {
            if (c != null && !p.test(c)) {
                return false;
            }
        }
        return true;
    }

    public final boolean hasRank(final int rank) {
        return ranks[rank] > 0;
    }

    public final boolean hasSuit(final int suit) {
        return suits[suit] > 0;
    }

    public final int countRanks() {
        return rankCount;
    }

    public final int countRank(final int rank) {
        return ranks[rank];
    }

    public final int countSuits() {
        return suitCount;
    }

    public final Card getAnyCard() {
        return anyCardPosition >= 0 ? positions[anyCardPosition] : null;
    }

    public final Card findFirstCard(final Predicate<Card> p) {
        for (final Card c : positions) {
            if (c != null && p.test(c)) {
                return c;
            }
        }
        return null;
    }

    protected void putCard(final Card card, final int pos) {
        if (positions[pos] != null) {
            throw new IllegalArgumentException();
        }
        ++numberOfCards;
        if (ranks[card.getRank()] == 0) {
            ++rankCount;
            if (!card.isAce()) {
                rankRange[0] = Integer.min(card.getRank(), rankRange[0]);
                rankRange[1] = Integer.max(card.getRank(), rankRange[1]);
            }
        }
        suitCount += suits[card.getSuit()] == 0 ? 1 : 0;
        positions[pos] = card;
        ++ranks[card.getRank()];
        ++suits[card.getSuit()];
        if (numberOfCards == 1) {
            anyCardPosition = pos;
        }
    }

    protected void removeCard(final int pos) {
        final Card card = positions[pos];
        if (card == null) {
            throw new IllegalArgumentException();
        }
        --suits[card.getSuit()];
        --ranks[card.getRank()];
        positions[pos] = null;
        suitCount -= suits[card.getSuit()] == 0 ? 1 : 0;
        if (ranks[card.getRank()] == 0) {
            --rankCount;
            if (!card.isAce()) {
                if (rankRange[0] >= rankRange[1]) {
                    rankRange[0] = Card.NUM_RANKS;
                    rankRange[1] = 0;
                } else if (card.getRank() == rankRange[0]) {
                    ++rankRange[0];
                    while (ranks[rankRange[0]] <= 0) {
                        ++rankRange[0];
                    }
                } else if (card.getRank() == rankRange[1]) {
                    --rankRange[1];
                    while (ranks[rankRange[1]] <= 0) {
                        --rankRange[1];
                    }
                }
            }
        }
        --numberOfCards;
        if (numberOfCards == 0) {
            anyCardPosition = -1;
        } else if (pos == anyCardPosition) {
            for (int i = 0; i < SIZE; ++i) {
                if (positions[i] != null) {
                    anyCardPosition = i;
                    break;
                }
            }
        }
    }

    private double score(final PokerSquaresPointSystem pointSystem, final Board board, final DeckTracker deck) {
        final PokerHand hand = getPokerHand();
        final int handScore = pointSystem.getHandScore(hand);
        final double progress = board.progress();
        if (isEmpty()) {
            return 1.45;
        }
        if (numberOfCards >= SIZE) {
            return handScore;
        }
        final Card card = getAnyCard();
        final int suit0 = card.getSuit();
        final int rank0 = card.getRank();
        final boolean isFlush = suitCount == 1;
        final boolean isStraight = hasStraightPotential();
        final boolean isRoyal = isFlush && isStraight && rankRange[0] > Card.NUM_RANKS - SIZE;
        switch (numberOfCards) {
            case 4:
                switch (hand) {
                    case FOUR_OF_A_KIND:
                        return 51;
                    case THREE_OF_A_KIND: {
                        int goodRank = rank0, otherRank = rank0;
                        if (countRank(rank0) == 1) {
                            goodRank = findFirstCard(c -> c.getRank() != rank0).getRank();
                        } else {
                            otherRank = findFirstCard(c -> c.getRank() != rank0).getRank();
                        }
                        return deck.hasRank(goodRank) ? 10.9 + 2.5 * deck.countRank(otherRank) : 10.9 + deck.countRank(otherRank);
                    }
                    case TWO_PAIR:
                        return 9.1;
                    case ONE_PAIR:
                        return 2.99;
                    default:
                        if (isRoyal) {
                            if (ranks[0] > 0) {
                                for (int rank = Card.NUM_RANKS - SIZE + 1; rank < Card.NUM_RANKS; ++rank) {
                                    if (ranks[rank] == 0) {
                                        return deck.hasCard(rank, suit0) ? 14.9 - 2.9 * progress : 14 - 2 * progress;
                                    }
                                }
                                throw new IllegalStateException();
                            } else {
                                return deck.hasCard(0, suit0) ? 14.9 - 2.9 * progress
                                    : (deck.hasCard(Card.NUM_RANKS - SIZE, suit0)
                                    ? 14.8 - 2.8 * progress : 14 - 2 * progress);
                            }
                        }
                        if (isFlush) {
                            return isStraight ? 14.9 : 14 - 2 * progress;
                        }
                        if (isStraight) {
                            int n = 0;
                            int rank = rankRange[0];
                            int rankEnd = rankRange[1] + 1;
                            if (rankEnd - rank < SIZE) {
                                if (rank > 0) {
                                    --rank;
                                }
                                if (rankEnd < Card.NUM_RANKS) {
                                    ++rankEnd;
                                }
                            }
                            for (; rank < rankEnd; ++rank) {
                                if (ranks[rank] > 0) {
                                    n += deck.countRank(rank);
                                }
                            }
                            return n * 0.9 + 1.1;
                        }
                        return 1.1;
                }
            case 3:
                switch (hand) {
                    case THREE_OF_A_KIND:
                        return deck.hasRank(rank0) ? 14 : 11.9;
                    case ONE_PAIR:
                        return 4.35;
                    default:
                        return isFlush ? (1 - progress) * 4 + 5.9 : (isStraight ? 2.7 - 0.6 * progress : 2.1);
                }
            case 2:
                switch (hand) {
                    case ONE_PAIR:
                        return 4.4;
                    default:
                        return isFlush ? (1 - progress) * 4 + 4.5 : (isStraight ? 2.5 - 0.3 * progress : 2.2);
                }
        }
        return 1.5;
    }
}
