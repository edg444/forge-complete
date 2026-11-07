package forge.ai.simulation;

import forge.ai.AiController;
import forge.ai.ComputerUtil;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * The fork's guard against discovered creature casts before combat. Kept out of upstream's files so their daily edits
 * merge cleanly.
 *
 * A main phase rollout of the pass line never plays the rest of the turn out (FlatMonteCarloPicker.rolloutMain), so
 * a creature the heuristics would cast after combat is missing from that line's score, and casting it before combat
 * always looks better by the value of the body. Only the heuristics know when the first main phase matters (haste,
 * pumps for the attack, exalted, backup, BuffedBy...), so their "after combat" stands.
 */
public final class FlatMcMain1Guard {
    private FlatMcMain1Guard() {
    }

    public static boolean waitsForMain2(final AiController brain, final Player ai, final SpellAbility sa) {
        final Card host = sa == null ? null : sa.getHostCard();
        if (host == null || !sa.isSpell() || sa.getApi() != ApiType.PermanentCreature) {
            return false;
        }
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.is(PhaseType.MAIN1, ai) || sa.hasParam("WithoutManaCost") || sa.isCastFromPlayEffect()
                || ai.getGame().getReplacementHandler().wouldPhaseBeSkipped(ai, PhaseType.MAIN2)) {
            return false;
        }
        // a combo piece, or a creature whose search finds something to cast this turn, may need the first main phase:
        // the heuristics cannot tell, so the rollouts weigh those as before
        if (brain.deckPlan(ai).combos.isKeyCard(host) || TutorPlan.hasUse(ai, sa)) {
            return false;
        }
        return !ComputerUtil.castPermanentInMain1(ai, sa);
    }
}
