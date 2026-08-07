package forge.game.staticability;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import forge.StaticData;
import forge.card.CardEdition;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * R&D's Secret Lair - "Play cards as written. Ignore all errata."
 * <p>
 * Only one piece of errata changed how a large number of cards actually play: "target creature or
 * player" was retemplated to "any target" in Dominaria, which quietly let those spells hit
 * planeswalkers (and later battles). While the Lair is out, a card printed before that retemplating
 * goes back to what it says - creatures and players only.
 * <p>
 * Everything else errata changed is card-specific and handled on the cards themselves.
 */
public class StaticAbilityIgnoreErrata {

    /** Dominaria, where "any target" was introduced. Anything older says "target creature or player". */
    private static final Date ANY_TARGET_TEMPLATING = dominariaRelease();

    private static Date dominariaRelease() {
        try {
            return new java.text.SimpleDateFormat("yyyy-MM-dd").parse("2018-04-27");
        } catch (final java.text.ParseException e) {
            return new Date(Long.MAX_VALUE); // never treat anything as pre-errata rather than guess
        }
    }

    private static final Map<String, Boolean> PREDATES_CACHE = new ConcurrentHashMap<>();

    public static boolean isActive(final Game game) {
        if (game == null) {
            return false;
        }
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (stAb.checkConditions(StaticAbilityMode.IgnoreErrata)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Why this printing can't target this card while the Lair is out, or null if it can. The
     * targeting rule and the message the player is shown both read this, so they can't drift apart -
     * the card script's own "player or planeswalker" wording is exactly what stops being true here.
     */
    public static String targetBlockedReason(final SpellAbility sa, final Card target) {
        if (sa == null || target == null || !sa.usesTargeting() || sa.getTargetRestrictions() == null) {
            return null;
        }
        final Card source = sa.getHostCard();
        if (source == null || source.getGame() == null || !isActive(source.getGame())) {
            return null;
        }
        final String[] valids = sa.getTargetRestrictions().getValidTgts();
        if (valids == null || valids.length == 0) {
            return null;
        }
        if ("Any".equals(valids[0]) && (target.isPlaneswalker() || target.isBattle())
                && predatesAnyTarget(source)) {
            return "as printed this reads \"target creature or player\", so it can't target "
                    + (target.isBattle() ? "battles" : "planeswalkers") + ".";
        }
        if (target.isPlaneswalker() && namesTargetType(valids, "Planeswalker")
                && predatesPlaneswalkers(source)) {
            return "this printing is older than planeswalkers, so as printed it can't target one.";
        }
        if (target.isBattle() && namesTargetType(valids, "Battle") && predatesBattles(source)) {
            return "this printing is older than battles, so as printed it can't target one.";
        }
        return null;
    }

    /** Whether a target restriction spells out this card type, as opposed to merely admitting it. */
    private static boolean namesTargetType(final String[] valids, final String type) {
        for (final String valid : valids) {
            if (valid.equals(type) || valid.startsWith(type + ".") || valid.startsWith(type + "+")) {
                return true;
            }
        }
        return false;
    }

    /**
     * A card printed before planeswalkers (or battles) existed cannot have named them as a target, so
     * wherever the current wording does, that naming is errata. Distinct from the "any target"
     * retemplating: a card printed between Lorwyn and Dominaria could legitimately say "target player
     * or planeswalker" while still not being able to hit a battle.
     */
    public static boolean predatesPlaneswalkers(final Card source) {
        return printedBefore(source, "20071012"); // Lorwyn
    }
    public static boolean predatesBattles(final Card source) {
        return printedBefore(source, "20230421"); // March of the Machine
    }

    /** Whether this printing is older than the given yyyyMMdd date. */
    public static boolean printedBefore(final Card source, final String yyyymmdd) {
        if (source == null) {
            return false;
        }
        final StaticData data = StaticData.instance();
        if (data == null || data.getEditions() == null) {
            return false;
        }
        final CardEdition ed = data.getEditions().get(source.getSetCode());
        if (ed == null || ed.getDate() == null) {
            return false;
        }
        try {
            return ed.getDate().before(new java.text.SimpleDateFormat("yyyyMMdd").parse(yyyymmdd));
        } catch (final java.text.ParseException e) {
            return false;
        }
    }

    /**
     * Whether THIS PRINTING predates the retemplating. It is printing specific on purpose: a
     * Lightning Bolt from an old set reads "target creature or player" and can't hit a planeswalker,
     * while a copy from a 2018-or-later set was printed with "any target" and can.
     */
    public static boolean predatesAnyTarget(final Card source) {
        if (source == null) {
            return false;
        }
        final String set = source.getSetCode();
        if (set == null || set.isEmpty()) {
            return false;
        }
        return PREDATES_CACHE.computeIfAbsent(set, code -> {
            final StaticData data = StaticData.instance();
            if (data == null || data.getEditions() == null) {
                return false;
            }
            final CardEdition ed = data.getEditions().get(code);
            return ed != null && ed.getDate() != null && ed.getDate().before(ANY_TARGET_TEMPLATING);
        });
    }
}
