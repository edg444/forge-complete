package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Command From the Shadows, Advanced Tactics and The Flavormancer (UNK), checked against Scryfall 2026-10-22 (no
 * rulings).
 */
public class UnknownEventBatch58Test extends AITest {

    private static void runPreOpeningHandActions(final Game game, final Player p) throws Exception {
        final java.lang.reflect.Method pre = game.getAction().getClass().getDeclaredMethod("runPreOpeningHandActions", Player.class);
        pre.setAccessible(true);
        pre.invoke(game.getAction(), p);
    }

    @Test
    public void testSecretCommanders() throws Exception {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        // conspiracies are variant cards
        final Card conspiracy = Card.fromPaperCard(forge.StaticData.instance().getVariantCards().getCard("Command From the Shadows"), human);
        human.getZone(ZoneType.Command).add(conspiracy);
        final Card mine = addCardToZone("Grizzly Bears", human, ZoneType.Command);
        human.addCommander(mine);
        final Card theirs = addCardToZone("Hill Giant", ai, ZoneType.Command);
        ai.addCommander(theirs);
        runPreOpeningHandActions(game, human);
        AssertJUnit.assertTrue(mine.isSecretCommander());
        AssertJUnit.assertTrue("every player's commanders", theirs.isSecretCommander());
        AssertJUnit.assertFalse(ai.allCommandersRevealed());
        AssertJUnit.assertFalse("hidden from opponents", mine.getView().canBeShownTo(ai.getView()));
        AssertJUnit.assertTrue("not from its controller", mine.getView().canBeShownTo(human.getView()));
        AssertJUnit.assertFalse("it's still cast normally", mine.getSpellAbilities().isEmpty());
        ai.incCommanderCast(theirs);
        AssertJUnit.assertFalse(theirs.isSecretCommander());
        AssertJUnit.assertTrue(ai.allCommandersRevealed());
    }

    private static forge.game.spellability.SpellAbility svar(final Card c, final String name, final Player p) {
        final forge.game.spellability.SpellAbility sa = forge.game.ability.AbilityFactory.getAbility(c.getSVar(name), c);
        sa.setActivatingPlayer(p);
        return sa;
    }

    @Test
    public void testAdvancedTactics() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card attacker = addCard("Grizzly Bears", ai);
        final Card next = addCard("Hill Giant", human);
        final Card diagonal = addCard("Craw Wurm", human);
        final Card tactics = addCard("Advanced Tactics", ai);
        game.getAction().checkStateEffects(true);
        forge.game.ability.AbilityUtils.resolve(svar(tactics, "TrigGrid", ai));
        final int[] grid = tactics.getTacticsGrid();
        int filled = 0;
        for (int i = 0; i < grid.length; i++) {
            if (grid[i] != 0) {
                filled++;
                AssertJUnit.assertEquals("top left first, every other space", 0, i % 2);
            }
        }
        AssertJUnit.assertEquals(3, filled);

        java.util.Arrays.fill(grid, 0);
        grid[0] = attacker.getId();
        grid[1] = next.getId();
        grid[6] = diagonal.getId();
        tactics.setTacticsGrid(grid);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertTrue(forge.game.combat.CombatUtil.canBlock(attacker, next));
        AssertJUnit.assertFalse("diagonal isn't orthogonally adjacent", forge.game.combat.CombatUtil.canBlock(attacker, diagonal));

        final Card newcomer = addCardToZone("Runeclaw Bear", ai, ZoneType.Hand);
        final Card entered = game.getAction().moveToPlay(newcomer, ai, null, forge.game.ability.AbilityKey.newMap());
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue("placed as it enters", forge.game.ability.effects.TacticsGridEffect.cellOf(tactics, entered) >= 0);

        final forge.game.spellability.SpellAbility move = tactics.getSpellAbilities().stream()
                .filter(s -> "Move".equals(s.getParam("Mode"))).findFirst().orElseThrow();
        move.setActivatingPlayer(human);
        move.getTargets().add(diagonal);
        final int before = forge.game.ability.effects.TacticsGridEffect.cellOf(tactics, diagonal);
        forge.game.ability.AbilityUtils.resolve(move);
        final int after = forge.game.ability.effects.TacticsGridEffect.cellOf(tactics, diagonal);
        AssertJUnit.assertEquals("one space", 1, Math.abs(before / 5 - after / 5) + Math.abs(before % 5 - after % 5));

        game.getAction().moveToGraveyard(tactics, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertNull("the grid is gone", game.getCardState(tactics).getTacticsGrid());
        AssertJUnit.assertTrue(forge.game.combat.CombatUtil.canBlock(attacker, diagonal));
    }

    @Test
    public void testFlavormancer() throws Exception {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card flavormancer = addCardToZone("The Flavormancer", ai, ZoneType.Command);
        ai.addCommander(flavormancer);
        final Card gideon = addCardToZone("Gideon Jura", ai, ZoneType.Library);
        final Card yawgmoth = addCardToZone("Yawgmoth, Thran Physician", ai, ZoneType.Library);
        final Card isamaru = addCardToZone("Isamaru, Hound of Konda", ai, ZoneType.Library);
        runPreOpeningHandActions(game, ai);
        AssertJUnit.assertTrue("planeswalkers get creature types this way", gideon.getType().hasStringType("Hero"));
        AssertJUnit.assertTrue(yawgmoth.getType().hasStringType("Villain"));
        AssertJUnit.assertFalse(isamaru.getType().hasStringType("Hero") || isamaru.getType().hasStringType("Villain"));
        final Card moved = game.getAction().moveToHand(gideon, null);
        AssertJUnit.assertTrue("perpetual", moved.getType().hasStringType("Hero"));

        final Card inPlay = game.getAction().moveToPlay(game.getCardState(flavormancer), ai, null, forge.game.ability.AbilityKey.newMap());
        game.getPhaseHandler().devModeSet(forge.game.phase.PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(inPlay.isInPlay());
        AssertJUnit.assertFalse("castable for free from hand", game.getCardState(moved).mayPlay(ai).isEmpty());
    }
}
