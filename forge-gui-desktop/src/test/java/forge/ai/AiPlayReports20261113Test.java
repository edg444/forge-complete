package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import forge.ai.simulation.ActivationGuard;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** AI misplays reported from play on 2026-11-13, each set up the way it happened. */
public class AiPlayReports20261113Test extends AITest {
    private static final int LONG_TIMEOUT_MS = 240_000;

    private Game game;
    private Player ai;
    private Player opp;

    @DataProvider(name = "modes")
    public Object[][] modes() {
        return new Object[][] { { false }, { true } };
    }

    private void setUp(final boolean flatMc) {
        game = initAndCreateGame();
        game.AI_FLAT_MONTE_CARLO = flatMc;
        game.AI_FLAT_MONTE_CARLO_MAX = flatMc;
        game.AI_TIMEOUT = 20;
        ai = game.getPlayers().get(1);
        opp = game.getPlayers().get(0);
        ai.setTeam(0);
        opp.setTeam(1);
        for (int i = 0; i < 10; i++) {
            addCardToZone("Mountain", ai, ZoneType.Library);
            addCardToZone("Plains", opp, ZoneType.Library);
        }
    }

    private Card ready(final String name, final Player owner) {
        final Card c = addCard(name, owner);
        c.setSickness(false);
        return c;
    }

    private SpellAbility nextPlay() {
        final AiController brain = ((PlayerControllerAi) ai.getController()).getAi();
        final List<SpellAbility> plays = brain.chooseSpellAbilityToPlay();
        return plays == null || plays.isEmpty() ? null : plays.get(0);
    }

    private static String describe(final SpellAbility sa) {
        return sa == null ? "pass" : sa.getHostCard().getName() + ": " + sa + " -> " + sa.getTargetCard();
    }

    // ------------------------------------------------------------------------------------------ Soulbright Flamekin

    /**
     * The AI attacks with a Craw Wurm, the Flamekin's ability has already resolved {@code resolved} times this turn and
     * the AI has {@code lands} Mountains. With {@code blocked}, the opponent's Grizzly Bears block the Wurm.
     */
    private SpellAbility flamekinCombat(final boolean flatMc, final int resolved, final int lands, final boolean blocked) {
        setUp(flatMc);
        for (int i = 0; i < lands; i++) {
            ready("Mountain", ai);
        }
        final Card flamekin = ready("Soulbright Flamekin", ai);
        final SpellAbility ability = flamekin.getSpellAbilities().stream().filter(SpellAbility::isActivatedAbility).findFirst().orElseThrow();
        for (int i = 0; i < resolved; i++) {
            flamekin.addAbilityResolved(ability);
        }
        final Card wurm = ready("Craw Wurm", ai);
        final Card bears = ready("Grizzly Bears", opp);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai, false);
        final Combat combat = new Combat(ai);
        combat.addAttacker(wurm, opp);
        if (blocked) {
            combat.addBlocker(wurm, bears);
        }
        combat.setBlocked(wurm, blocked);
        game.getPhaseHandler().setCombat(combat);
        game.getAction().checkStateEffects(true);
        ability.setActivatingPlayer(ai);
        return ability;
    }

    // the Wurm went unblocked and only one or two more activations are affordable: trample does nothing for it, and the
    // third resolution's {R}{R}{R}{R}{R}{R}{R}{R} is out of reach
    @Test(dataProvider = "modes", timeOut = LONG_TIMEOUT_MS)
    public void testFlamekinKeptForAnUnblockedAttacker(final boolean flatMc) {
        flamekinCombat(flatMc, 0, 4, false);
        final SpellAbility play = nextPlay();
        AssertJUnit.assertTrue("[flatMc=" + flatMc + "] picked " + describe(play),
                play == null || !"Soulbright Flamekin".equals(play.getHostCard().getName()));
    }

    @Test(dataProvider = "modes", timeOut = LONG_TIMEOUT_MS)
    public void testFlamekinKeptWhenTheThirdIsOutOfReach(final boolean flatMc) {
        flamekinCombat(flatMc, 1, 3, false);
        final SpellAbility play = nextPlay();
        AssertJUnit.assertTrue("[flatMc=" + flatMc + "] picked " + describe(play),
                play == null || !"Soulbright Flamekin".equals(play.getHostCard().getName()));
    }

    /** The guard's verdict on the Flamekin's ability in {@link #flamekinCombat}: the game is set up first. */
    private String guard(final int resolved, final int lands, final boolean blocked) {
        final SpellAbility ability = flamekinCombat(true, resolved, lands, blocked);
        return ActivationGuard.problem(ai, ability);
    }

    @Test
    public void testFlamekinGuard() {
        AssertJUnit.assertNotNull("unblocked, 2 of 3 affordable", guard(0, 4, false));
        AssertJUnit.assertNotNull("unblocked, 1 resolved, 1 of 2 affordable", guard(1, 3, false));
        AssertJUnit.assertNull("unblocked, all 3 affordable", guard(0, 6, false));
        AssertJUnit.assertNull("unblocked, the third resolution", guard(2, 2, false));
        AssertJUnit.assertNull("blocked: trample matters", guard(0, 2, true));
    }

    // ------------------------------------------------------------------------------------------ Saddle

    // the AI saddled Gila Courser with Raging Goblin and then kept the Courser home: a 4/2 into an untapped Hill Giant
    @Test(dataProvider = "modes", timeOut = LONG_TIMEOUT_MS)
    public void testNoSaddleWhenTheMountStaysHome(final boolean flatMc) {
        setUp(flatMc);
        ready("Mountain", ai);
        final Card courser = ready("Gila Courser", ai);
        ready("Raging Goblin", ai);
        ready("Hill Giant", opp);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai, false);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse("setup: the Courser stays home", ComputerUtilCard.doesSpecifiedCreatureAttackAI(ai, courser));
        final SpellAbility play = nextPlay();
        AssertJUnit.assertTrue("[flatMc=" + flatMc + "] picked " + describe(play),
                play == null || !play.isKeyword(Keyword.SADDLE));
    }

    @Test
    public void testSaddleWhenTheMountAttacks() {
        setUp(false);
        ready("Mountain", ai);
        final Card courser = ready("Gila Courser", ai);
        ready("Raging Goblin", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai, false);
        game.getAction().checkStateEffects(true);
        SpellAbility saddle = null;
        for (final SpellAbility sa : courser.getSpellAbilities()) {
            if (sa.isKeyword(Keyword.SADDLE)) {
                saddle = sa;
            }
        }
        AssertJUnit.assertNotNull(saddle);
        saddle.setActivatingPlayer(ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(saddle));
        AssertJUnit.assertFalse("the decision leaves the real Courser alone", courser.isSaddled());
    }
}
