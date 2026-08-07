package forge.game.ability.effects;

import java.util.List;

import com.google.common.collect.Lists;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Time Machine. The game this resolves in ends before the return happens, so it can't be a normal
 * delayed trigger - it is queued on the Match and rebuilt as a real Command-zone trigger when the
 * next game starts (same mechanism as the Double Dip family, see Match.java).
 */
public class QueueNextGameReturnEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return sa.getHostCard() + " - return in a later game.";
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        if (activator == null || activator.getRegisteredPlayer() == null) {
            return;
        }

        final int turn = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Turn", "1"), sa);

        final List<String> names = Lists.newArrayList();
        for (final Card c : getDefinedCardsOrTargeted(sa)) {
            // by name, because a card object doesn't survive into the next game
            if (c.getPaperCard() != null) {
                names.add(c.getPaperCard().getName());
            }
        }
        if (names.isEmpty()) {
            return;
        }

        host.getGame().getMatch().queueTimeMachineReturn(activator.getRegisteredPlayer(), names, turn);
    }
}
