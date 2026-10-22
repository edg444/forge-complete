package forge.game.ability.effects;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.MyRandom;

/**
 * The Octopus Official's top 8. Mode$ Announce reveals until eight nonland cards, which are exiled in four random
 * two-card piles (the player's tournament, unless one is already running), with the lands on the bottom in a random
 * order. Mode$ Advance puts a random card of each pile into its owner's hand and pairs off the rest into the next
 * round; when one card is left, the tournament is over and that champion is remembered on the host.
 */
public class TournamentEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Announce".equals(sa.getParamOrDefault("Mode", "Announce")) ? "Announce the top 8." : "Play the next round.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player p = sa.getActivatingPlayer();
        final Game game = p.getGame();
        if ("Announce".equals(sa.getParamOrDefault("Mode", "Announce"))) {
            if (p.getTournament() != null) {
                return;
            }
            final CardCollection revealed = new CardCollection();
            final List<Card> entrants = Lists.newArrayList();
            final List<Card> lands = Lists.newArrayList();
            for (final Card c : p.getCardsIn(ZoneType.Library)) {
                revealed.add(c);
                if (c.isLand()) {
                    lands.add(c);
                } else {
                    entrants.add(c);
                    if (entrants.size() == 8) {
                        break;
                    }
                }
            }
            if (revealed.isEmpty()) {
                return;
            }
            game.getAction().reveal(revealed, p, false);
            Collections.shuffle(entrants, MyRandom.getRandom());
            final List<List<Card>> piles = Lists.newArrayList();
            for (int i = 0; i < 4; i++) {
                piles.add(Lists.newArrayList());
            }
            for (int i = 0; i < entrants.size(); i++) {
                final Card moved = game.getAction().exile(entrants.get(i), sa, AbilityKey.newMap());
                if (moved != null && moved.isInZone(ZoneType.Exile)) {
                    moved.setExiledWith(host);
                    moved.setExiledBy(p);
                    piles.get(i / 2).add(moved);
                }
            }
            Collections.shuffle(lands, MyRandom.getRandom());
            for (final Card land : lands) {
                game.getAction().moveToBottomOfLibrary(land, sa);
            }
            piles.removeIf(List::isEmpty);
            if (!piles.isEmpty()) {
                p.setTournament(piles);
                game.getGameLog().add(GameLogEntryType.INFORMATION, p + "'s top 8: " + piles + ".");
            }
            return;
        }

        final List<List<Card>> piles = p.getTournament();
        if (piles == null) {
            return;
        }
        final List<Card> advancing = Lists.newArrayList();
        for (final List<Card> pile : piles) {
            final List<Card> live = Lists.newArrayList();
            for (final Card c : pile) {
                final Card now = game.getCardState(c);
                if (now != null && now.isInZone(ZoneType.Exile)) {
                    live.add(now);
                }
            }
            if (live.isEmpty()) {
                continue;
            }
            final Card out = Aggregates.random(live);
            game.getAction().moveToHand(out, sa);
            live.remove(out);
            advancing.addAll(live);
        }
        if (advancing.size() <= 1) {
            p.setTournament(null);
            if (advancing.size() == 1) {
                game.getGameLog().add(GameLogEntryType.INFORMATION, advancing.get(0) + " wins " + p + "'s tournament!");
                if (sa.hasParam("RememberChampion")) {
                    host.addRemembered(advancing.get(0));
                }
            }
            return;
        }
        final List<List<Card>> next = Lists.newArrayList();
        for (int i = 0; i < advancing.size(); i += 2) {
            next.add(Lists.newArrayList(advancing.subList(i, Math.min(i + 2, advancing.size()))));
        }
        p.setTournament(next);
    }
}
