package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import forge.card.GamePieceType;
import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Clear, Fair Magic. Targets: a permanent is destroyed; a card in the command zone (plane, scheme, vanguard,
 * conspiracy, bounty) is put into its owner's graveyard; anything there that isn't a card (an emblem, a dungeon)
 * just stops existing. Designation$ True: its caster picks one of the designations in the game - the day/night
 * tracker, the monarch, the initiative, a city's blessing - and it's gone (user ruling 2026-10-22: designations are
 * chosen from a list). An AI takes an opponent's designation first.
 */
public class DestroyAnythingEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.hasParam("Designation") ? "Destroy a designation." : "Destroy " + getTargetCards(sa) + ".";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Game game = sa.getHostCard().getGame();
        if (sa.hasParam("Designation")) {
            destroyDesignation(sa, game);
            return;
        }
        for (final Card target : getTargetCards(sa)) {
            final Card c = game.getCardState(target, null);
            if (c == null || !c.equalsWithGameTimestamp(target)) {
                continue;
            }
            if (c.isInPlay()) {
                game.getAction().destroy(c, sa, true, AbilityKey.newMap());
            } else if (c.isInZone(ZoneType.Command)) {
                if (c.getGamePieceType() == GamePieceType.EFFECT || c.getPaperCard() == null || c.isEmblem()) {
                    c.getZone().remove(c);
                    game.getGameLog().add(GameLogEntryType.ZONE_CHANGE, c + " is destroyed.");
                } else {
                    game.getAction().moveToGraveyard(c, sa);
                }
            }
        }
    }

    private static void destroyDesignation(final SpellAbility sa, final Game game) {
        final Player p = sa.getActivatingPlayer();
        final Map<String, Runnable> options = new LinkedHashMap<>();
        final List<String> theirs = new ArrayList<>();
        if (game.getDayTime() != null) {
            options.put("The day/night tracker (it's " + (game.isDay() ? "day" : "night") + ")", () -> game.setDayTime(null));
        }
        final Player monarch = game.getMonarch();
        if (monarch != null) {
            final String label = "The monarch (" + monarch + ")";
            options.put(label, () -> {
                monarch.removeMonarchEffect();
                game.setMonarch(null);
            });
            if (monarch.isOpponentOf(p)) {
                theirs.add(label);
            }
        }
        final Player initiative = game.getHasInitiative();
        if (initiative != null) {
            final String label = "The initiative (" + initiative + ")";
            options.put(label, () -> {
                initiative.removeInitiativeEffect();
                game.setHasInitiative(null);
            });
            if (initiative.isOpponentOf(p)) {
                theirs.add(label);
            }
        }
        for (final Player pl : game.getPlayers()) {
            if (pl.hasBlessing()) {
                final String label = "The city's blessing (" + pl + ")";
                options.put(label, () -> pl.setBlessing(false, null));
                if (pl.isOpponentOf(p)) {
                    theirs.add(label);
                }
            }
        }
        if (options.isEmpty()) {
            return;
        }
        final String pick;
        if (p.getController().isAI()) {
            pick = !theirs.isEmpty() ? theirs.get(0) : options.keySet().iterator().next();
        } else {
            pick = p.getController().chooseSomeType("designation to destroy", sa, new ArrayList<>(options.keySet()));
        }
        if (pick != null && options.containsKey(pick)) {
            options.get(pick).run();
            game.getGameLog().add(GameLogEntryType.INFORMATION, p + " destroys " + pick + ".");
        }
    }
}
