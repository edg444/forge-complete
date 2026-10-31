package forge.game.judgetower.table;

import java.util.ArrayList;
import java.util.List;

import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * What a player has put on the table's stack, and what they declared for it. It's only ever looked at: nothing resolves
 * on the table, a player carries it out by hand.
 */
public final class TableStackItem extends SpellAbility {
    private final boolean spell;
    private final String text;
    /** Declared by the player: the modes by description, the optional costs by description, and X. */
    public final List<String> modes = new ArrayList<>();
    public final List<String> optionalCosts = new ArrayList<>();
    public Integer x;

    public TableStackItem(final Card source, final Player controller, final boolean spell0, final String text0) {
        super(source, Cost.Zero);
        setActivatingPlayer(controller);
        spell = spell0;
        text = text0;
        setStackDescription(text0);
        setDescription(text0);
    }

    public String getText() {
        return text;
    }

    @Override
    public boolean isSpell() {
        return spell;
    }

    @Override
    public boolean isAbility() {
        return !spell;
    }

    @Override
    public boolean canPlay() {
        return false;
    }

    @Override
    public void resolve() {
    }
}
