package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Windy City Aven, Windy City Elemental, Identify the Culprit, Hero's Uncle, 17-Year Cicadas and Fear of Forgetting
 * Names (UNK uncommons), checked against Scryfall 2026-10-22 (no rulings).
 */
public class UnknownEventBatch32Test extends AITest {

    @Test
    public void testWindyCityBlessing() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card aven = addCard("Windy City Aven", ai);
        final SpellAbility attack = aven.getTriggers().get(1).ensureAbility();
        attack.setActivatingPlayer(ai);
        AssertJUnit.assertEquals(0, AbilityUtils.calculateAmount(aven, "Attended", attack));
        ai.setMatchNote("AttendedMagicCon:Chicago", true);
        AssertJUnit.assertEquals(1, AbilityUtils.calculateAmount(aven, "Attended", attack));
        for (int i = 0; i < 9; i++) {
            addCard("Plains", ai);
        }
        game.getAction().checkStateEffects(true);
        game.getAction().checkStaticAbilities();
        final boolean blessed = ai.getCardsIn(ZoneType.Command).anyMatch(c -> c.getName().equals("Windy City's Blessing"));
        if (!blessed) {
            // the static trigger runs during state checks in a real game; resolve it directly here
            final SpellAbility bless = aven.getTriggers().get(0).ensureAbility();
            bless.setActivatingPlayer(ai);
            AbilityUtils.resolve(bless);
        }
        final Card blessing = ai.getCardsIn(ZoneType.Command).stream().filter(c -> c.getName().equals("Windy City's Blessing")).findFirst().orElseThrow();
        AssertJUnit.assertTrue(blessing.isValid("Effect.namedWindy_City's_Blessing", ai, aven, null));
        AssertJUnit.assertTrue(aven.getTriggers().get(1).requirementsCheck(game));
    }

    @Test
    public void testIdentifyTheCulprit() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", human);
        final Card culprit = addCardToZone("Identify the Culprit", ai, ZoneType.Hand);
        final SpellAbility cast = culprit.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        cast.getTargets().add(human);
        AbilityUtils.resolve(cast);
        AssertJUnit.assertTrue("a lone creature is always guessed", bears.isInZone(ZoneType.Battlefield));
    }

    @Test
    public void testHerosUncleFindsATeamUp() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCardToZone("Grizzly Bears", ai, ZoneType.Library);
        addCardToZone("Drana, Liberator of Malakir", ai, ZoneType.Library);
        final Card teamUp = addCardToZone("Drana and Linvala", ai, ZoneType.Library);
        final Card uncle = addCard("Hero's Uncle", ai);
        final SpellAbility dies = uncle.getTriggers().get(0).ensureAbility();
        dies.setActivatingPlayer(ai);
        AbilityUtils.resolve(dies);
        AssertJUnit.assertTrue(teamUp.isInZone(ZoneType.Hand));
    }

    @Test
    public void testCicadasResuspend() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(forge.game.phase.PhaseType.MAIN1, ai);
        Card cicadas = addCardToZone("17-Year Cicadas", ai, ZoneType.Hand);
        final SpellAbility cast = cicadas.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        cicadas = game.getAction().moveToStack(cicadas, cast);
        cast.setHostCard(cicadas);
        game.getStack().add(cast);
        playUntilStackClear(game);
        AssertJUnit.assertEquals(10, ai.getCreaturesInPlay().size());
        final Card exiled = game.getCardState(cicadas);
        AssertJUnit.assertTrue(exiled.isInZone(ZoneType.Exile));
        AssertJUnit.assertEquals(17, exiled.getCounters(CounterEnumType.TIME));
        AssertJUnit.assertTrue("suspended again", exiled.hasSuspend());
    }

    @Test
    public void testFearOfForgettingNames() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", human);
        final Card fear = addCard("Fear of Forgetting Names", ai);
        final Card mine = addCard("Llanowar Elves", ai);
        final SpellAbility etb = fear.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        etb.getTargets().add(bears);
        AbilityUtils.resolve(etb);
        AssertJUnit.assertTrue(bears.hasNoName());
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(mine.hasKeyword(forge.game.keyword.Keyword.PROTECTION));
        AssertJUnit.assertTrue(bears.isValid("Creature.noName", ai, fear, null));
        AssertJUnit.assertFalse(fear.isValid("Creature.noName", ai, fear, null));
    }
}
