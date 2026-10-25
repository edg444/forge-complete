package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.ability.AttachmentMoveAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** AttachmentMoveAi: AILogic$ MoveAttachment and Licid. */
public class NoAiAttachmentMoveTest extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    private static SpellAbility activated(Card c) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility() && !sa.isManaAbility()) {
                return sa;
            }
        }
        return null;
    }

    private Card ready(String name, Player p) {
        Card c = addCard(name, p);
        c.setSickness(false);
        return c;
    }

    private void lands(Player p, String name, int n) {
        for (int i = 0; i < n; i++) {
            addCard(name, p);
        }
    }

    // Brass Squire: "{T}: Attach target Equipment you control to target creature you control."
    @Test
    public void testBrassSquireEquipsForFree() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card squire = ready("Brass Squire", ai);
        Card bonesplitter = addCard("Bonesplitter", ai);
        Card bears = ready("Grizzly Bears", ai);
        SpellAbility sa = activated(squire);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(bonesplitter, sa.getTargetCard());
        AssertJUnit.assertTrue(bears.equals(sa.getSubAbility().getTargetCard()) || squire.equals(sa.getSubAbility().getTargetCard()));
    }

    // Crown of the Ages: their Pacifism off its Serra Angel and onto their Hill Giant
    @Test
    public void testCrownOfTheAgesMovesTheirCurse() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Plains", 4);
        Card crown = addCard("Crown of the Ages", ai);
        Card angel = addCard("Serra Angel", ai);
        Card giant = addCard("Hill Giant", opp);
        Card pacifism = addCard("Pacifism", opp);
        pacifism.attachToEntity(angel, null);
        SpellAbility sa = activated(crown);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(pacifism, sa.getTargetCard());
        AssertJUnit.assertEquals(giant, AttachmentMoveAi.destination(crown));
    }

    // Kjeldoran Pride: "Attach this Aura to target creature other than enchanted creature." - away from removal, not just
    // for the sake of moving
    @Test
    public void testKjeldoranPrideDodgesRemoval() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Island", 3);
        Card bears = addCard("Grizzly Bears", ai);
        Card other = addCard("Grizzly Bears", ai);
        Card pride = addCard("Kjeldoran Pride", ai);
        pride.attachToEntity(bears, null);
        SpellAbility sa = activated(pride);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        SpellAbility bolt = addCardToZone("Doom Blade", opp, ZoneType.Hand).getFirstSpellAbility();
        bolt.setActivatingPlayer(opp);
        bolt.getTargets().add(bears);
        game.getStack().add(bolt);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(other, sa.getTargetCard());
    }

    // Dominating Licid: becomes an Aura with "You control enchanted creature" - worth its own 1/1 body for their Angel
    @Test
    public void testDominatingLicidSteals() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Island", 3);
        Card licid = ready("Dominating Licid", ai);
        Card angel = addCard("Serra Angel", opp);
        SpellAbility sa = activated(licid);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(angel, sa.getSubAbility().getTargetCard());

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
    }
}
