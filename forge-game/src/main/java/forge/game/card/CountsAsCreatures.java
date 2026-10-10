package forge.game.card;

import forge.game.CardTraitBase;
import forge.game.GameObject;
import forge.game.player.Player;
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityMode;

/**
 * Five Kids in a Trenchcoat: {@code S:Mode$ CountsAsCreatures | Amount$ 5} - the card counts as that many creatures
 * when a spell or effect counts how many creatures a player controls (user rulings 2026-10-10: anyone's effect,
 * filtered counts and thresholds included; a count of all creatures still sees one). Its ruling: costs and effects
 * that act on some number of creatures see one creature.
 */
public final class CountsAsCreatures {
    private CountsAsCreatures() {
    }

    /** How many creatures this card counts as in a count of the creatures someone controls. */
    public static int amount(final Card c) {
        for (final StaticAbility st : c.getStaticAbilities()) {
            if (st.checkConditions(StaticAbilityMode.CountsAsCreatures)) {
                return Integer.parseInt(st.getParam("Amount"));
            }
        }
        return 1;
    }

    /** The creatures a player controls, counted. */
    public static int count(final Iterable<Card> creaturesControlled) {
        int n = 0;
        for (final Card c : creaturesControlled) {
            n += amount(c);
        }
        return n;
    }

    /** Counts objects already matched by a comma-separated valid restriction, weighing creature-control counts. */
    public static int count(final Iterable<? extends GameObject> matched, final String restriction,
            final Player sourceController, final Card source, final CardTraitBase ctb) {
        int n = 0;
        String[] alternatives = null;
        for (final GameObject o : matched) {
            if (o instanceof Card c) {
                final int amount = amount(c);
                if (amount != 1) {
                    if (alternatives == null) {
                        alternatives = restriction.split(",");
                    }
                    if (countsCreaturesControlled(c, alternatives, sourceController, source, ctb)) {
                        n += amount;
                        continue;
                    }
                }
            }
            n++;
        }
        return n;
    }

    private static boolean countsCreaturesControlled(final Card c, final String[] alternatives,
            final Player sourceController, final Card source, final CardTraitBase ctb) {
        for (final String alternative : alternatives) {
            if (isCreatureControlCount(alternative) && c.isValid(alternative.trim(), sourceController, source, ctb)) {
                return true;
            }
        }
        return false;
    }

    /** "Creature.YouCtrl", "Creature.Other+ControlledBy TargetedPlayer"... - creatures, restricted to a controller. */
    static boolean isCreatureControlCount(final String alternative) {
        final String[] parts = alternative.trim().split("\\.", 2);
        boolean creature = parts[0].equals("Creature");
        boolean controlled = false;
        if (parts.length > 1) {
            for (final String p : parts[1].split("\\+")) {
                if (p.equals("Creature")) {
                    creature = true;
                }
                if (!p.startsWith("!") && !p.contains("Dont") && (p.endsWith("Ctrl") || p.startsWith("ControlledBy"))) {
                    controlled = true;
                }
            }
        }
        return creature && controlled;
    }
}
