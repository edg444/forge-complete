package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.CardSplitType;
import forge.card.CardStateName;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/**
 * P-Smelt // Herd // P-Saw (CMB1 #100), checked against Scryfall 2026-10-15 (no rulings): a three-face split card.
 * Smelt and Saw clash with real cards, so those faces take the P- prefix.
 */
public class SmeltHerdSawTest extends AITest {

    private static final String NAME = "P-Smelt // Herd // P-Saw";

    private static SpellAbility face(Card card, CardStateName state, Player p) {
        SpellAbility sa = card.getState(state).getFirstAbility();
        sa.setActivatingPlayer(p);
        return sa;
    }

    @Test
    public void namesDontShadowRealCards() {
        initAndCreateGame();
        AssertJUnit.assertEquals(3, FModel.getMagicDb().getCommonCards().getCard(NAME).getRules().getAllFaces().size());
        AssertJUnit.assertEquals(CardSplitType.None, FModel.getMagicDb().getCommonCards().getCard("Smelt").getRules().getSplitType());
        AssertJUnit.assertEquals(CardSplitType.None, FModel.getMagicDb().getCommonCards().getCard("Saw").getRules().getSplitType());
    }

    @Test
    public void allThreeFaces() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card card = addCardToZone(NAME, ai, ZoneType.Hand);
        AssertJUnit.assertEquals("{R} + {5}{G} + {1}{B}", 9, card.getCMC());
        AssertJUnit.assertTrue(card.getColor().hasRed() && card.getColor().hasGreen() && card.getColor().hasBlack());

        Card bauble = addCard("Mishra's Bauble", opp);
        SpellAbility smelt = face(card, CardStateName.LeftSplit, ai);
        smelt.getTargets().add(bauble);
        AbilityUtils.resolve(smelt);
        AssertJUnit.assertTrue(bauble.isInZone(ZoneType.Graveyard));

        AbilityUtils.resolve(face(card, CardStateName.RightSplit, ai));
        List<Card> elks = ai.getCreaturesInPlay().stream().filter(c -> c.getType().hasCreatureType("Elk")).toList();
        AssertJUnit.assertEquals(3, elks.size());
        AssertJUnit.assertEquals(2, elks.get(0).getNetPower());
        AssertJUnit.assertTrue(elks.get(0).getColor().hasGreen());

        Card bears = addCard("Grizzly Bears", opp);
        SpellAbility saw = face(card, CardStateName.Split3, ai);
        AssertJUnit.assertTrue(saw.isSpell());
        saw.getTargets().add(bears);
        AbilityUtils.resolve(saw);
        AssertJUnit.assertTrue(bears.isInZone(ZoneType.Graveyard));
        List<Card> halves = opp.getCreaturesInPlay().stream().filter(c -> c.getType().hasCreatureType("Half")).toList();
        AssertJUnit.assertEquals("its controller gets them", 2, halves.size());
        AssertJUnit.assertEquals(1, halves.get(0).getNetPower());
        AssertJUnit.assertEquals(2, halves.get(0).getNetToughness());
        AssertJUnit.assertTrue(halves.get(0).getColor().hasRed());
    }

    @Test
    public void eachFaceIsCastable() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        addCard("Grizzly Bears", opp);
        addCard("Mishra's Bauble", opp);
        Card card = addCardToZone(NAME, ai, ZoneType.Hand);
        long spells = card.getAllPossibleAbilities(ai, true).stream().filter(SpellAbility::isSpell).count();
        AssertJUnit.assertEquals(3, spells);
    }
}
