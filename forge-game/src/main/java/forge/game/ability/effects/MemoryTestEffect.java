package forge.game.ability.effects;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

/**
 * Memory Test. Mode$ Exile: the target opponent exiles cards from the bottom of their library until five nonland cards
 * are exiled, then turns those face down - everyone sees them first (rulings). Mode$ Name: name the face-down cards;
 * each one the cards hold must be named, twice for two of the same card (rulings). The player picks each name from
 * Forge's card-name search; the AI remembers each card 80% of the time (user, 2026-10-21). Named correctly, they turn
 * face up and the host's NamedAll SVar is 1. Every exiled card stays in exile.
 */
public class MemoryTestEffect extends SpellAbilityEffect {

    private static final int NONLANDS = 5;

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Exile".equals(sa.getParam("Mode")) ? "Target opponent exiles cards from the bottom of their library."
                : sa.getActivatingPlayer() + " names the face-down exiled cards.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        if ("Exile".equals(sa.getParam("Mode"))) {
            exile(sa);
        } else {
            name(sa);
        }
    }

    private void exile(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        for (final Player p : getTargetPlayers(sa)) {
            final CardCollection nonlands = new CardCollection();
            while (nonlands.size() < NONLANDS && !p.getCardsIn(ZoneType.Library).isEmpty()) {
                final Card bottom = p.getCardsIn(ZoneType.Library).getLast();
                final Map<AbilityKey, Object> params = AbilityKey.newMap();
                final Card exiled = game.getAction().exile(bottom, sa, params);
                if (exiled == null || !exiled.isInZone(ZoneType.Exile)) {
                    break;
                }
                exiled.setExiledWith(host);
                exiled.setExiledBy(p);
                host.addExiledCard(exiled);
                if (!exiled.isLand()) {
                    nonlands.add(exiled);
                }
            }
            for (final Card c : nonlands) {
                c.turnFaceDown(true);
            }
        }
    }

    private void name(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player you = sa.getActivatingPlayer();
        final Game game = you.getGame();
        host.setSVar("NamedAll", "Number$0");
        final CardCollection faceDown = CardLists.filter(you.getGame().getCardsIn(ZoneType.Exile),
                c -> c.isFaceDown() && host.hasExiledCard(c));
        if (faceDown.isEmpty()) {
            return;
        }
        final Map<String, Integer> remaining = Maps.newHashMap();
        for (final Card c : faceDown) {
            remaining.merge(c.getState(forge.card.CardStateName.Original).getName(), 1, Integer::sum);
        }
        boolean all = true;
        if (you.getController().isAI()) {
            for (int i = 0; i < faceDown.size(); i++) {
                all &= MyRandom.percentTrue(80);
            }
        } else {
            final List<String> named = Lists.newArrayList();
            for (int i = 1; i <= faceDown.size(); i++) {
                final String name = you.getController().chooseCardName(sa, f -> true, "Card",
                        "Name face-down exiled card " + i + " of " + faceDown.size() + " (no peeking)");
                named.add(name);
            }
            for (final String name : named) {
                final Integer left = name == null ? null : remaining.get(name);
                if (left == null || left == 0) {
                    all = false;
                } else {
                    remaining.put(name, left - 1);
                }
            }
        }
        final List<String> names = Lists.newArrayList();
        for (final Card c : faceDown) {
            names.add(c.getState(forge.card.CardStateName.Original).getName());
        }
        you.getController().notifyOfValue(sa, host, "The face-down cards are: " + String.join(", ", names));
        game.getGameLog().add(GameLogEntryType.INFORMATION, you + (all ? " named every face-down card." : " missed at least one."));
        if (all) {
            for (final Card c : faceDown) {
                c.turnFaceUp(null);
            }
            host.setSVar("NamedAll", "Number$1");
        }
    }
}
