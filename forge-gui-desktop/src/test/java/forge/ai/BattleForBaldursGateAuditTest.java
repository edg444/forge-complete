package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Commander Legends: Battle for Baldur's Gate review (2026-10-23). */
public class BattleForBaldursGateAuditTest extends AITest {

    @Test
    public void testKenkuArtificerTargetsAnyNoncreatureArtifact() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card kenku = addCard("Kenku Artificer", p);
        Card rock = addCard("Mind Stone", opp);
        Trigger t = kenku.getTriggers().iterator().next();
        t.ensureAbility().setActivatingPlayer(p);
        AssertJUnit.assertTrue(t.ensureAbility().canTarget(rock));
    }

    @Test
    public void testOverwhelmingEncounterTextMatchesItsCounters() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        String text = addCardToZone("Overwhelming Encounter", p, ZoneType.Hand).getAbilityText();
        AssertJUnit.assertTrue(text, text.contains("10—19 | Put two +1/+1 counters on each creature you control."));
        AssertJUnit.assertTrue(text, text.contains("20 | Put four +1/+1 counters on each creature you control."));
    }

    @Test
    public void testPsionicRitualReplicateText() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        String text = addCardToZone("Psionic Ritual", p, ZoneType.Hand).getAbilityText();
        AssertJUnit.assertTrue(text, text.contains("Replicate—Tap an untapped Horror you control."));
    }

    private String activeGrant(final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Veteran Soldier", "CLB"), p);
        p.getZone(ZoneType.Battlefield).add(c);
        game.getAction().checkStateEffects(true);
        String found = null;
        for (StaticAbility st : c.getStaticAbilities()) {
            if (st.hasParam("AddTrigger") && st.checkConditions()) {
                AssertJUnit.assertNull("only one version should be active", found);
                found = st.getParam("AddTrigger");
            }
        }
        AssertJUnit.assertNotNull(found);
        return found;
    }

    @Test
    public void testVeteranSoldierPrintedTokensAttackThatPlayer() {
        AssertJUnit.assertEquals("AttacksPlayer", activeGrant(false));
        AssertJUnit.assertEquals("AttacksPlayerPrinted", activeGrant(true));
    }
}
