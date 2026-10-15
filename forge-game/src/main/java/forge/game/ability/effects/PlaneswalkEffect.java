package forge.game.ability.effects;

import forge.StaticData;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.replacement.ReplacementResult;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Localizer;

import java.util.List;
import java.util.Map;


public class PlaneswalkEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        Player activator = sa.getActivatingPlayer();
        Game game = activator.getGame();

        // Seek Bolas's Counsel planeswalks to a named plane even outside a Planechase game (its rulings)
        final boolean named = sa.hasParam("PlaneName");
        if (game.getActivePlanes() == null && !named) { // not a planechase game, nothing happens
            return;
        }

        if (sa.hasParam("Optional") && !activator.getController().confirmAction(sa, null,
                Localizer.getInstance().getMessage("lblWouldYouLikeToPlaneswalk"), null)) {
                    return;
        }

        final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(activator);
        Object cause = sa.hasParam("Cause") ? sa.getParam("Cause") : sa;
        repParams.put(AbilityKey.Cause, cause);
        if (game.getReplacementHandler().run(ReplacementType.Planeswalk, repParams) == ReplacementResult.Replaced) {
            return;
        }

        if (!sa.hasParam("DontPlaneswalkAway") && game.getActivePlanes() != null) {
            for (Player p : game.getPlayers()) {
                p.leaveCurrentPlane();
            }
        }
        if (named) {
            // "This planeswalk isn't represented by a plane card": it stops existing once everyone leaves it
            final Card plane = Card.fromPaperCard(StaticData.instance().getVariantCards().getCard(sa.getParam("PlaneName")), activator);
            game.getAction().moveTo(ZoneType.None, plane, sa, AbilityKey.newMap());
            activator.planeswalkTo(sa, new CardCollection(plane));
            // the command zone holds a new object, the last plane added
            final List<Card> planes = game.getActivePlanes();
            planes.get(planes.size() - 1).setSVar("NotAPlaneCard", "True");
        } else if (sa.hasParam("Defined")) {
            CardCollectionView destinations = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa);
            activator.planeswalkTo(sa, destinations);
        } else {
            activator.planeswalk(sa);
        }
    }
}
