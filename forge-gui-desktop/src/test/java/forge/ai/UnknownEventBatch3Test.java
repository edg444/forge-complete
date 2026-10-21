package forge.ai;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Reverse Ninja, Demon Detective, Syr Konrad's Squire, Fear (split card) // Loathing, May of the Machine and Shedding
 * Snake (UNK CB02-CB05), checked against Scryfall 2026-10-21 (no rulings).
 */
public class UnknownEventBatch3Test extends AITest {

    @Test
    public void testReverseNinjaBlocksTheSameCreature() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card attacker = addCard("Hill Giant", human);
        final Card blocker = addCard("Grizzly Bears", ai);
        addCard("Swamp", ai);
        final Card ninja = addCardToZone("Reverse Ninja", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, human);
        final Combat combat = new Combat(human);
        combat.addAttacker(attacker, ai);
        combat.addBlocker(attacker, blocker);
        game.getPhaseHandler().setCombat(combat);

        final SpellAbility ustujnin = ninja.getSpellAbilities().stream().filter(sa -> sa.hasParam("Ustujnin"))
                .findFirst().orElseThrow();
        ustujnin.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(ustujnin.canPlay());
        AssertJUnit.assertTrue(ComputerUtil.playNoStack(ai, ustujnin, game, true));
        final Card onField = game.getCardState(ninja);
        AssertJUnit.assertTrue(onField.isInPlay());
        AssertJUnit.assertTrue("the blocker went back to hand", game.getCardState(blocker).isInZone(ZoneType.Hand));
        AssertJUnit.assertTrue("blocking the same creature", game.getCombat().isBlocking(onField, attacker));
    }

    @Test
    public void testDemonDetectiveWithoutCommanders() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        AssertJUnit.assertTrue("no commanders: all of them have been revealed", ai.allCommandersRevealed());
        addCard("Swamp", ai);
        addCard("Swamp", ai);
        final Card demon = addCardToZone("Demon Detective", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final SpellAbility cast = demon.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(cast.canPlay());
        AssertJUnit.assertTrue(demon.hasKeyword(Keyword.FLYING));
    }

    @Test
    public void testSyrKonradsSquire() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card squire = addCardToZone("Syr Konrad's Squire", ai, ZoneType.Hand);
        final int life = human.getLife();
        game.getAction().moveToGraveyard(squire, null);
        resolveAll(game);
        AssertJUnit.assertEquals("put into a graveyard from hand", life - 1, human.getLife());
        game.getAction().exile(game.getCardState(squire), null, null);
        resolveAll(game);
        AssertJUnit.assertEquals("left your graveyard and was put into exile: two triggers", life - 3, human.getLife());
        game.getAction().moveToLibrary(game.getCardState(squire), null);
        resolveAll(game);
        AssertJUnit.assertEquals("put into your library", life - 4, human.getLife());
    }

    @Test
    public void testFearLoathing() {
        final Game game = initAndCreateGame();
        final Card split = addCardToZone("Fear (split card) // Loathing", game.getPlayers().get(1), ZoneType.Hand);
        AssertJUnit.assertTrue(split.isSplitCard());
        AssertJUnit.assertTrue(split.hasKeyword(Keyword.FUSE));
    }

    @Test
    public void testMayOfTheMachineDates() {
        final Game game = initAndCreateGame();
        final Card may = addCardToZone("May of the Machine", game.getPlayers().get(1), ZoneType.Hand);
        final java.util.function.Supplier<ZonedDateTime> real = AbilityUtils.realWorldNow;
        try {
            AbilityUtils.realWorldNow = () -> ZonedDateTime.of(2026, 5, 6, 12, 0, 0, 0, ZoneId.systemDefault());
            AssertJUnit.assertEquals(506, AbilityUtils.calculateAmount(may, "Today", may.getFirstSpellAbility()));
            AbilityUtils.realWorldNow = () -> ZonedDateTime.of(2026, 12, 25, 12, 0, 0, 0, ZoneId.systemDefault());
            AssertJUnit.assertEquals(1225, AbilityUtils.calculateAmount(may, "Today", may.getFirstSpellAbility()));
        } finally {
            AbilityUtils.realWorldNow = real;
        }
    }

    @Test
    public void testSheddingSnakeResetsCommanderTax() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card commander = addCardToZone("Grizzly Bears", ai, ZoneType.Command);
        ai.incCommanderCast(commander);
        ai.incCommanderCast(commander);
        AssertJUnit.assertEquals(4, ai.getCommanderTax(commander));
        final Card snake = addCard("Shedding Snake", ai);
        final SpellAbility etb = snake.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        final int life = ai.getLife();
        AbilityUtils.resolve(etb);
        AssertJUnit.assertEquals("paid 2 life", life - 2, ai.getLife());
        AssertJUnit.assertEquals("tax back to zero", 0, ai.getCommanderTax(commander));
        AssertJUnit.assertEquals("cast count kept", 2, ai.getCommanderCast(commander));
        ai.incCommanderCast(commander);
        AssertJUnit.assertEquals(2, ai.getCommanderTax(commander));
    }

    private void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }
}
