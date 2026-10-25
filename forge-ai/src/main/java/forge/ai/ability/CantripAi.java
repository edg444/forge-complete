package forge.ai.ability;

import java.util.List;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.game.GameObject;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * AILogic$ Cantrip (Skyshroud Blessing, Spiritualize, Sway of Illusion, Touch of Vitae, Verdant Rebirth, Quicken,
 * Scout's Warning): a card that replaces itself and whose own effect rarely matters. Cast at the opponent's end step
 * with mana that would go unused anyway, on its own creature when it targets one. Verdant Rebirth also answers removal
 * on its creature, and Skyshroud Blessing answers an effect aimed at its land.
 */
public final class CantripAi {

    private CantripAi() { }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        SpellAbility tgt = null;
        for (SpellAbility sub = sa; sub != null; sub = sub.getSubAbility()) {
            if (sub.usesTargeting()) {
                tgt = sub;
                break;
            }
        }
        if (tgt != null) {
            tgt.resetTargets();
        }
        if (!ai.getGame().getStack().isEmpty()) {
            final List<GameObject> doomed = ComputerUtil.predictThreatenedObjects(ai, null);
            // Verdant Rebirth: the creature comes back to hand
            if (sa.getApi() == ApiType.Animate && sa.hasParam("Triggers") && tgt != null) {
                final Card save = ComputerUtilCard.getBestCreatureAI(CardLists.filter(CardUtil.getValidCardsToTarget(tgt),
                        c -> c.getController().equals(ai) && doomed.contains(c)));
                if (save != null) {
                    tgt.getTargets().add(save);
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
            }
            // Skyshroud Blessing: all lands gain shroud
            if (sa.getApi() == ApiType.PumpAll && sa.getParamOrDefault("KW", "").contains("Shroud")
                    && doomed.stream().anyMatch(o -> o instanceof Card c && c.isLand() && c.getController().equals(ai))) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }
        // Overmaster, Insist: a sorcery goes in its own second main phase instead
        final boolean sorcery = sa.isSpell() && sa.getHostCard().isSorcery();
        if (sorcery ? !ph.isPlayerTurn(ai) || !ph.is(PhaseType.MAIN2) : !ph.is(PhaseType.END_OF_TURN) || !ph.getNextTurn().equals(ai)) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }
        if (tgt != null && tgt.getMinTargets() > 0) {
            final List<Card> options = CardUtil.getValidCardsToTarget(tgt);
            final Card own = ComputerUtilCard.getBestCreatureAI(CardLists.filter(options, c -> c.getController().equals(ai)));
            if (own != null) {
                tgt.getTargets().add(own);
            } else if (!options.isEmpty()) {
                tgt.getTargets().add(options.get(0));
            } else {
                return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
            }
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
}
