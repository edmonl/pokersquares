package mengyaxi.pokersquares.board;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import mengyaxi.pokersquares.Card;
import mengyaxi.pokersquares.DeckTracker;
import mengyaxi.pokersquares.util.Pokers;

/**
 *
 * @author Meng
 */
public class RowCol {

    public static final Comparator<RowCol> NUMBER_OF_CARDS_COMPARATOR = (rc0, rc1) -> rc0.numberOfCards() - rc1.numberOfCards();
    public static final Comparator<RowCol> REVERSE_NUMBER_OF_CARDS_COMPARATOR = (rc0, rc1) -> rc1.numberOfCards() - rc0.numberOfCards();

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
    private double expectedSccore = 0.0;

    public RowCol(final int index) {
        this.index = index;
        rankRange[0] = Card.NUM_RANKS;
        rankRange[1] = 0;
    }

    public final Card getCard(final int pos) {
        return positions[pos];
    }

    public final double calculateCardScore(final Card card, final int pos, final double progress, final DeckTracker deck) {
        deck.deal(card);
        putCard(card, pos);
        final double score1 = calculateExpectedScore(progress, deck);
        removeCard(pos);
        deck.putBack(card);
        return score1 - expectedSccore;
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
        if (isEmpty()) {
            return true;
        }
        final int newRank = card.rank;
        if (ranks[newRank] > 0 || isFull() || !hasStraightPotential()) {
            return false;
        }
        if (rankCount == 1) {
            return Pokers.rankDistance(newRank, getAnyCard().rank) < SIZE;
        }
        if (newRank == 0) {
            return rankRange[1] < SIZE || rankRange[0] > Card.NUM_RANKS - SIZE;
        }
        if (ranks[0] > 0) {
            return rankRange[1] < SIZE && newRank < SIZE || rankRange[0] > Card.NUM_RANKS - SIZE && newRank > Card.NUM_RANKS - SIZE;
        }
        return Integer.max(rankRange[1], newRank) - Integer.min(rankRange[0], newRank) < SIZE;
    }

    public final boolean hasFlushPotential() {
        return suitCount <= 1;
    }

    public final boolean hasFlushPotential(final Card card) {
        return isEmpty() || !isFull() && suitCount <= 1 && getAnyCard().suit == card.suit;
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

    public final boolean isEmpty(final int index) {
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

    public final int getAnotherRank(final int rank) {
        if (isEmpty()) {
            return -1;
        }
        if (ranks[0] > 0) {
            if (rank != 0) {
                return 0;
            }
            return rankCount == 1 ? -1 : rankRange[0];
        }
        return rank == rankRange[0] ? (rank == rankRange[1] ? -1 : rankRange[1]) : rankRange[0];
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
        if (ranks[card.rank] == 0) {
            ++rankCount;
            if (!card.isAce()) {
                rankRange[0] = Integer.min(card.rank, rankRange[0]);
                rankRange[1] = Integer.max(card.rank, rankRange[1]);
            }
        }
        suitCount += suits[card.suit] == 0 ? 1 : 0;
        positions[pos] = card;
        ++ranks[card.rank];
        ++suits[card.suit];
        if (numberOfCards == 1) {
            anyCardPosition = pos;
        }
    }

    protected void removeCard(final int pos) {
        final Card card = positions[pos];
        if (card == null) {
            throw new IllegalArgumentException();
        }
        --suits[card.suit];
        --ranks[card.rank];
        positions[pos] = null;
        suitCount -= suits[card.suit] == 0 ? 1 : 0;
        if (ranks[card.rank] == 0) {
            --rankCount;
            if (!card.isAce()) {
                if (rankRange[0] >= rankRange[1]) {
                    rankRange[0] = Card.NUM_RANKS;
                    rankRange[1] = 0;
                } else if (card.rank == rankRange[0]) {
                    ++rankRange[0];
                    while (ranks[rankRange[0]] <= 0) {
                        ++rankRange[0];
                    }
                } else if (card.rank == rankRange[1]) {
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

    final double updateExpectedScore(final double progress, final DeckTracker deck) {
        expectedSccore = calculateExpectedScore(progress, deck);
        return expectedSccore;
    }

    private double calculateExpectedScore(final double progress, final DeckTracker deck) {
        if (numberOfCards == 0) {
            return 1.45;
        }
        if (numberOfCards == 1) {
            return 1.9;
        }
        if (rankCount == 1) {
            switch (numberOfCards) {
                case 2:
                    return 4.4; // PokerHand.ONE_PAIR
                case 3:
                    return deck.hasRank(getAnyCard().rank) ? 14 : 11.9; // PokerHand.THREE_OF_A_KIND
            }
            return 51; // PokerHand.FOUR_OF_A_KIND
        }
        switch (numberOfCards - rankCount) {
            case 1: // PokerHand.ONE_PAIR
                switch (numberOfCards) {
                    case 3:
                        return 4.35;
                    case 4:
                        return 2.99;
                }
                return 2;
            case 2: {
                int rank = getAnyCard().rank;
                if (numberOfCards == 4) {
                    if (ranks[rank] == 2) { // PokerHand.TWO_PAIR
                        return 9.1;
                    }
                    // PokerHand.THREE_OF_A_KIND
                    int otherRank = rank;
                    if (ranks[rank] == 1) {
                        rank = getAnotherRank(rank);
                    } else {
                        otherRank = getAnotherRank(rank);
                    }
                    return deck.hasRank(rank) ? 13.9 + deck.countRank(otherRank) : 11.9 + deck.countRank(otherRank);
                }
                // numberOfCards == 5
                switch (ranks[rank]) {
                    case 3:
                        return 10; // PokerHand.THREE_OF_A_KIND;
                    case 2:
                        return 5; // PokerHand.TWO_PAIR
                }
                return ranks[getAnotherRank(rank)] == 2 ? 5/*PokerHand.TWO_PAIR*/ : 10/*PokerHand.THREE_OF_A_KIND*/;
            }
            case 3: // numberOfCards == 5
                switch (ranks[getAnyCard().rank]) {
                    case 1:
                    case 4:
                        return 50; // PokerHand.FOUR_OF_A_KIND
                }
                return 25; // PokerHand.FULL_HOUSE
        }
        // numberOfCards == rankCount
        final boolean isFlush = suitCount == 1;
        final boolean isStraight = ranks[0] > 0 && (rankRange[1] < SIZE || rankRange[0] > Card.NUM_RANKS - SIZE)
            || ranks[0] == 0 && (rankRange[1] - rankRange[0] < SIZE);
        switch (numberOfCards) {
            case 2:
                return isFlush ? (1 - progress) * 4 + 4.5 : (isStraight ? 2.5 - 0.3 * progress : 2.2);
            case 3:
                return isFlush ? (1 - progress) * 4 + 5.9 : (isStraight ? 2.7 - 0.6 * progress : 2.1);
            case 5:
                if (isStraight) {
                    if (isFlush) {
                        return rankRange[0] > Card.NUM_RANKS - SIZE
                            ? 100 // PokerHand.ROYAL_FLUSH
                            : 75; // PokerHand.STRAIGHT_FLUSH
                    }
                    return 15; // PokerHand.STRAIGHT
                }
                return isFlush ? 20/*PokerHand.FLUSH*/ : 0/*PokerHand.HIGH_CARD*/;
        }
        if (isStraight) {
            if (isFlush) {
                if (rankRange[0] > Card.NUM_RANKS - SIZE) {
                    if (ranks[0] > 0) {
                        for (int rank = Card.NUM_RANKS - SIZE + 1; rank < Card.NUM_RANKS; ++rank) {
                            if (ranks[rank] == 0) {
                                return deck.hasCard(rank, getAnyCard().suit) ? 14.9 - 2.9 * progress : 14 - 2 * progress;
                            }
                        }
                        throw new IllegalStateException();
                    }
                    final int suit0 = getAnyCard().suit;
                    return deck.hasCard(0, suit0) ? 14.9 - 2.9 * progress
                        : (deck.hasCard(Card.NUM_RANKS - SIZE, suit0)
                        ? 14.8 - 2.8 * progress : 14 - 2 * progress);
                }
                return 14.9;
            }
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
        return isFlush ? 14 - 2 * progress : 1.1;
    }
}
