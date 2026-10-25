package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.ability.MiscSpellAi;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Final pass, batch 126: MiscSpellAi, ZonePickAi and group F's combat bits. */
public class NoAiBatch126Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    private static SpellAbility ability(Card c, ApiType api) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility() && !sa.isManaAbility() && (api == null || sa.getApi() == api)) {
                return sa;
            }
        }
        return null;
    }

    private void lands(Player p, String name, int n) {
        for (int i = 0; i < n; i++) {
            addCard(name, p);
        }
    }

    // Blast Zone: at their end step it charges to 2, the mana value of their two Bears; then the sweep is worth it
    @Test
    public void testBlastZoneChargesAndFires() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Plains", 4);
        addCard("Grizzly Bears", opp);
        addCard("Grizzly Bears", opp);
        Card zone = addCard("Blast Zone", ai);
        zone.addCounterInternal(CounterEnumType.CHARGE, 1, ai, false, null, null);
        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        SpellAbility charge = ability(zone, ApiType.PutCounter);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, charge));
        AssertJUnit.assertEquals(Integer.valueOf(1), charge.getXManaCostPaid());

        zone.addCounterInternal(CounterEnumType.CHARGE, 1, ai, false, null, null);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, ability(zone, ApiType.DestroyAll)));
    }

    // Mystic Confluence: three cards at their end step
    @Test
    public void testMysticConfluenceDrawsThree() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Island", 5);
        SpellAbility sa = addCardToZone("Mystic Confluence", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(3, sa.getChosenList().size());
        AssertJUnit.assertTrue(sa.getChosenList().stream().allMatch(s -> s.getApi() == ApiType.Draw));
    }

    // Root Greevil: their two green enchantments outweigh its white one
    @Test
    public void testRootGreevilNamesTheirColor() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Forest", 3);
        addCard("Overgrowth", opp);
        addCard("Rancor", opp);
        addCard("Pacifism", ai);
        SpellAbility sa = ability(addCard("Root Greevil", ai), null);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals("green", MiscSpellAi.plannedColor(sa));
    }

    // Flash: at their end step, the Serra Angel its 3 lands can pay {3} for once reduced
    @Test
    public void testFlashPicksWhatItCanPayFor() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Island", 5);
        addCardToZone("Serra Angel", ai, ZoneType.Hand);
        addCardToZone("Craw Wurm", ai, ZoneType.Hand);
        SpellAbility sa = addCardToZone("Flash", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Fluros of Myra's Marvels: the power its creature cards share most
    @Test
    public void testFlurosNamesTheCommonPower() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 4; i++) {
            addCardToZone("Hill Giant", ai, ZoneType.Library);
        }
        addCardToZone("Grizzly Bears", ai, ZoneType.Library);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card fluros = game.getAction().moveToPlay(addCardToZone("Fluros of Myra's Marvels", ai, ZoneType.Hand), null, null);
        AssertJUnit.assertEquals(Integer.valueOf(3), fluros.getChosenNumber());
    }

    // Fiery Bombardment: its spare Ash Zealot ({R}{R}) is 2 damage, enough for their Bears
    @Test
    public void testFieryBombardmentCountsRedSymbols() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Mountain", 3);
        Card bears = addCard("Grizzly Bears", opp);
        Card goblin = addCard("Ash Zealot", ai);
        goblin.setSVar("SacMe", "1");
        SpellAbility sa = ability(addCard("Fiery Bombardment", ai), null);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(bears, sa.getTargetCard());
    }

    // Aleatory: its Bears blocked by a Hill Giant trade with it at 3/3
    @Test
    public void testAleatoryOnACloseFight() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card bears = addCard("Grizzly Bears", ai);
        Card giant = addCard("Hill Giant", opp);
        SpellAbility sa = addCardToZone("Aleatory", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai, false, 1);
        Combat combat = new Combat(ai);
        combat.addAttacker(bears, opp);
        combat.addBlocker(bears, giant);
        combat.setBlocked(bears, true);
        game.getPhaseHandler().setCombat(combat);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(bears, sa.getTargetCard());
    }

    // Riptide Mangler: their Craw Wurm's 6 power for its 0
    @Test
    public void testRiptideManglerCopiesTheBiggest() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Island", 2);
        Card wurm = addCard("Craw Wurm", opp);
        SpellAbility sa = ability(addCard("Riptide Mangler", ai), null);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(wurm, sa.getTargetCard());
    }

    // Throes of Chaos: a main-phase cascade
    @Test
    public void testThroesOfChaosForTheCascade() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        SpellAbility sa = addCardToZone("Throes of Chaos", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }
}
