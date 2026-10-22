package forge.game.ability.effects;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;

/**
 * Welcome to Australia: "Players flip their libraries over" - each library's order is reversed, so the bottom card is
 * now on top. User ruling 2026-10-22: they flip back when it leaves.
 */
public class ReverseLibrariesEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Players flip their libraries over.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        for (final Player p : sa.getActivatingPlayer().getGame().getPlayers()) {
            final PlayerZone library = p.getZone(ZoneType.Library);
            final List<forge.game.card.Card> cards = Lists.newArrayList(library.getCards());
            Collections.reverse(cards);
            library.setCards(cards);
        }
    }
}
