
import java.util.Scanner;

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

    public static void main(final String[] args) {
        final MengYaXiPlayer player = new MengYaXiPlayer();

        int times = 1;
        long seed = System.currentTimeMillis();
        int argn = 0;
        boolean interactive = false;
        for (final String arg : args) {
            if (arg.equals("-v")) {
                player.verbose = true;
            } else if (arg.equals("-s")) {
                player.parallel = false;
            } else if (arg.equals("-i")) {
                interactive = true;
            } else if (argn == 0) {
                times = Integer.parseUnsignedInt(arg);
                ++argn;
            } else {
                seed = Long.parseUnsignedLong(arg);
                ++argn;
            }
        }

        final PokerSquares game = new PokerSquares(player, PokerSquaresPointSystem.getAmericanPointSystem());
        if (interactive) {
            //game.setSeed(times + seed);
            game.play(new Scanner(System.in));
        } else {
            game.playSequence(times, seed, player.verbose);
        }
    }
}
