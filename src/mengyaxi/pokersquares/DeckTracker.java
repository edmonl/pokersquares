package mengyaxi.pokersquares;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Meng
 */
public final class DeckTracker {

    private final boolean[] bookkeepingByCardId = new boolean[Card.NUM_CARDS]; // card id -> if the card has not been dealt
    private int numberOfCards;

    public DeckTracker() {
        clear();
    }

    public void copyFrom(final DeckTracker deck) {
        System.arraycopy(deck.bookkeepingByCardId, 0, bookkeepingByCardId, 0, Card.NUM_CARDS);
        numberOfCards = deck.numberOfCards;
    }

    public int getNumberOfCards() {
        return numberOfCards;
    }

    public boolean isEmpty() {
        return numberOfCards == 0;
    }

    public boolean hasRank(final int rank) {
        for (int i = rank; i < bookkeepingByCardId.length; i += Card.NUM_RANKS) {
            if (bookkeepingByCardId[i]) {
                return true;
            }
        }
        return false;
    }

    public boolean hasCard(final int rank, final int suit) {
        return bookkeepingByCardId[suit * Card.NUM_RANKS + rank];
    }

    public int countSuit(final int suit) {
        int n = 0;
        int i = suit * Card.NUM_RANKS;
        final int end = i + Card.NUM_RANKS;
        for (; i < end; ++i) {
            if (bookkeepingByCardId[i]) {
                ++n;
            }
        }
        return n;
    }

    public int countRank(final int rank) {
        int n = 0;
        for (int i = rank; i < bookkeepingByCardId.length; i += Card.NUM_RANKS) {
            if (bookkeepingByCardId[i]) {
                ++n;
            }
        }
        return n;
    }

    public List<Card> getCards() {
        final List<Card> deck = new ArrayList<>(numberOfCards);
        for (int id = 0; id < bookkeepingByCardId.length; ++id) {
            if (bookkeepingByCardId[id]) {
                deck.add(Card.getCardById(id));
            }
        }
        return deck;
    }

    public void deal(final Card card) {
        if (!bookkeepingByCardId[card.id]) {
            throw new IllegalArgumentException("The card " + card + " has been dealt.");
        }
        --numberOfCards;
        bookkeepingByCardId[card.id] = false;
    }

    public void putBack(final Card card) {
        if (bookkeepingByCardId[card.id]) {
            throw new IllegalArgumentException("The card " + card + " has not been dealt.");
        }
        ++numberOfCards;
        bookkeepingByCardId[card.id] = true;
    }

    public final void clear() {
        Arrays.fill(bookkeepingByCardId, true);
        numberOfCards = bookkeepingByCardId.length;
    }
}
