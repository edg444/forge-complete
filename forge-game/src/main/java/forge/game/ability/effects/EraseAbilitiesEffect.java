package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import forge.game.CardTraitBase;
import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.ApiType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardTraitChanges;
import forge.game.card.perpetual.PerpetualAbilities;
import forge.game.card.perpetual.PerpetualKeywords;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordInterface;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityMode;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;

/**
 * Sharp Eraser: erase any number of whole keywords and abilities from the card (user ruling 2026-10-21) - not the
 * spell's own effect, which the card couldn't make sense without. They're gone for the rest of the game, in every zone.
 * The AI erases only what Forge's AI already counts against a creature (CreatureEvaluator's bad keywords and upkeep
 * costs), and nothing else.
 */
public class EraseAbilitiesEffect extends SpellAbilityEffect {

    private static final String DONE = "(done erasing)";

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Erase any number of keywords and abilities on the card.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Player p = sa.getActivatingPlayer();
        final Game game = p.getGame();
        for (final Card card : getDefinedCardsOrTargeted(sa)) {
            final Map<String, KeywordInterface> keywords = new LinkedHashMap<>();
            for (final KeywordInterface k : card.getKeywords()) {
                if (k.isIntrinsic()) {
                    keywords.put(k.getOriginal(), k);
                }
            }
            final Map<String, CardTraitBase> traits = new LinkedHashMap<>();
            for (final Trigger t : card.getTriggers()) {
                if (t.isIntrinsic() && t.getKeyword() == null && t.hasParam("TriggerDescription")) {
                    traits.put(t.getParam("TriggerDescription"), t);
                }
            }
            for (final StaticAbility st : card.getStaticAbilities()) {
                if (st.isIntrinsic() && st.getKeyword() == null && st.hasParam("Description")) {
                    traits.put(st.getParam("Description"), st);
                }
            }
            for (final ReplacementEffect re : card.getReplacementEffects()) {
                if (re.isIntrinsic() && re.getKeyword() == null && re.hasParam("Description")) {
                    traits.put(re.getParam("Description"), re);
                }
            }
            for (final SpellAbility ab : card.getSpellAbilities()) {
                if (ab.isIntrinsic() && !ab.isSpell() && ab.getKeyword() == null && ab.hasParam("SpellDescription")) {
                    traits.put(ab.getParam("SpellDescription"), ab);
                }
            }

            final Set<String> erased = new HashSet<>();
            if (p.getController().isAI()) {
                for (final Map.Entry<String, KeywordInterface> e : keywords.entrySet()) {
                    if (isDrawback(e.getValue())) {
                        erased.add(e.getKey());
                    }
                }
                for (final Map.Entry<String, CardTraitBase> e : traits.entrySet()) {
                    if (isDrawback(e.getValue())) {
                        erased.add(e.getKey());
                    }
                }
            } else {
                final List<String> options = new ArrayList<>(keywords.keySet());
                options.addAll(traits.keySet());
                while (!options.isEmpty()) {
                    final List<String> menu = new ArrayList<>(options);
                    menu.add(0, DONE);
                    final String pick = p.getController().chooseSomeType("text to erase from " + card.getName(), sa, menu);
                    if (pick == null || DONE.equals(pick)) {
                        break;
                    }
                    erased.add(pick);
                    options.remove(pick);
                }
            }
            if (erased.isEmpty()) {
                continue;
            }

            final long ts = game.getNextTimestamp();
            final List<String> removeKeywords = new ArrayList<>();
            for (final String k : keywords.keySet()) {
                if (erased.contains(k)) {
                    removeKeywords.add(k);
                }
            }
            if (!removeKeywords.isEmpty()) {
                final PerpetualKeywords lose = new PerpetualKeywords(ts, List.of(), removeKeywords, false);
                card.addPerpetual(lose);
                lose.applyEffect(card);
            }
            final Set<String> erasedTraits = new HashSet<>(erased);
            erasedTraits.removeAll(removeKeywords);
            if (!erasedTraits.isEmpty()) {
                // matched by their text, so the same abilities are gone from the card in whatever zone it moves to
                final PerpetualAbilities lose = new PerpetualAbilities(ts, new CardTraitChanges(null, null, null, null,
                        t -> erasedTraits.contains(descriptionOf(t))));
                card.addPerpetual(lose);
                lose.applyEffect(card);
            }
            card.updateStateForView();
            game.getGameLog().add(GameLogEntryType.INFORMATION, p + " erases from " + card + ": " + String.join("; ", erased));
        }
    }

    private static String descriptionOf(final CardTraitBase t) {
        if (t instanceof Trigger) {
            return t.getParam("TriggerDescription");
        }
        if (t instanceof SpellAbility) {
            return t.getParam("SpellDescription");
        }
        return t.getParam("Description");
    }

    /** What Forge's AI already counts against a creature (CreatureEvaluator's "bad keywords" and upkeep costs). */
    private static boolean isDrawback(final KeywordInterface k) {
        final Keyword kw = k.getKeyword();
        return kw == Keyword.DEFENDER || kw == Keyword.PHASING || kw == Keyword.CUMULATIVE_UPKEEP || kw == Keyword.ECHO
                || kw == Keyword.FADING || kw == Keyword.VANISHING
                || "CARDNAME can't attack.".equals(k.getOriginal()) || "CARDNAME can't block.".equals(k.getOriginal())
                || "CARDNAME can't attack or block.".equals(k.getOriginal());
    }

    private static boolean isDrawback(final CardTraitBase t) {
        if (t instanceof StaticAbility st) {
            return (st.checkMode(StaticAbilityMode.CantAttack) || st.checkMode(StaticAbilityMode.CantBlock))
                    && "Card.Self".equals(st.getParamOrDefault("ValidCard", ""));
        }
        if (t instanceof Trigger tr && tr.getMode() == TriggerType.Phase && "Upkeep".equals(tr.getParam("Phase"))) {
            final SpellAbility ab = tr.ensureAbility();
            if (ab == null) {
                return false;
            }
            return ab.getApi() == ApiType.DealDamage && "You".equals(ab.getParamOrDefault("Defined", "You"))
                    || ab.getApi() == ApiType.Sacrifice && ab.hasParam("UnlessCost");
        }
        return false;
    }
}
