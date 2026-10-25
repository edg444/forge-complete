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
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.zone.ZoneType;

/**
 * AILogic$ Polymorph (Mass Polymorph, Synthetic Destiny, Reweave): its own creatures traded for creatures from its
 * library, worth it when what it gives up (tokens, small creatures) is worth less than the average creature card
 * still in its library, or in answer to a spell that would destroy them anyway.
 */
public final class PolymorphAi {

    private PolymorphAi() { }

    private static AiAbilityDecision no(final AiPlayDecision why) {
        return new AiAbilityDecision(0, why);
    }

    // what a creature card from its library is worth on average
    private static int average(final Player ai) {
        final List<Card> creatures = CardLists.filter(ai.getCardsIn(ZoneType.Library), Card::isCreature);
        if (creatures.size() < 3) {
            return 0;
        }
        return (int) creatures.stream().mapToInt(ComputerUtilCard::evaluateCreature).average().orElse(0);
    }

    private static boolean wrathComing(final Player ai) {
        for (final SpellAbilityStackInstance si : ai.getGame().getStack()) {
            final SpellAbility s = si.getSpellAbility();
            if (s.getActivatingPlayer().isOpponentOf(ai) && (s.getApi() == ApiType.DestroyAll || s.getApi() == ApiType.DamageAll)) {
                return true;
            }
        }
        return false;
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        final int avg = average(ai);
        if (avg == 0) {
            return no(AiPlayDecision.CantPlayAi);
        }
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (sa.usesTargeting()) {
            // Reweave: its weakest creature, a token being the best of all to give up
            final List<GameObject> doomed = ai.getGame().getStack().isEmpty() ? List.of() : ComputerUtil.predictThreatenedObjects(ai, null);
            Card best = null;
            int gain = 0;
            for (final Card c : CardUtil.getValidCardsToTarget(sa)) {
                if (!c.getController().equals(ai) || !c.isCreature()) {
                    continue;
                }
                final int g = avg - (doomed.contains(c) ? 0 : ComputerUtilCard.evaluateCreature(c) + (c.isToken() ? -60 : 0));
                if (g > gain) {
                    gain = g;
                    best = c;
                }
            }
            if (best == null || gain < 80) {
                return no(AiPlayDecision.TargetingFailed);
            }
            sa.resetTargets();
            sa.getTargets().add(best);
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        final List<Card> mine = ai.getCreaturesInPlay();
        if (mine.isEmpty()) {
            return no(AiPlayDecision.CantPlayAi);
        }
        if (wrathComing(ai)) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        if (!ph.isPlayerTurn(ai) || !ph.is(PhaseType.MAIN2)) {
            return no(AiPlayDecision.AnotherTime);
        }
        final int now = mine.stream().mapToInt(ComputerUtilCard::evaluateCreature).sum();
        return mine.size() * avg - now >= 200 ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : no(AiPlayDecision.CantPlayAi);
    }
}
