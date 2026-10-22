package forge.game.ability.effects;

import java.util.List;

import com.google.common.collect.Lists;

import forge.StaticData;
import forge.game.Game;
import forge.game.GameFormat;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Aggregates;

/**
 * The Wheeling Runner: Amount$ cards chosen at random from every card legal in LegalIn$ (one printing each, different
 * names), put into the activator's hand. They didn't start in the deck.
 */
public class RandomCardsEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Add " + sa.getParam("Amount") + " random " + sa.getParam("LegalIn") + "-legal cards to your hand.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player you = sa.getActivatingPlayer();
        final Game game = you.getGame();
        final int amount = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Amount", "1"), sa);
        final GameFormat format = GameFormat.named(sa.getParam("LegalIn"));
        final List<PaperCard> pool = Lists.newArrayList(StaticData.instance().getCommonCards().getUniqueCards());
        if (format != null) {
            pool.removeIf(format.getFilterRules().negate());
        }
        for (final PaperCard pc : Aggregates.random(pool, amount)) {
            final Card made = Card.fromPaperCard(pc, you);
            game.getAction().moveTo(you.getZone(ZoneType.Hand), made, sa);
        }
    }
}
