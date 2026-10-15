package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

/** Gold Mine: mark one of the card's unmarked nodes - on the physical card, so it's still marked next game. */
public class MarkNodeEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        if (host.getMarkedNodes() >= host.getNodeCount()) {
            return;
        }
        host.setMarkedNodes(host.getMarkedNodes() + 1);
        host.getGame().getMatch().writeMarkedNodes(host, host.getMarkedNodes());
    }
}
