package forge.game.judgetower.table;

import java.util.List;
import java.util.Map;

import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

/** A human at the table: everything the judge asks them, whether the app is on screen or a test is playing. */
public interface TableSeat {
    enum Window {
        /** The step's turn-based actions and anything triggered. */
        STEP,
        /** Carrying out the top of the stack. */
        RESOLVE,
        /** Their own state-based actions and triggers, after someone else's action. */
        SETTLE,
        ATTACK,
        BLOCK,
        DISCARD
    }

    /** Lets them work on the table until they say they're done. */
    void window(Window kind, String prompt);

    /** They have priority. What they play has its declarations and costs done on the table by then; null is a pass. */
    SpellAbility priority(String prompt);

    /** The judge is ruling against them. True overrules it. */
    boolean appeal(String reason);

    /** Something the engine can't read off the table, because someone else is carrying it out: indices of what's chosen. */
    List<Integer> choose(String prompt, List<String> options, int min, int max);

    /** How a creature's combat damage is split, on the table's cards; a null key is the defending player or planeswalker. */
    Map<Card, Integer> assignDamage(Card attacker, List<Card> blockers, int damage, boolean toDefender);

    /** They've left "until something changes": it ended. */
    void stopAutoOk();
}
