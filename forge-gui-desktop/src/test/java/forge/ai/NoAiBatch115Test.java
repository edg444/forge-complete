package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

/** Second pass, batch 115: exploit (Profaner of the Dead, Silumgar Sorcerer). */
public class NoAiBatch115Test extends AITest {

    private static CardCollectionView exploits(Player ai, Card c) {
        for (Trigger t : c.getTriggers()) {
            if (t.getMode() == TriggerType.ChangesZone && t.ensureAbility() != null && t.ensureAbility().isKeyword(Keyword.EXPLOIT)) {
                SpellAbility sac = t.ensureAbility();
                sac.setActivatingPlayer(ai);
                return ai.getController().choosePermanentsToSacrifice(sac, 0, 1, new CardCollection(ai.getCreaturesInPlay()), "creature");
            }
        }
        return null;
    }

    // Profaner of the Dead: its 2-toughness Bears bounce their two Elves, but nothing of a Craw Wurm's
    @Test
    public void testProfanerExploitsOnlyForABounce() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card bears = addCard("Grizzly Bears", ai);
        addCard("Craw Wurm", opp);
        Card profaner = addCard("Profaner of the Dead", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertTrue(exploits(ai, profaner).isEmpty());

        addCard("Llanowar Elves", opp);
        addCard("Llanowar Elves", opp);
        AssertJUnit.assertEquals(bears, exploits(ai, profaner).get(0));
    }

    // Silumgar Sorcerer: flashed in against their creature spell, it exploits to counter it, and not with nothing to counter
    @Test
    public void testSilumgarSorcererExploitsToCounter() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Grizzly Bears", ai);
        Card sorcerer = addCard("Silumgar Sorcerer", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        AssertJUnit.assertTrue(exploits(ai, sorcerer).isEmpty());

        SpellAbility wurm = addCardToZone("Craw Wurm", opp, ZoneType.Hand).getFirstSpellAbility();
        wurm.setActivatingPlayer(opp);
        game.getStack().add(wurm);
        AssertJUnit.assertEquals(1, exploits(ai, sorcerer).size());
    }
}
