package forge.game.ability.effects;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.MyRandom;

public class ReorderZoneEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        if (sa.hasParam("Seconds")) {
            return Lang.joinHomogenous(getTargetPlayers(sa)) + " has " + sa.getParam("Seconds") + " seconds to rearrange their " + ZoneType.smartValueOf(sa.getParam("Zone")).toString().toLowerCase() + ".";
        }
        if (sa.hasParam("Program")) {
            return Lang.joinHomogenous(getTargetPlayers(sa)) + " order their hands into programs.";
        }
        final ZoneType zone = ZoneType.smartValueOf(sa.getParam("Zone"));
        final List<Player> tgtPlayers = getTargetPlayers(sa);
        boolean shuffle = sa.hasParam("Random");

        return "Reorder " + Lang.joinHomogenous(tgtPlayers) + " " + zone.toString() + " " + (shuffle ? "at random." : "as your choose.");
    }

    @Override
    public void resolve(SpellAbility sa) {
        final ZoneType zone = ZoneType.smartValueOf(sa.getParam("Zone"));
        boolean shuffle = sa.hasParam("Random");

        for (final Player p : getTargetPlayers(sa)) {
            if (!p.isInGame()) {
                continue;
            }

            // The Grand Calcutron: the hand becomes a program, kept in the order chosen here
            if (sa.hasParam("Program")) {
                p.addProgramSource(sa.getHostCard());
            }

            CardCollection list = new CardCollection(p.getCardsIn(zone));
            if (list.size() < 2 && sa.hasParam("Program")) {
                continue;
            }
            if (sa.hasParam("Seconds")) {
                // Hot Fix: rearranged against the clock; still touching a card when time runs out = shuffle
                final Pair<CardCollectionView, Boolean> result = p.getController().rearrangeInTime(list,
                        Integer.parseInt(sa.getParam("Seconds")), sa);
                if (result.getLeft().size() == list.size() && result.getLeft().containsAll(list)) {
                    p.getZone(zone).setCards(result.getLeft());
                }
                if (result.getRight()) {
                    p.getGame().getGameLog().add(GameLogEntryType.STACK_RESOLVE, p + " was still touching their " + zone.toString().toLowerCase() + " when time ran out and shuffles it.");
                    p.shuffle(sa);
                }
            } else if (shuffle) {
                Collections.shuffle(list, MyRandom.getRandom());
                p.getZone(zone).setCards(list);
            } else {
                CardCollectionView orderedCards = p.getController().orderMoveToZoneList(list, zone, sa);
                p.getZone(zone).setCards(orderedCards);
            }
        }
    }
}
