package forge.game.ability.effects;

import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/** Motivate (Lazier Goblin): the creature can attack and block while the player who paid controls it. */
public class MotivateEffect extends SpellAbilityEffect {
    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player payer = sa.getActivatingPlayer();
        host.addMotivatedBy(payer);
        host.updateAbilityTextForView();
        host.getGame().getGameLog().add(GameLogEntryType.STACK_RESOLVE, payer + " motivates " + host + ".");
    }

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getActivatingPlayer() + " motivates " + sa.getHostCard() + ".";
    }
}
