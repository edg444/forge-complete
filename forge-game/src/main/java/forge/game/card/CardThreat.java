package forge.game.card;

import java.util.function.ToIntFunction;

/**
 * How big a threat or how impactful a card is, for effects that model a person's judgement rather than a
 * player's choice (Sacrifice Play's and Defective Detective's person outside the game). The real measure is
 * the AI's card evaluation, which lives in forge-ai and so can't be called from here; the GUI layer plugs it
 * in at startup, and power plus toughness (mana value for noncreatures) stands in until then.
 */
public final class CardThreat {
    private CardThreat() { }

    private static ToIntFunction<Card> evaluator = c -> c.isCreature() ? c.getNetPower() + c.getNetToughness() : c.getCMC();

    public static void setEvaluator(final ToIntFunction<Card> e) {
        evaluator = e;
    }

    public static int evaluate(final Card c) {
        return evaluator.applyAsInt(c);
    }
}
