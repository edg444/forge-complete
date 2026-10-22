package forge.game.ability.effects;

import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class SetInMotionEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        Card source = sa.getHostCard();
        Player controller = source.getController();
        boolean again = sa.hasParam("Again");

        int repeats = 1;
        if (sa.hasParam("RepeatNum")) {
            repeats = AbilityUtils.calculateAmount(source, sa.getParam("RepeatNum"), sa);
        }

        // The Archenemy's Charm: a scheme named on the card, from outside the game
        if (sa.hasParam("DefinedName")) {
            final forge.item.PaperCard pc = forge.StaticData.instance().getVariantCards().getCard(sa.getParam("DefinedName"));
            if (pc != null) {
                final Card scheme = Card.fromPaperCard(pc, controller);
                controller.setSchemeInMotion(sa, scheme);
            }
            return;
        }
        for (int i = 0; i < repeats; i++) {
            if (again) {
                controller.setSchemeInMotion(sa, controller.getGame().getCardState((Card) sa.getRootAbility().getTriggeringObject(AbilityKey.Scheme), null));
            } else {
                controller.setSchemeInMotion(sa);
            }
        }
    }
}
