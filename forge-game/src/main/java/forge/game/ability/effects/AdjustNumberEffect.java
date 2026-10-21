package forge.game.ability.effects;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import forge.card.CardStateName;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.game.CardTraitBase;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardState;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;

/**
 * Truss, Chief Engineer: add or subtract Amount$ from a number or number word on the target spell or permanent until
 * end of turn, never below 1 (or one). The numbers it can reach (user, 2026-10-21): power, toughness and loyalty, a
 * spell's generic mana, and the literal numbers in its abilities - damage, cards, life, counters, tokens, dice and sides,
 * pumps and the like. An X can't be changed (rulings). Names and collector numbers stay as printed.
 */
public class AdjustNumberEffect extends SpellAbilityEffect {

    /** Ability parameters holding a printed number, by what they count. */
    public static final Set<String> NUMBER_PARAMS = Set.of("NumDmg", "NumCards", "LifeAmount", "CounterNum", "Amount",
            "TokenAmount", "NumAtt", "NumDef", "ChangeNum", "DigNum", "Sides", "ScryNum", "SurveilNum", "Num", "NumTurns",
            "AddPower", "AddToughness", "Power", "Toughness", "Times", "MaxRepeat", "NumCopies");

    /** One number on the card that can be changed. */
    public record Number(String label, int value, String sign, CardTraitBase trait, String param) { }

    public static List<Number> numbersOn(final Card c) {
        final List<Number> numbers = new java.util.ArrayList<>();
        final CardState state = c.getCurrentState();
        if (c.isCreature()) {
            numbers.add(new Number("power " + state.getBasePower(), state.getBasePower(), "", null, "Power"));
            numbers.add(new Number("toughness " + state.getBaseToughness(), state.getBaseToughness(), "", null, "Toughness"));
        }
        if (c.isPlaneswalker() && state.getBaseLoyalty() != null && state.getBaseLoyalty().matches("\\d+")) {
            numbers.add(new Number("loyalty " + state.getBaseLoyalty(), Integer.parseInt(state.getBaseLoyalty()), "", null, "Loyalty"));
        }
        if (c.isSpell() || c.getZone() != null && c.getZone().is(forge.game.zone.ZoneType.Stack)) {
            final int generic = state.getManaCost().getGenericCost();
            if (generic > 0) {
                numbers.add(new Number("{" + generic + "} in its mana cost", generic, "", null, "ManaCost"));
            }
        }
        final List<CardTraitBase> traits = new java.util.ArrayList<>();
        for (final SpellAbility sa : c.getSpellAbilities()) {
            for (SpellAbility part = sa; part != null; part = part.getSubAbility()) {
                traits.add(part);
            }
        }
        for (final Trigger t : c.getTriggers()) {
            for (SpellAbility part = t.ensureAbility(); part != null; part = part.getSubAbility()) {
                traits.add(part);
            }
        }
        for (final StaticAbility st : c.getStaticAbilities()) {
            traits.add(st);
        }
        for (final CardTraitBase trait : traits) {
            for (final Map.Entry<String, String> p : trait.getMapParams().entrySet()) {
                if (!NUMBER_PARAMS.contains(p.getKey())) {
                    continue;
                }
                final String v = p.getValue().trim();
                final String sign = v.startsWith("+") || v.startsWith("-") ? v.substring(0, 1) : "";
                final String digits = v.substring(sign.length());
                if (!digits.matches("\\d+")) {
                    continue;
                }
                String where = trait instanceof SpellAbility sa ? sa.getDescription() : trait.toString();
                if (where == null || where.isBlank()) {
                    where = trait.toString();
                }
                if (where.length() > 60) {
                    where = where.substring(0, 57) + "...";
                }
                numbers.add(new Number(digits + " in \"" + where + "\"", Integer.parseInt(digits), sign, trait, p.getKey()));
            }
        }
        return numbers;
    }

    /** The new value: changed by amount, but never reduced below 1 (rulings). */
    public static int adjust(final int value, final int amount, final boolean add) {
        if (add) {
            return value + amount;
        }
        return value - amount >= 1 ? value - amount : Math.min(value, 1);
    }

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Add or subtract X from a number or number word on " + getTargetCards(sa) + " until end of turn.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player you = sa.getActivatingPlayer();
        final Game game = you.getGame();
        final int amount = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Amount", "X"), sa);
        for (final Card target : getTargetCards(sa)) {
            final Card c = game.getCardState(target);
            final List<Number> numbers = numbersOn(c);
            if (numbers.isEmpty() || amount <= 0) {
                continue;
            }
            final Map<String, Number> byLabel = Maps.newLinkedHashMap();
            for (final Number n : numbers) {
                String label = n.label();
                for (int i = 2; byLabel.containsKey(label); i++) {
                    label = n.label() + " (" + i + ")";
                }
                byLabel.put(label, n);
            }
            final Number number;
            final boolean add;
            if (you.getController().isAI()) {
                // the AI's own creature hits harder; anyone else's gets smaller
                final boolean own = you.equals(c.getController());
                number = numbers.stream().filter(n -> "Power".equals(n.param())).findFirst().orElse(numbers.get(0));
                add = own;
            } else {
                final String chosen = you.getController().chooseSomeType("number", sa, ImmutableList.copyOf(byLabel.keySet()));
                if (chosen == null) {
                    continue;
                }
                number = byLabel.get(chosen);
                add = you.getController().confirmAction(sa, PlayerActionConfirmMode.OptionalChoose,
                        "Add " + amount + " to " + number.label() + ", or subtract it?",
                        ImmutableList.of("Add " + amount, "Subtract " + amount), null, null);
            }
            final int changed = adjust(number.value(), amount, add);
            apply(sa, c, number, changed);
            game.getAction().notifyOfValue(sa, c, number.label() + " becomes " + changed + " until end of turn", null);
        }
    }

    public static void apply(final SpellAbility sa, final Card c, final Number number, final int changed) {
        final Game game = c.getGame();
        final long ts = game.getNextTimestamp();
        switch (number.param()) {
            case "Power", "Toughness" -> {
                if (number.trait() == null) {
                    final boolean power = "Power".equals(number.param());
                    c.addNewPT(power ? changed : null, power ? null : changed, ts, 0);
                    addUntilCommand(sa, () -> c.removeNewPT(ts, 0));
                    return;
                }
            }
            case "Loyalty" -> {
                final CardState state = c.getState(CardStateName.Original);
                final String old = state.getBaseLoyalty();
                state.setBaseLoyalty(Integer.toString(changed));
                addUntilCommand(sa, () -> state.setBaseLoyalty(old));
                return;
            }
            case "ManaCost" -> {
                final CardState state = c.getCurrentState();
                final ManaCost old = state.getManaCost();
                final StringBuilder cost = new StringBuilder(Integer.toString(changed));
                for (final forge.card.mana.ManaCostShard shard : old) {
                    cost.append(' ').append(shard.toString().replace("{", "").replace("}", ""));
                }
                state.setManaCost(new ManaCost(new ManaCostParser(cost.toString())));
                addUntilCommand(sa, () -> state.setManaCost(old));
                return;
            }
            default -> {
            }
        }
        final CardTraitBase trait = number.trait();
        final String old = trait.getParam(number.param());
        trait.putParam(number.param(), number.sign() + changed);
        addUntilCommand(sa, () -> trait.putParam(number.param(), old));
    }
}
