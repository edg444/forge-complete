package forge.game.card;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import forge.card.mana.ManaCost;
import forge.game.CardTraitBase;
import forge.game.keyword.KeywordInterface;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;

/**
 * The numbers printed on an object that More or Less can change: power, toughness, the numeral in its mana cost,
 * numbers in its keywords, and numbers in its abilities' text. An ability's number counts only if its text
 * really prints it - "draw a card" draws 1 but prints "a", which isn't a number word (Unstable ruling) - and X is
 * never a literal, so it's never offered.
 */
public final class NumberInstances {
    private NumberInstances() { }

    private static final String[] ORDINALS = {"", "first ", "second ", "third ", "fourth ", "fifth "};

    public static List<NumberNudge> of(final Card c) {
        final List<NumberNudge> result = Lists.newArrayList();
        // an instant has a base power of 0 too, but nothing printed
        final boolean hasPT = c.getCurrentState().hasPrintedPT();
        if (hasPT && isPrintedNumber(c.getBasePowerString())) {
            final int p = Integer.parseInt(c.getBasePowerString());
            result.add(new NumberNudge(NumberNudge.Kind.POWER, "Power " + p, null, null, null, 0, null, p, p, 0));
        }
        if (hasPT && isPrintedNumber(c.getBaseToughnessString())) {
            final int t = Integer.parseInt(c.getBaseToughnessString());
            result.add(new NumberNudge(NumberNudge.Kind.TOUGHNESS, "Toughness " + t, null, null, null, 0, null, t, t, 0));
        }
        final ManaCost mc = c.getManaCost();
        if (mc != null && !mc.isNoCost() && (mc.getGenericCost() > 0 || mc.isZero())) {
            final int g = mc.getGenericCost();
            result.add(new NumberNudge(NumberNudge.Kind.MANA_COST, "The {" + g + "} in its mana cost", null, null,
                    null, 0, null, g, g, 0));
        }

        for (final KeywordInterface kw : c.getKeywords()) {
            if (!kw.isIntrinsic()) {
                continue;
            }
            final String original = kw.getOriginal();
            final int colon = original.indexOf(':');
            if (colon < 0) {
                continue;
            }
            for (final String token : original.substring(colon + 1).split("[: ]")) {
                if (token.matches("\\d+")) {
                    final int n = Integer.parseInt(token);
                    result.add(new NumberNudge(NumberNudge.Kind.KEYWORD,
                            "The " + n + " in " + original.replace(':', ' '), null, null, null, 0, original, n, n, 0));
                    break;
                }
            }
        }

        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getKeyword() != null || !sa.isIntrinsic()) {
                continue;
            }
            addAbility(result, sa, sa.getParamOrDefault("SpellDescription", sa.getDescription()), abilityTree(sa));
            // an activated ability's cost prints its numbers too ("{2}, {T}: ..."); a spell's cost is its mana cost
            if (!sa.isSpell() && sa.hasParam("Cost")) {
                for (final String token : sa.getParam("Cost").split(" ")) {
                    if (token.matches("\\d+")) {
                        final int n = Integer.parseInt(token);
                        result.add(new NumberNudge(NumberNudge.Kind.COST, "The {" + n + "} in the cost of \""
                                + snippet(sa.getParamOrDefault("SpellDescription", sa.getDescription())) + "\"",
                                sa.getOriginalMapParams(), "Cost", null, 0, null, n, n, 0));
                        break;
                    }
                }
            }
        }
        for (final Trigger t : c.getTriggers()) {
            if (t.getKeyword() != null || !t.isIntrinsic()) {
                continue;
            }
            final List<CardTraitBase> tree = Lists.newArrayList(t);
            if (t.ensureAbility() != null) {
                tree.addAll(abilityTree(t.ensureAbility()));
            }
            addAbility(result, t, t.getParam("TriggerDescription"), tree);
        }
        for (final StaticAbility st : c.getStaticAbilities()) {
            if (st.getKeyword() != null || !st.isIntrinsic()) {
                continue;
            }
            addAbility(result, st, st.getParam("Description"), List.of(st));
        }
        for (final ReplacementEffect re : c.getReplacementEffects()) {
            if (re.getKeyword() != null || !re.isIntrinsic()) {
                continue;
            }
            final List<CardTraitBase> tree = Lists.newArrayList(re);
            if (re.ensureAbility() != null) {
                tree.addAll(abilityTree(re.ensureAbility()));
            }
            addAbility(result, re, re.getParam("Description"), tree);
        }

        // labels are what the player picks from, so they have to be distinct
        final List<String> seen = new ArrayList<>();
        final List<NumberNudge> unique = Lists.newArrayList();
        for (final NumberNudge n : result) {
            String label = n.label;
            for (int i = 2; seen.contains(label); i++) {
                label = n.label + " (" + i + ")";
            }
            seen.add(label);
            unique.add(label.equals(n.label) ? n : new NumberNudge(n.kind, label, n.paramTrait, n.key, n.descTrait,
                    n.occurrence, n.keyword, n.from, n.to, n.timestamp));
        }
        return unique;
    }

    private static boolean isPrintedNumber(final String s) {
        return s != null && s.matches("\\d+");
    }

    private static List<CardTraitBase> abilityTree(final SpellAbility sa) {
        final List<CardTraitBase> tree = Lists.newArrayList();
        for (SpellAbility cur = sa; cur != null; cur = cur.getSubAbility()) {
            tree.add(cur);
            for (final SpellAbility extra : cur.getAdditionalAbilities().values()) {
                tree.addAll(abilityTree(extra));
            }
            for (final List<AbilitySub> list : cur.getAdditionalAbilityLists().values()) {
                for (final AbilitySub extra : list) {
                    tree.addAll(abilityTree(extra));
                }
            }
        }
        return tree;
    }

    private record Found(CardTraitBase trait, String key, int value, CardTraitBase descTrait, String desc) { }

    // numbers in an ability's parameters, kept only where the text describing them prints that many of them - a
    // sub-ability with its own description ("Scry 1." then "Draw two cards.") is read against its own text
    private static void addAbility(final List<NumberNudge> result, final CardTraitBase root, final String rootDesc,
            final List<CardTraitBase> tree) {
        final List<Found> found = Lists.newArrayList();
        for (final CardTraitBase trait : tree) {
            CardTraitBase descTrait = root;
            String desc = rootDesc;
            if (trait != root && trait.hasParam("SpellDescription")) {
                descTrait = trait;
                desc = trait.getParam("SpellDescription");
            }
            if (desc == null || desc.isEmpty()) {
                continue;
            }
            final List<String> keys = Lists.newArrayList(trait.getMapParams().keySet());
            // +N/+N: the power bonus is printed first
            keys.sort(Comparator.comparing((String k) -> k.equals("NumAtt") ? 0 : k.equals("NumDef") ? 1 : 2)
                    .thenComparing(k -> k));
            for (final String key : keys) {
                if (key.equals("Cost") || key.endsWith("Description")) {
                    continue;
                }
                final String value = trait.getParam(key);
                if (value != null && value.matches("[+-]?\\d+")) {
                    found.add(new Found(trait, key, Math.abs(Integer.parseInt(value.replace("+", ""))), descTrait, desc));
                }
            }
        }
        final Map<String, Integer> used = new java.util.HashMap<>();
        for (final Found f : found) {
            final int occurrence = used.merge(System.identityHashCode(f.descTrait) + ":" + f.value, 1, Integer::sum) - 1;
            final int printed = NumberNudge.countIn(f.desc, f.value);
            if (occurrence >= printed) {
                continue;
            }
            final String which = printed > 1 && occurrence < ORDINALS.length - 1 ? ORDINALS[occurrence + 1] : "";
            result.add(new NumberNudge(NumberNudge.Kind.PARAM, "The " + which + f.value + " in \"" + snippet(f.desc) + "\"",
                    f.trait.getOriginalMapParams(), f.key, f.descTrait.getOriginalMapParams(), occurrence, null,
                    f.value, f.value, 0));
        }
    }

    private static String snippet(final String desc) {
        final String s = desc.replace("CARDNAME", "this").replace("\\n", " ");
        return s.length() > 90 ? s.substring(0, 87) + "..." : s;
    }
}
