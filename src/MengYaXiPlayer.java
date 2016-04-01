
/**
 *
 * @author Meng
 */
public final class MengYaXiPlayer extends mengyaxi.pokersquares.PokerSquaresPlayer implements PokerSquaresPlayer {

    @Override
    public void setPointSystem(final PokerSquaresPointSystem system, final long millis) {
        // the American point system is guaranteed.
    }

    @Override
    public int[] getPlay(final Card card, final long millisRemaining) {
        return super.getPlay(mengyaxi.pokersquares.Card.getCardById(card.getCardId()), millisRemaining);
    }
}
