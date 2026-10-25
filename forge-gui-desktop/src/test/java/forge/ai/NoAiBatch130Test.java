package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerController.BinaryChoiceType;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Final pass, batch 130: LastCardAi and the LockEffect untap preference. */
public class NoAiBatch130Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    private static SpellAbility ability(Card c) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility() && !sa.isManaAbility()) {
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

    // Entrancing Lyre: once it has locked their Serra Angel, it stays tapped through its untap step
    @Test
    public void testEntrancingLyreStaysTapped() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Plains", 5);
        addCard("Serra Angel", opp);
        Card lyre = addCard("Entrancing Lyre", ai);
        SpellAbility sa = ability(lyre);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        // the {T} cost taps it before the lock begins
        lyre.setTapped(true);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertFalse(ai.getController().chooseBinary(new SpellAbility.EmptySa(lyre, ai), "", BinaryChoiceType.UntapOrLeaveTapped, true));
    }

    // Dazzling Reflection: their attacking Craw Wurm's 6 prevented, and 6 life
    @Test
    public void testDazzlingReflectionOnTheirAttacker() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card wurm = addCard("Craw Wurm", opp);
        SpellAbility sa = addCardToZone("Dazzling Reflection", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, opp, false, 1);
        Combat combat = new Combat(opp);
        combat.addAttacker(wurm, ai);
        combat.setBlocked(wurm, false);
        game.getPhaseHandler().setCombat(combat);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(wurm, sa.getTargetCard());
    }

    // Covetous Elegy: they keep two of their five creatures, it keeps both of its own
    @Test
    public void testCovetousElegyWhenTheyLoseMore() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Serra Angel", ai);
        addCard("Grizzly Bears", ai);
        for (int i = 0; i < 5; i++) {
            addCard("Hill Giant", opp);
        }
        SpellAbility sa = addCardToZone("Covetous Elegy", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Destined Confrontation: their three Hill Giants (9 power) down to one; its Bears and Elves stay
    @Test
    public void testDestinedConfrontation() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Grizzly Bears", ai);
        addCard("Llanowar Elves", ai);
        for (int i = 0; i < 3; i++) {
            addCard("Hill Giant", opp);
        }
        SpellAbility sa = addCardToZone("Destined Confrontation", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Dark Triumph: two unblocked Bears and 8 life to go is lethal at +2/+0 each
    @Test
    public void testDarkTriumphForLethal() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        opp.setLife(8, null);
        SpellAbility sa = addCardToZone("Dark Triumph", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai, false, 1);
        Combat combat = new Combat(ai);
        for (int i = 0; i < 2; i++) {
            Card b = addCard("Grizzly Bears", ai);
            combat.addAttacker(b, opp);
            combat.setBlocked(b, false);
        }
        game.getPhaseHandler().setCombat(combat);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Balthor the Defiled: its graveyard's black creatures outweigh theirs
    @Test
    public void testBalthorRaisesItsOwn() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        lands(ai, "Swamp", 3);
        addCardToZone("Sengir Vampire", ai, ZoneType.Graveyard);
        addCardToZone("Hypnotic Specter", ai, ZoneType.Graveyard);
        SpellAbility sa = ability(addCard("Balthor the Defiled", ai));
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Lightning Volley: at their end step, three untapped creatures to ping with
    @Test
    public void testLightningVolleyAtTheirEnd() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 3; i++) {
            addCard("Grizzly Bears", ai).setSickness(false);
        }
        SpellAbility sa = addCardToZone("Lightning Volley", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }
}
