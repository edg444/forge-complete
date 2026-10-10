package forge.game.staticability;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Iterables;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

public class StaticAbilityMustTarget {

    /**
     * One "must choose at least one [type]" requirement. With {@code IncludePlayers$ True} a player of that type meets
     * it too (Enroll in the Coalition). Requirements for the same type in the same zone are one requirement, and a
     * player meets it if any of them includes players - the Enroll ruling: with Coalition Honor Guards out, an
     * opponent's Shock "could target either creature or you".
     */
    private static final class Restriction {
        private final String type;
        private final ZoneType zone;
        private boolean players;

        private Restriction(String type, ZoneType zone, boolean players) {
            this.type = type;
            this.zone = zone;
            this.players = players;
        }

        private boolean matches(final Card card) {
            return card.getType().hasStringType(type) && card.isInZone(zone);
        }

        private boolean matches(final Player player) {
            return players && StaticAbilityPlayerType.hasType(player, type);
        }
    }

    public static boolean filterMustTargetCards(Player targetingPlayer, List<Card> targets, final SpellAbility spellAbility) {
        return filterMustTargets(targetingPlayer, targets, candidatePlayers(spellAbility), spellAbility);
    }

    /** Filters the card and player candidates down to what meets the requirements; true if anything was filtered. */
    public static boolean filterMustTargets(Player targetingPlayer, List<Card> cards, List<Player> players, final SpellAbility spellAbility) {
        //Only applied when the targeting player and controller are the same
        if (targetingPlayer != spellAbility.getHostCard().getController()) {
            return false;
        }

        List<Restriction> restrictions = getAllRestrictions(spellAbility);
        return applyMustTargetAbility(restrictions, cards, players, spellAbility);
    }

    public static boolean meetsMustTargetRestriction(final SpellAbility spellAbility) {
        // Copied spell is not affected.
        // (ChangeTarget does not go this path so not checked here.)
        if (spellAbility.isCopied()) return true;

        final Game game = spellAbility.getHostCard().getGame();
        List<Restriction> restrictions = getAllRestrictions(spellAbility);

        if (restrictions.isEmpty()) return true;

        SpellAbility currentAbility = spellAbility;
        boolean usesTargeting = false;
        do {
            if (currentAbility.usesTargeting() && !currentAbility.hasParam("TargetingPlayer")) {
                usesTargeting = true;
                // Check if currentAbility can target any MustTarget cards
                TargetRestrictions tgt = currentAbility.getTargetRestrictions();
                List<ZoneType> zone = tgt.getZone();
                List<Card> validCards = CardLists.getValidCards(game.getCardsIn(zone), tgt.getValidTgts(), currentAbility.getActivatingPlayer(), currentAbility.getHostCard(), currentAbility);
                List<Card> choices = CardLists.getTargetableCards(validCards, currentAbility);

                isRestrictionsMet(restrictions, choices, candidatePlayers(currentAbility), currentAbility);
            }
            currentAbility = currentAbility.getSubAbility();
        } while (currentAbility != null);

        return !usesTargeting || restrictions.isEmpty();
    }

    private static List<Player> candidatePlayers(final SpellAbility spellAbility) {
        final List<Player> players = new ArrayList<>();
        final TargetRestrictions tgt = spellAbility.getTargetRestrictions();
        if (tgt == null) {
            return players;
        }
        for (final GameEntity e : tgt.getAllCandidates(spellAbility, true)) {
            if (e instanceof Player p) {
                players.add(p);
            }
        }
        return players;
    }

    private static List<Restriction> getAllRestrictions(final SpellAbility spellAbility) {
        final Game game = spellAbility.getHostCard().getGame();
        List<Restriction> restrictions = new ArrayList<>();

        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.MustTarget) || !stAb.matchesValidParam("ValidSA", spellAbility)) {
                    continue;
                }
                final String type = stAb.getParam("ValidTarget");
                final ZoneType zone = ZoneType.smartValueOf(stAb.getParam("ValidZone"));
                final boolean players = "True".equals(stAb.getParam("IncludePlayers"));
                Restriction same = null;
                for (Restriction r : restrictions) {
                    if (r.type.equals(type) && r.zone == zone) {
                        same = r;
                        break;
                    }
                }
                if (same == null) {
                    restrictions.add(new Restriction(type, zone, players));
                } else {
                    same.players |= players;
                }
            }
        }

        return restrictions;
    }

    private static boolean isRestrictionsMet(List<Restriction> restrictions, List<Card> cards, List<Player> players, final SpellAbility spellAbility) {
        for (int i = restrictions.size() - 1; i >= 0; i--) {
            Restriction restriction = restrictions.get(i);
            // First, check satisfied restrictions that is already targeted by spellAbility
            boolean found = spellAbility.getTargets().getTargetCards().stream().anyMatch(restriction::matches)
                    || Iterables.any(spellAbility.getTargets().getTargetPlayers(), restriction::matches);
            if (found) {
                restrictions.remove(i);
                continue;
            }

            // Second check if their are any targetable card with type in zone, or player of the type
            found = cards.stream().anyMatch(restriction::matches) || players.stream().anyMatch(restriction::matches);
            if (!found) {
                restrictions.remove(i);
            }
        }

        return restrictions.isEmpty();
    }

    private static boolean applyMustTargetAbility(List<Restriction> restrictions, List<Card> cards, List<Player> players, final SpellAbility spellAbility) {
        if (isRestrictionsMet(restrictions, cards, players, spellAbility)) {
            return false;
        }

        // If remaining restrictions are larger than possible target numbers, then all targets are cleared (means not possible to target any one)
        final int maxTargets = spellAbility.getMaxTargets();
        final int targeted = spellAbility.getTargets().size();
        if (restrictions.size() > maxTargets - targeted) {
            cards.clear();
            players.clear();
            return true;
        }

        // Filter out everything not satisfying any of the restrictions
        boolean filtered = cards.removeIf(card -> restrictions.stream().noneMatch(r -> r.matches(card)));
        filtered |= players.removeIf(player -> restrictions.stream().noneMatch(r -> r.matches(player)));
        return filtered;
    }

}
