package forge.game.ability.effects;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityCantDraw;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.Localizer;

public class DrawEffect extends SpellAbilityEffect {

    @Override
    public boolean movesCardToOrFromLibrary(final SpellAbility sa) {
        return true;
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        if (sa.hasParam("IfDesc")) {
            if (sa.getParam("IfDesc").equals("True") && sa.hasParam("SpellDescription")) {
                String ifDesc = sa.getParam("SpellDescription");
                sb.append(ifDesc, 0, ifDesc.indexOf(",") + 1);
            } else {
                sb.append(sa.getParam("IfDesc"));
            }
            sb.append(" ");
        }

        final List<Player> tgtPlayers = getDefinedPlayersOrTargeted(sa);

        if (!tgtPlayers.isEmpty()) {
            int numCards = sa.hasParam("NumCards") ? AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumCards"), sa) : 1;

            sb.append(Lang.joinHomogenous(tgtPlayers));

            if (tgtPlayers.size() > 1) {
                sb.append(" each");
            }
            sb.append(Lang.joinVerb(tgtPlayers, " draw")).append(" ");
            //if NumCards calculation could change between getStackDescription and resolve, use NumCardsDesc to avoid
            //a "wrong" stack description
            sb.append(sa.hasParam("NumCardsDesc") ? sa.getParam("NumCardsDesc") : numCards == 1 ? "a card" :
                    (Lang.getNumeral(numCards) + " cards"));
            sb.append(".");
        }

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();
        final int numCards = sa.hasParam("NumCards") ? AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumCards"), sa) : 1;
        final boolean upto = sa.hasParam("Upto");
        final boolean optional = sa.hasParam("OptionalDecider") || upto;
        Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
        moveParams.put(AbilityKey.LastStateBattlefield, sa.getLastStateBattlefield());
        moveParams.put(AbilityKey.LastStateGraveyard, sa.getLastStateGraveyard());

        final List<Player> tgts = getTargetPlayersWithDuplicates(true, "Defined", sa);

        // Double Take-style delayed draw: the game this resolves in ends before the draw applies,
        // so it can't be a normal card trigger - queue it on the Match instead (see LifeGainEffect
        // and Match.java for the identical Double Dip mechanism).
        if (sa.hasParam("NextGameFirstUpkeep")) {
            for (final Player p : Sets.newLinkedHashSet(tgts)) {
                p.getGame().getMatch().queueFirstUpkeepDraw(p.getRegisteredPlayer(), numCards * Collections.frequency(tgts, p));
            }
            return;
        }

        final Game game = source.getGame();
        final Collection<Player> targeted = Sets.newLinkedHashSet(tgts);
        // Dandan: players drawing at once from the shared library are dealt the cards one at a time, from the active
        // player in turn order; choices of how many come first, in the same order
        final boolean deal = game.isSharedZone(ZoneType.Library) && targeted.size() > 1
                && !sa.hasParam("FromLibraryOf") && !sa.hasParam("FromBottom");
        final Map<Player, Integer> toDeal = Maps.newLinkedHashMap();
        final Collection<Player> drawers = deal
                ? game.getPlayersInTurnOrder(game.getPhaseHandler().getPlayerTurn()).filter(targeted::contains) : targeted;

        for (final Player p : drawers) {
            if (!p.isInGame()) {
                continue;
            }

            int actualNum = numCards * Collections.frequency(tgts, p);

            // it is optional, not upto and player can't choose to draw that many cards
            if (optional && !upto && !p.canDraw(actualNum)) {
                continue;
            }

            if (optional && !p.getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoYouWantDrawCards", Lang.nounWithAmount(actualNum, " card")), null)) {
                continue;
            }

            if (upto) { // if it is upto, player can only choose how many cards they can draw
                actualNum = StaticAbilityCantDraw.canDrawAmount(p, actualNum);
            }
            if (actualNum <= 0) {
                continue;
            }
            if (upto) {
                actualNum = p.getController().chooseNumber(sa, Localizer.getInstance().getMessage("lblHowManyCardDoYouWantDraw"), 0, actualNum);
            }
            if (deal) {
                toDeal.put(p, actualNum);
                continue;
            }

            // FromLibraryOf$: draw from that player's library instead (Spy Eye)
            // Centrifuge: ToTheirRight - each drawer from the library of the player to their right
            final Player libraryOf = !sa.hasParam("FromLibraryOf") ? p
                    : "ToTheirRight".equals(sa.getParam("FromLibraryOf"))
                    ? p.getGame().getNextPlayerAfter(p, forge.game.Direction.Right)
                    : AbilityUtils.getDefinedPlayers(source, sa.getParam("FromLibraryOf"), sa).getFirst();
            final boolean fromBottom = sa.hasParam("FromBottom");
            if (fromBottom) {
                p.setDrawingFromBottom(true);
            }
            final CardCollectionView drawn;
            try {
                drawn = p.drawCards(actualNum, sa, moveParams, p.getZone(ZoneType.Hand), libraryOf);
            } finally {
                if (fromBottom) {
                    p.setDrawingFromBottom(false);
                }
            }
            afterDraw(sa, source, p, drawn);
        }

        if (!toDeal.isEmpty()) {
            for (final Map.Entry<Player, CardCollection> e : game.getAction().dealCards(toDeal, sa, moveParams).entrySet()) {
                afterDraw(sa, source, e.getKey(), e.getValue());
            }
        }
    }

    private static void afterDraw(final SpellAbility sa, final Card source, final Player p, final CardCollectionView drawn) {
        if (sa.hasParam("Reveal")) {
            p.getGame().getAction().reveal(drawn, p, !sa.getParam("Reveal").equals("All"));
        }
        if (sa.hasParam("RememberDrawn")) {
            source.addRemembered(drawn);
        }
    }
}
