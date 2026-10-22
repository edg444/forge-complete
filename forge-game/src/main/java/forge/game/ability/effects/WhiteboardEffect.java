package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import forge.game.CardTraitBase;
import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardState;
import forge.game.keyword.KeywordInterface;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;

/**
 * The Whiteboarder. Mode$ Note: note the Defined$ creature spell's power, toughness, or one of its abilities on the
 * host. Mode$ Create: a white-bordered blue Elemental token with a noted power and toughness and up to two noted
 * abilities, and those notes are erased. An AI notes keyword abilities first, else the bigger number, and uses its
 * biggest numbers and first abilities.
 */
public class WhiteboardEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Note".equals(sa.getParam("Mode")) ? "Note that spell's power, toughness, or one of its abilities."
                : "Create a white-bordered blue Elemental token from the notes.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        if ("Note".equals(sa.getParam("Mode"))) {
            note(sa);
        } else {
            create(sa);
        }
    }

    private static void note(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player p = sa.getActivatingPlayer();
        final List<Card> spells = getDefinedCardsOrTargeted(sa);
        if (spells.isEmpty()) {
            return;
        }
        final Card spell = spells.get(0);
        final Map<String, Card.WhiteboardNote> options = new LinkedHashMap<>();
        final Map<String, Card.WhiteboardNote> keywords = new LinkedHashMap<>();
        options.put("Power: " + spell.getNetPower(), new Card.WhiteboardNote("Power", "Power " + spell.getNetPower(), spell.getNetPower()));
        options.put("Toughness: " + spell.getNetToughness(), new Card.WhiteboardNote("Toughness", "Toughness " + spell.getNetToughness(), spell.getNetToughness()));
        for (final KeywordInterface kw : spell.getKeywords()) {
            if (kw.getOriginal().startsWith("HIDDEN") || kw.getOriginal().startsWith("etbCounter")) {
                continue;
            }
            final Card.WhiteboardNote n = new Card.WhiteboardNote("Ability", kw.getTitle(), kw.getOriginal());
            options.putIfAbsent("Ability: " + kw.getTitle(), n);
            keywords.putIfAbsent("Ability: " + kw.getTitle(), n);
        }
        final List<CardTraitBase> traits = new ArrayList<>();
        for (final Trigger t : spell.getTriggers()) {
            if (t.getKeyword() == null && !t.isSecondary()) {
                traits.add(t);
            }
        }
        for (final StaticAbility st : spell.getStaticAbilities()) {
            if (st.getKeyword() == null && !st.isSecondary() && st.hasParam("Description")) {
                traits.add(st);
            }
        }
        for (final SpellAbility ab : spell.getSpellAbilities()) {
            if (!ab.isSpell() && ab.getKeyword() == null) {
                traits.add(ab);
            }
        }
        for (final CardTraitBase t : traits) {
            final String text = t instanceof SpellAbility s ? s.getDescription() : t.toString();
            if (text != null && !text.isEmpty()) {
                options.putIfAbsent("Ability: " + text, new Card.WhiteboardNote("Ability", text, t));
            }
        }
        final String pick;
        if (p.getController().isAI()) {
            pick = !keywords.isEmpty() ? keywords.keySet().iterator().next()
                    : spell.getNetPower() >= spell.getNetToughness() ? "Power: " + spell.getNetPower() : "Toughness: " + spell.getNetToughness();
        } else {
            pick = p.getController().chooseSomeType("thing to note", sa, new ArrayList<>(options.keySet()));
        }
        final Card.WhiteboardNote noted = pick == null ? null : options.get(pick);
        if (noted != null) {
            host.getWhiteboardNotes().add(noted);
            host.getGame().getGameLog().add(GameLogEntryType.INFORMATION, p + " notes " + noted.label() + " (from " + spell.getName() + ").");
        }
    }

    private static void create(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player p = sa.getActivatingPlayer();
        final Game game = p.getGame();
        final List<Card.WhiteboardNote> notes = host.getWhiteboardNotes();
        final Card.WhiteboardNote power = pickNumber(sa, p, notes, "Power");
        final Card.WhiteboardNote toughness = pickNumber(sa, p, notes, "Toughness");
        final List<Card.WhiteboardNote> abilities = new ArrayList<>();
        final List<Card.WhiteboardNote> abilityNotes = new ArrayList<>(notes.stream().filter(n -> "Ability".equals(n.kind())).toList());
        while (abilities.size() < 2 && !abilityNotes.isEmpty()) {
            final Card.WhiteboardNote n;
            if (p.getController().isAI()) {
                n = abilityNotes.get(0);
            } else {
                final Map<String, Card.WhiteboardNote> byLabel = new LinkedHashMap<>();
                abilityNotes.forEach(x -> byLabel.putIfAbsent(x.label(), x));
                final String pick = p.getController().chooseSomeType("noted ability to give it (or none)", sa,
                        new ArrayList<>(byLabel.keySet()), true);
                n = pick == null ? null : byLabel.get(pick);
            }
            if (n == null) {
                break;
            }
            abilities.add(n);
            abilityNotes.remove(n);
        }

        final SpellAbility make = AbilityFactory.getAbility("DB$ Token | TokenScript$ u_0_0_elemental | RememberTokens$ True", host);
        make.setActivatingPlayer(p);
        final List<Object> before = com.google.common.collect.Lists.newArrayList(host.getRemembered());
        AbilityUtils.resolve(make);
        for (final Object o : com.google.common.collect.Lists.newArrayList(host.getRemembered())) {
            if (before.contains(o) || !(o instanceof Card token)) {
                continue;
            }
            host.removeRemembered(token);
            final CardState state = token.getCurrentState();
            state.setBasePower(power == null ? 0 : (Integer) power.value());
            state.setBaseToughness(toughness == null ? 0 : (Integer) toughness.value());
            for (final Card.WhiteboardNote n : abilities) {
                if (n.value() instanceof String kw) {
                    state.addIntrinsicKeyword(kw, true);
                } else if (n.value() instanceof Trigger t) {
                    state.addTrigger(t.copy(token, false));
                } else if (n.value() instanceof StaticAbility st) {
                    state.addStaticAbility(st.copy(token, false, false));
                } else if (n.value() instanceof SpellAbility ab) {
                    state.addSpellAbility(ab.copy(token, false));
                }
            }
            token.setSVar("WhiteBorderedToken", "True");
            token.updateStateForView();
            game.getTriggerHandler().clearActiveTriggers(token, null);
            game.getTriggerHandler().registerActiveTrigger(token, false);
        }
        notes.remove(power);
        notes.remove(toughness);
        notes.removeAll(abilities);
    }

    private static Card.WhiteboardNote pickNumber(final SpellAbility sa, final Player p, final List<Card.WhiteboardNote> notes, final String kind) {
        final List<Card.WhiteboardNote> numbers = notes.stream().filter(n -> kind.equals(n.kind())).toList();
        if (numbers.isEmpty()) {
            return null;
        }
        if (p.getController().isAI() || numbers.size() == 1) {
            return numbers.stream().max((a, b) -> Integer.compare((Integer) a.value(), (Integer) b.value())).orElse(null);
        }
        final Map<String, Card.WhiteboardNote> byLabel = new LinkedHashMap<>();
        numbers.forEach(x -> byLabel.putIfAbsent(x.label(), x));
        final String pick = p.getController().chooseSomeType("noted " + kind.toLowerCase(), sa, new ArrayList<>(byLabel.keySet()));
        return pick == null ? numbers.get(0) : byLabel.get(pick);
    }
}
