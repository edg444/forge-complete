package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Trash Panda: put Defined$ cards into Destination$ of the targeted player (or DefinedPlayer$), even though they don't
 * own them. The cards stay their owner's.
 */
public class PutInZoneOfEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Put " + getDefinedCardsOrTargeted(sa) + " into " + getTargetPlayers(sa, "DefinedPlayer") + "'s "
                + sa.getParamOrDefault("Destination", "Graveyard").toLowerCase() + ".";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Game game = sa.getHostCard().getGame();
        final ZoneType dest = ZoneType.smartValueOf(sa.getParamOrDefault("Destination", "Graveyard"));
        for (final Player p : getTargetPlayers(sa, "DefinedPlayer")) {
            for (final Card c : getDefinedCardsOrTargeted(sa)) {
                final Card cur = game.getCardState(c);
                if (cur.getZone() != null && !cur.getZone().is(ZoneType.Stack)) {
                    game.getAction().moveTo(p.getZone(dest), cur, sa);
                }
            }
        }
    }
}
