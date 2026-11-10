package forge.ai.ability;

import java.util.List;
import java.util.Map;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Draft an ability: the script lists each pool best-first, so the AI keeps the offered ability that comes earliest
 * in Abilities$. Fork-only.
 */
public class DraftAbilityAi extends SpellAbilityAi {
    @Override
    protected AiAbilityDecision canPlay(Player ai, SpellAbility sa) {
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    @SuppressWarnings("unchecked")
    public SpellAbility chooseSingleSpellAbility(Player ai, SpellAbility sa, List<SpellAbility> spells, Map<String, Object> params) {
        final List<String> names = params == null ? null : (List<String>) params.get("DraftedFrom");
        if (names == null || names.size() != spells.size()) {
            return spells.get(0);
        }
        final List<String> pool = new java.util.ArrayList<>();
        for (final String name : sa.getParam("Abilities").split(",")) {
            pool.add(name.trim());
        }
        int best = 0;
        for (int i = 1; i < spells.size(); i++) {
            if (pool.indexOf(names.get(i)) < pool.indexOf(names.get(best))) {
                best = i;
            }
        }
        return spells.get(best);
    }
}
