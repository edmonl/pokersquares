
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Meng
 */
final class DeckTracker implements Iterable<Card> {

    private final class Iterator implements java.util.Iterator<Card> {

        private int next;

        public Iterator(final int next) {
            this.next = next;
        }

        @Override
        public boolean hasNext() {
            return next < bookkeepingByCardId.length;
        }

        @Override
        public Card next() {
            final Card card = Card.getCard(next);
            next = findNext(next + 1);
            return card;
        }
    }

    private final boolean[] bookkeepingByCardId = new boolean[Card.NUM_CARDS]; // card id -> if the card has not been dealt
    private int numberOfCards;

    public DeckTracker() {
        reset();
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
                deck.add(Card.getCard(id));
            }
        }
        return deck;
    }

    public void deal(final Card card) {
        final int cardId = card.getCardId();
        if (!bookkeepingByCardId[cardId]) {
            throw new IllegalArgumentException("The card " + card + " has been dealt.");
        }
        --numberOfCards;
        bookkeepingByCardId[cardId] = false;
    }

    public void putBack(final Card card) {
        final int cardId = card.getCardId();
        if (bookkeepingByCardId[cardId]) {
            throw new IllegalArgumentException("The card " + card + " has not been dealt.");
        }
        ++numberOfCards;
        bookkeepingByCardId[cardId] = true;
    }

    public final void reset() {
        Arrays.fill(bookkeepingByCardId, true);
        numberOfCards = bookkeepingByCardId.length;
    }

    @Override
    public java.util.Iterator<Card> iterator() {
        return new Iterator(findNext(0));
    }

    private int findNext(int current) {
        if (isEmpty()) {
            return bookkeepingByCardId.length;
        }
        while (current < bookkeepingByCardId.length && !bookkeepingByCardId[current]) {
            ++current;
        }
        return current;
    }
}
