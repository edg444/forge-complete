package forge.game.ability.effects;

import java.util.List;

import com.google.common.collect.Lists;

import forge.card.MagicColor;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.mana.Mana;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Trampled Lotus: tap up to Amount$ untapped lands (ValidCards$) for mana, and the activator adds it. Each land gives
 * one mana of a color it could make (the activator picks when there's more than one), or {C}.
 */
public class TapForManaEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getActivatingPlayer() + " taps up to " + sa.getParamOrDefault("Amount", "1") + " lands for mana.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player you = sa.getActivatingPlayer();
        final Game game = you.getGame();
        final int max = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Amount", "1"), sa);
        final CardCollection lands = CardLists.filter(CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield),
                sa.getParam("ValidCards"), you, host, sa), c -> c.isUntapped() && !c.getManaAbilities().isEmpty());
        if (lands.isEmpty() || max <= 0) {
            return;
        }
        final CardCollection chosen = new CardCollection(you.getController().chooseCardsForEffect(lands, sa,
                "Choose up to " + max + " lands to tap for mana", 0, Math.min(max, lands.size()), true, null));
        final List<Mana> mana = Lists.newArrayList();
        for (final Card land : chosen) {
            final List<String> colors = Lists.newArrayList();
            for (final SpellAbility ab : land.getManaAbilities()) {
                for (final String color : MagicColor.Constant.ONLY_COLORS) {
                    final String shortColor = MagicColor.toShortString(color);
                    if (!colors.contains(color) && ab.getManaPart() != null && ab.getManaPart().canProduce(shortColor, ab)) {
                        colors.add(color);
                    }
                }
            }
            byte color = MagicColor.COLORLESS;
            if (colors.size() == 1) {
                color = MagicColor.fromName(colors.get(0));
            } else if (colors.size() > 1) {
                color = you.getController().chooseColor("Choose the color of mana " + land + " makes for you", sa,
                        forge.card.ColorSet.fromNames(colors));
            }
            if (!land.tap(true, sa, you)) {
                continue;
            }
            mana.add(new Mana(color, land, null, you));
        }
        you.getManaPool().addMana(mana);
    }
}
