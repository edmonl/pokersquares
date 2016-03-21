
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Meng
 */
final class Cards implements Iterable<Card> {

    private final List<Card> cards;

    public Cards(final Card[] cards) {
        this.cards = new ArrayList<>();
        for (final Card c : cards) {
            if (c != null) {
                this.cards.add(c);
            }
        }
    }

    public int size() {
        return cards.size();
    }

    @Override
    public java.util.Iterator<Card> iterator() {
        return cards.iterator();
    }
}
