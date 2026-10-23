package forge.game.ability.effects;

import java.util.List;

import com.google.common.collect.Lists;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.WrappedAbility;

/**
 * The Many Deeds of Belzenlok: copy the Defined Saga card's chapter ability numbered Chapter$, wherever that card is.
 * A Saga without that chapter gives nothing to copy (rulings). The copy's controller may choose its targets.
 */
public class CopyChapterEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getParamOrDefault("SpellDescription", "");
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Player controller = sa.getActivatingPlayer();
        final int chapter = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("Chapter"), sa);
        final List<SpellAbility> copies = Lists.newArrayList();
        for (final Card saga : AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa)) {
            for (final Trigger t : saga.getTriggers()) {
                if (t.hasParam("Chapter") && String.valueOf(chapter).equals(t.getParam("Chapter"))) {
                    final SpellAbility copy = t.ensureAbility().copy(saga, controller, false);
                    copy.setActivatingPlayer(controller);
                    final WrappedAbility wrapped = new WrappedAbility(t, copy, controller);
                    wrapped.setCopied(true);
                    copies.add(wrapped);
                    break;
                }
            }
        }
        if (!copies.isEmpty()) {
            controller.getController().orderAndPlaySimultaneousSa(copies);
        }
    }
}
