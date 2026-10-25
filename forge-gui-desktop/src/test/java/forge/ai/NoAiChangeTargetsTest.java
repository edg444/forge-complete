package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** ChangeTargetsAi and PlayerControllerAi.chooseNewTargetsFor: retargeting what's on the stack, scored by RetargetAi. */
public class NoAiChangeTargetsTest extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        sa.resetTargets();
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    private SpellAbility cast(Game game, Player p, String name, GameEntity target) {
        SpellAbility sa = addCardToZone(name, p, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        sa.getTargets().add(target);
        game.getStack().add(sa);
        return sa;
    }

    private void lands(Player p, String name, int n) {
        for (int i = 0; i < n; i++) {
            addCard(name, p);
        }
    }

    // Deflection: "Change the target of target spell with a single target." - a Bolt at its creature goes to theirs
    @Test
    public void testDeflectionSendsTheBoltBack() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Island", 4);
        Card giant = addCard("Hill Giant", ai);
        Card bears = addCard("Grizzly Bears", opp);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        SpellAbility bolt = cast(game, opp, "Lightning Bolt", giant);
        SpellAbility sa = addCardToZone("Deflection", ai, ZoneType.Hand).getFirstSpellAbility();

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertSame(bolt, sa.getTargets().getFirstTargetedSpell());

        AssertJUnit.assertNotNull(ai.getController().chooseNewTargetsFor(bolt, null, false));
        AssertJUnit.assertEquals(bears, bolt.getTargetCard());
    }

    // three points moved from one face to the other aren't worth a card
    @Test
    public void testDeflectionNotForAFewPoints() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Island", 4);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        cast(game, opp, "Lightning Bolt", ai);
        SpellAbility sa = addCardToZone("Deflection", ai, ZoneType.Hand).getFirstSpellAbility();

        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Rebound: "Change the target of target spell that targets only a player. The new target must be a player."
    @Test
    public void testReboundTurnsALethalLavaAxe() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Island", 2);
        ai.setLife(5, null);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        SpellAbility axe = cast(game, opp, "Lava Axe", ai);
        SpellAbility sa = addCardToZone("Rebound", ai, ZoneType.Hand).getFirstSpellAbility();

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertNotNull(ai.getController().chooseNewTargetsFor(axe, null, false));
        AssertJUnit.assertEquals(opp, axe.getTargets().getFirstTargetedPlayer());
    }

    // Meddle: a helpful spell on their creature moves to the AI's
    @Test
    public void testMeddleStealsAGiantGrowth() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Island", 2);
        Card angel = addCard("Serra Angel", ai);
        Card bears = addCard("Grizzly Bears", opp);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        SpellAbility growth = cast(game, opp, "Giant Growth", bears);
        SpellAbility sa = addCardToZone("Meddle", ai, ZoneType.Hand).getFirstSpellAbility();

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertNotNull(ai.getController().chooseNewTargetsFor(growth, null, false));
        AssertJUnit.assertEquals(angel, growth.getTargetCard());
    }

    // "You may choose new targets" (Redirect, Boltbender): a spell already pointed the AI's way keeps its target
    @Test
    public void testNoChangeForTheWorse() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card giant = addCard("Hill Giant", ai);
        Card bears = addCard("Grizzly Bears", opp);
        SpellAbility bolt = cast(game, ai, "Lightning Bolt", bears);
        SpellAbility growth = cast(game, ai, "Giant Growth", giant);

        AssertJUnit.assertNull(ai.getController().chooseNewTargetsFor(bolt, null, false));
        AssertJUnit.assertNull(ai.getController().chooseNewTargetsFor(growth, null, false));
        AssertJUnit.assertEquals(bears, bolt.getTargetCard());
        AssertJUnit.assertEquals(giant, growth.getTargetCard());
    }

    // Goblin Flectomancer sacrifices itself: not for three points of face damage, but to save a creature
    @Test
    public void testFlectomancerOnlyForSomethingReal() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card flecto = addCard("Goblin Flectomancer", ai);
        Card giant = addCard("Hill Giant", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        SpellAbility sa = null;
        for (SpellAbility s : flecto.getSpellAbilities()) {
            if (s.isActivatedAbility()) {
                sa = s;
            }
        }

        cast(game, opp, "Lightning Bolt", ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        game.getStack().clear();
        cast(game, opp, "Lightning Bolt", giant);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }
}
