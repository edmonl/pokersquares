package mengyaxi.pokersquares;

/**
 * Card - Playing card class representing all 52 cards of a French deck.
 *
 * @author Meng
 */
public class Card {

    private static final String[] RANK_NAMES = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "T", "J", "Q", "K"}; // all single-character rank names
    private static final String[] SUIT_NAMES = {"C", "D", "H", "S"}; // all single-character suit names

    public static final int NUM_RANKS = RANK_NAMES.length;
    public static final int NUM_SUITS = SUIT_NAMES.length;
    public static final int NUM_CARDS = NUM_RANKS * NUM_SUITS;

    private static final Card[] CARDS = new Card[NUM_CARDS]; // index is the id

    static { // create all card objects
        for (int suit = 0; suit < SUIT_NAMES.length; ++suit) {
            for (int rank = 0; rank < RANK_NAMES.length; ++rank) {
                final Card c = new Card(rank, suit);
                CARDS[c.id] = c;
            }
        }
    }

    /**
     * Get the Card object associated with the given card identification
     * integer.
     *
     * @param id the unique integer identification number for the desired card
     * @return the Card object associated with the given card identification
     * integer
     */
    public static Card getCardById(final int id) {
        return CARDS[id];
    }

    // Non-static definitions
    public final int rank; // the index of the rank String in rankNames
    public final int suit; // the index of the suit String in suitNames

    /**
     * The unique integer card identification number accords to "suit-major"
     * ordering where cards are ordered A, 2, ..., K within suits that are
     * ordered alphabetically. This identification number is the 0-based suit
     * integer times the number of ranks plus the 0-based rank number.
     */
    public final int id;

    /**
     * Return whether or not the card is an ace.
     *
     * @return whether or not the card is an ace
     */
    public boolean isAce() {
        return rank == 0;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public java.lang.String toString() {
        return RANK_NAMES[rank] + SUIT_NAMES[suit];
    }

    /**
     * Create a card with the given rank and suit.
     *
     * @param rank Card rank. Should be in range [0, NUM_RANKS - 1].
     * @param suit Card suit. Should be in range [0, NUM_SUITS - 1].
     */
    private Card(final int rank, final int suit) {
        this.rank = rank;
        this.suit = suit;
        id = suit * NUM_RANKS + rank;
    }
}
