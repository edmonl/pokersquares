package mengyaxi.pokersquares.util;

/**
 *
 * @author Meng
 */
public class Pokers {

    public static final int NUM_RANKS = 13;
    public static final int HAND_SIZE = 5;

    public static int rankDistance(final int rank0, final int rank1) {
        if (rank0 == 0) {
            return rank1 > NUM_RANKS - HAND_SIZE ? NUM_RANKS - rank1 : rank1;
        }
        if (rank1 == 0) {
            return rank0 > NUM_RANKS - HAND_SIZE ? NUM_RANKS - rank0 : rank0;
        }
        return Math.abs(rank1 - rank0);
    }
}
