package forge.ai.ability;

import java.util.ArrayList;
import java.util.List;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellApiToAi;
import forge.game.card.Card;
import forge.game.card.CardUtil;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * AILogic$ CastFromGraveyard (Mizzix's Mastery, Psionic Ritual, Sins of the Past, Spelltwine): an instant or sorcery
 * in a graveyard to cast for free. Each candidate is asked whether the AI would cast it now, as if it were in hand;
 * one it would is worth its mana value, and the card is used when that beats its own cost.
 */
public final class GraveCastAi {

    private GraveCastAi() { }

    /** What casting c now would be worth to the AI, or 0 when it wouldn't cast it. */
    static int castValue(final Player ai, final Card c) {
        final SpellAbility first = c.getFirstSpellAbility();
        if (first == null || !first.isSpell() || first.getApi() == null) {
            return 0;
        }
        // it's cast for free, so judged without its mana cost
        final SpellAbility spell = first.copyWithNoManaCost(ai);
        try {
            final AiAbilityDecision d = SpellApiToAi.Converter.get(spell).canPlayFromElsewhere(ai, spell);
            return d.willingToPlay() ? 50 + c.getCMC() * 30 : 0;
        } catch (final RuntimeException e) {
            // a spell whose AI assumes it's cast from hand

            return 0;
        }
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        final List<SpellAbility> tgts = new ArrayList<>();
        for (SpellAbility sub = sa; sub != null; sub = sub.getSubAbility()) {
            if (sub.usesTargeting()) {
                tgts.add(sub);
            }
        }
        if (tgts.isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        int total = 0;
        final List<Card> picks = new ArrayList<>();
        for (final SpellAbility t : tgts) {
            t.resetTargets();
            Card best = null;
            int bestV = 0;
            for (final Card c : CardUtil.getValidCardsToTarget(t)) {
                if (picks.contains(c) || c.equals(sa.getHostCard())) {
                    continue;
                }
                final int v = castValue(ai, c);
                if (v > bestV) {
                    bestV = v;
                    best = c;
                }
            }
            // Spelltwine needs both of its targets
            if (best == null && t.getMinTargets() > 0) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            picks.add(best);
            total += bestV;
        }
        if (total < 60 + sa.getHostCard().getCMC() * 10) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        for (int i = 0; i < tgts.size(); i++) {
            if (picks.get(i) != null) {
                tgts.get(i).getTargets().add(picks.get(i));
            }
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
}
