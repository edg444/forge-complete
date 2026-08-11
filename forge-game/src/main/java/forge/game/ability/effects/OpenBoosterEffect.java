package forge.game.ability.effects;

import java.util.List;
import java.util.Map;

import forge.StaticData;
import forge.card.CardEdition;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.item.SealedTemplate;
import forge.item.generation.BoosterGenerator;

/**
 * Stocking Tiger. The printed card has you open a real sealed booster you own from outside the game,
 * so this generates a genuine one from the chosen set's own booster template rather than faking a
 * handful of cards - the same generator draft and sealed use.
 */
public class OpenBoosterEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final String set = sa.getHostCard().getChosenExpansion();
        return sa.getHostCard() + " - open the chosen booster pack"
                + (set.isEmpty() ? "" : " (" + set + ")") + ".";
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final String setCode = host.getChosenExpansion();
        if (setCode.isEmpty()) {
            return;
        }
        final StaticData data = StaticData.instance();
        if (data == null || data.getEditions() == null) {
            return;
        }
        final CardEdition edition = data.getEditions().get(setCode);
        if (edition == null) {
            return;
        }
        final SealedTemplate template = edition.getBoosterTemplate();
        if (template == null) {
            return;
        }

        final List<PaperCard> pack;
        try {
            pack = BoosterGenerator.getBoosterPack(template);
        } catch (final Exception e) {
            // a set can be listed with a booster template it can't actually fill
            return;
        }
        if (pack == null || pack.isEmpty()) {
            return;
        }

        for (final Player p : getDefinedPlayersOrTargeted(sa)) {
            final CardCollection opened = new CardCollection();
            for (final PaperCard pc : pack) {
                final Card card = Card.fromPaperCard(pc, p);
                final Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
                moveParams.put(AbilityKey.LastStateBattlefield, game.copyLastStateBattlefield());
                moveParams.put(AbilityKey.LastStateGraveyard, game.copyLastStateGraveyard());
                opened.add(game.getAction().moveTo(ZoneType.Hand, card, sa, moveParams));
            }
            if (!opened.isEmpty() && sa.hasParam("Reveal")) {
                game.getAction().reveal(opened, p, false,
                        p + " opens a " + setCode + " booster pack: ");
            }
        }
    }
}
