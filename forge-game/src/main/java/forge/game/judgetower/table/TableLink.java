package forge.game.judgetower.table;

import java.util.List;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Between the judge's game, where the rules run, and what's driving the physical table. The engine calls the first group
 * at the moments the table has to be consulted, and the table's prompts call the rest.
 */
public interface TableLink {
    /** The engine has done a step's turn-based actions, which the table hasn't. */
    void afterStepBegan();

    /** State-based actions and triggers are done in the engine, and a player is about to get priority. */
    void beforePriority(Player player);

    /** Everyone has passed with something on the stack. */
    void resolveTop();

    /** An AI seat has cast or activated something. */
    void afterAiAction();

    /** The game as it stands is what an infraction would go back to. */
    void onCheckpoint();

    /** The engine has gone back to the last checkpoint. */
    void onRolledBack();

    /** A ruling's been made, and the offender may be out of the game. */
    void onRuled();

    /** The order a shuffle in the engine has to end in: the table's. Null when it's the engine's own shuffle. */
    List<Card> adoptShuffle(List<Card> cards);

    /** The next coin the table flipped, or null if it didn't flip one. */
    Boolean nextCoin();

    /** The next die the table rolled, or null. */
    Integer nextDie(int sides);

    // ---------------------------------------------------------------- for the table's prompts

    /** What can be tried with a card on the table: spells and abilities in the judge's game, the engine being idle. */
    List<SpellAbility> offeredAbilities(Card tableCard, Player tablePlayer);

    /** The trigger texts of a card on the table, for a player putting one on the stack. */
    List<String> triggerTexts(Card tableCard);

    void noteCoin(boolean won);

    void noteDie(int sides, int result);

    /** Something announced that leaves no trace (a color, a number, a card's name), for the judge to go by. */
    void announce(String what);

    /** Goes back to the last legal state of the table: what's been done since is taken back, at no cost. */
    void resetTable();

    /** Has an AI seat say what it does in what's now resolving. False if nothing's resolving or it had nothing to do. */
    boolean askAi();
}
