package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.ability.NumberGuessAi;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Final pass, batch 128: MiscCardAi and Wrath of the Skies. */
public class NoAiBatch128Test extends AITest {

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

    // Wistful Thinking: at their hand of three
    @Test
    public void testWistfulThinkingOnTheirHand() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 3; i++) {
            addCardToZone("Hill Giant", opp, ZoneType.Hand);
        }
        SpellAbility sa = addCardToZone("Wistful Thinking", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(opp, sa.getTargets().getFirstTargetedPlayer());
    }

    // Confiscation Coup: their 4-drop Hill Giant within the four energy
    @Test
    public void testConfiscationCoupStealsWithinItsEnergy() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card giant = addCard("Hill Giant", opp);
        addCard("Craw Wurm", opp);
        SpellAbility sa = addCardToZone("Confiscation Coup", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(giant, sa.getSubAbility().getTargetCard());
    }

    // HELIOS One: five energy destroys their 5-drop Serra Angel
    @Test
    public void testHeliosOneDestroysByEnergy() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Plains", 4);
        Card angel = addCard("Serra Angel", opp);
        ai.addCounterInternal(CounterEnumType.ENERGY, 5, ai, false, null, null);
        SpellAbility sa = ability(addCard("HELIOS One", ai), ApiType.Destroy);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(angel, sa.getTargetCard());
        AssertJUnit.assertEquals(Integer.valueOf(5), sa.getXManaCostPaid());
    }

    // Wrath of the Skies: paying 2 wipes their two Bears and leaves its Serra Angel
    @Test
    public void testWrathOfTheSkiesPaysForTheirs() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Plains", 6);
        addCard("Serra Angel", ai);
        addCard("Grizzly Bears", opp);
        addCard("Grizzly Bears", opp);
        SpellAbility sa = addCardToZone("Wrath of the Skies", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(Integer.valueOf(2), NumberGuessAi.plannedNumber(sa));
    }

    // Safe Haven: its Serra Angel exiled ahead of their Doom Blade
    @Test
    public void testSafeHavenDodgesRemoval() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Plains", 2);
        Card angel = addCard("Serra Angel", ai);
        SpellAbility sa = ability(addCard("Safe Haven", ai), ApiType.ChangeZone);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        SpellAbility blade = addCardToZone("Doom Blade", opp, ZoneType.Hand).getFirstSpellAbility();
        blade.setActivatingPlayer(opp);
        blade.getTargets().add(angel);
        game.getStack().add(blade);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(angel, sa.getTargetCard());
    }

    // Juxtapose: their Craw Wurm for its Grizzly Bears
    @Test
    public void testJuxtaposeTradesUp() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Grizzly Bears", ai);
        addCard("Craw Wurm", opp);
        SpellAbility sa = addCardToZone("Juxtapose", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Soul's Might: its Craw Wurm doubles to 12 power
    @Test
    public void testSoulsMightOnItsBiggest() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card wurm = addCard("Craw Wurm", ai);
        addCard("Grizzly Bears", ai);
        SpellAbility sa = addCardToZone("Soul's Might", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(wurm, sa.getTargetCard());
    }
}
