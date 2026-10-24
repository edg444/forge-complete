package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** AI:RemoveDeck:All cards, batch 36 (I-J): each test is the situation that card's new AI is for. */
public class NoAiBatch36Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa);
    }

    private static SpellAbility activated(Card c) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility()) {
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

    private void cards(Player p, String name, ZoneType zone, int n) {
        for (int i = 0; i < n; i++) {
            addCardToZone(name, p, zone);
        }
    }

    // Windfall: "Each player discards their hand, then draws cards equal to the greatest number of cards a player discarded this way."
    @Test
    public void testWindfallOnlyWhenTheAiComesOutAhead() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Island", 3);
        cards(ai, "Island", ZoneType.Library, 10);
        cards(opp, "Island", ZoneType.Hand, 4);
        SpellAbility sa = addCardToZone("Windfall", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        cards(ai, "Hill Giant", ZoneType.Hand, 4);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Island of Wak-Wak: "{T}: The power of target creature with flying becomes 0 until end of turn."
    @Test
    public void testWakWakBlanksAFlyingAttacker() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        SpellAbility sa = activated(addCard("Island of Wak-Wak", ai));
        Card angel = addCard("Serra Angel", opp);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, opp);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, opp, false, 1);
        Combat combat = new Combat(opp);
        combat.addAttacker(angel, ai);
        game.getPhaseHandler().setCombat(combat);
        sa.resetTargets();
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(angel, sa.getTargetCard());
    }

    // Intimidation Bolt: "... Other creatures can't attack this turn."
    @Test
    public void testIntimidationBoltNotBeforeOwnAttack() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Plateau", 3);
        addCard("Hill Giant", ai).setSickness(false);
        Card bears = addCard("Grizzly Bears", opp);
        bears.setSickness(false);
        SpellAbility sa = addCardToZone("Intimidation Bolt", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertEquals(AiPlayDecision.AnotherTime, SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision());

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, opp, false, 1);
        Combat combat = new Combat(opp);
        combat.addAttacker(bears, ai);
        game.getPhaseHandler().setCombat(combat);
        sa.resetTargets();
        AssertJUnit.assertNotSame(AiPlayDecision.AnotherTime, SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision());
    }

    // Jalira, Master Polymorphist: "{2}{U}, {T}, Sacrifice another creature: Reveal cards ... until a nonlegendary creature ..."
    @Test
    public void testJaliraOnlySacrificesSpareFodder() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Island", 3);
        cards(ai, "Hill Giant", ZoneType.Library, 5);
        Card jalira = addCard("Jalira, Master Polymorphist", ai);
        jalira.setSickness(false);
        Card bears = addCard("Grizzly Bears", ai);
        SpellAbility sa = activated(jalira);
        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
        bears.setSVar("SacMe", "5");
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }
}
