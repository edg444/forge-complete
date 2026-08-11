package forge.game.ability.effects;

import java.util.Map;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Decorated Knight - "draw a card from your original deck if it's outside the game".
 * <p>
 * A draw, not a search: it has to come off the top. Present Arms moves the displaced library into
 * the sideboard in library order, and a Zone keeps its list in insertion order, so the first card
 * still flagged as displaced IS the top of the original deck. Cards that were always in the
 * sideboard are skipped - they were never part of that deck.
 */
public class DrawFromOutsideGameEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return sa.getHostCard() + " - draw from the original deck outside the game.";
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final int amount = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("NumCards", "1"), sa);

        for (final Player p : getDefinedPlayersOrTargeted(sa)) {
            for (int i = 0; i < amount; i++) {
                Card top = null;
                for (final Card c : p.getCardsIn(ZoneType.Sideboard)) {
                    if (c.isDisplacedFromLibrary()) {
                        top = c;
                        break;
                    }
                }
                if (top == null) {
                    break;
                }
                final Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
                game.getAction().moveTo(ZoneType.Hand, top, sa, moveParams);
            }
        }
    }
}
