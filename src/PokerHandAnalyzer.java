
import java.util.ArrayList;
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
        final PokerSquaresPointSystem pointSystem, final DeckTracker deck) {
        final List<Card> cards = rc.getCards();
        final double score0 = score(cards, pointSystem, deck);
        cards.add(card);
        return score(cards, pointSystem, deck) - score0;
    }

    private static double score(final List<Card> cards,
        final PokerSquaresPointSystem pointSystem, final DeckTracker deck) {
        if (cards.isEmpty()) {
            return 3.12;
        }
        if (cards.size() == 1) {
            return 3.11;
        }
        final PokerHand hand = PokerHand.getPokerHand(cards.toArray(new Card[POKER_HAND_SIZE]));
        final int handScore = pointSystem.getHandScore(hand);
        if (cards.size() >= POKER_HAND_SIZE) {
            return handScore;
        }
        final int vacancies = POKER_HAND_SIZE - cards.size();
        switch (hand) {
            case FOUR_OF_A_KIND:
                return handScore + 1;
            case THREE_OF_A_KIND: {
                if (cards.size() == 3) {
                    final int rank = cards.get(0).getRank();
                    return handScore + 2
                        + 2 * (pointSystem.getHandScore(PokerHand.FOUR_OF_A_KIND) - handScore) * deck.countRank(rank) / (double) deck.getNumberOfCards();
                }
                final int rank = cards.get(0).getRank() == cards.get(1).getRank() || cards.get(0).getRank() == cards.get(2).getRank()
                    ? cards.get(0).getRank() : cards.get(1).getRank();
                final int otherRank = cards.stream().filter(c -> c.getRank() != rank).findAny().get().getRank();
                return handScore + 1
                    + (pointSystem.getHandScore(PokerHand.FOUR_OF_A_KIND) - handScore) * deck.countRank(rank) / (double) deck.getNumberOfCards()
                    + (pointSystem.getHandScore(PokerHand.FULL_HOUSE) - handScore) * deck.countRank(otherRank) / (double) deck.getNumberOfCards();
            }
            case TWO_PAIR: {
                final int rank = cards.get(0).getRank();
                final int otherRank = cards.stream().filter(c -> c.getRank() != rank).findAny().get().getRank();
                return handScore + 1
                    + (pointSystem.getHandScore(PokerHand.FULL_HOUSE) - handScore) * deck.countRank(rank) / (double) deck.getNumberOfCards()
                    + (pointSystem.getHandScore(PokerHand.FULL_HOUSE) - handScore) * deck.countRank(otherRank) / (double) deck.getNumberOfCards();
            }
            case ONE_PAIR:
                return handScore + vacancies * 0.9;
        }
        double score = vacancies * 0.5;
        final int suit0 = cards.get(0).getSuit();
        final boolean flush = cards.stream().skip(0).allMatch(c -> c.getSuit() == suit0);
        if (flush) {
            score += vacancies * pointSystem.getHandScore(PokerHand.FLUSH)
                * Math.pow((double) deck.countSuit(suit0) / deck.getNumberOfCards(), vacancies);
        }
        final boolean[] ranks = collectRanks(cards);
        final int[] straightRange = hasStraightPotential(ranks, cards.size());
        if (straightRange != null) {
            final List<Integer> neededRanks = new ArrayList<>(POKER_HAND_SIZE);
            double sp = 0.0; // rough estimate of the likelihood of straight
            if (straightRange[1] == 0) {
                for (int rank = Card.NUM_RANKS - POKER_HAND_SIZE + 1; rank < Card.NUM_RANKS; ++rank) {
                    if (!ranks[rank]) {
                        neededRanks.add(rank);
                    }
                }
            } else {
                int startRank = straightRange[1] + 1 - POKER_HAND_SIZE;
                if (startRank < 0) {
                    startRank = 0;
                }
                int endRank = straightRange[0] + POKER_HAND_SIZE;
                if (endRank > ranks.length) {
                    endRank = ranks.length;
                }
                for (; startRank < endRank; ++startRank) {
                    if (!ranks[startRank]) {
                        neededRanks.add(startRank);
                    }
                }
            }
            final double[] rankScores = new double[neededRanks.size()];
            for (int i = 0; i < rankScores.length; ++i) {
                rankScores[i] = (double) deck.countRank(neededRanks.get(i)) / deck.getNumberOfCards();
            }
            for (int i = rankScores.length - vacancies; i >= 0; --i) {
                double p = 0.0;
                for (int j = 0; j < vacancies; ++j) {
                    p *= rankScores[i + j];
                }
                sp += vacancies * p;
            }
            score += pointSystem.getHandScore(PokerHand.STRAIGHT) * sp;
            if (flush) {
                score += pointSystem.getHandScore(PokerHand.STRAIGHT_FLUSH) * sp;
                if (straightRange[1] == 0) {
                    score += pointSystem.getHandScore(PokerHand.ROYAL_FLUSH) * sp;
                }
            }
        }
        return score;
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
