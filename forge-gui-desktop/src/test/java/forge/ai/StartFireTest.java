package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.CardStateName;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/**
 * P-Start // P-Fire (CMB1 #101), checked against Scryfall 2026-10-15 (no rulings). Start and Fire clash with the
 * faces of Start // Finish and Fire // Ice, so both faces take the P- prefix.
 */
public class StartFireTest extends AITest {

    private static final String NAME = "P-Start // P-Fire";

    @Test
    public void namesDontShadowRealCards() {
        initAndCreateGame();
        AssertJUnit.assertNotNull(FModel.getMagicDb().getCommonCards().getCard(NAME));
        AssertJUnit.assertEquals("Start // Finish", FModel.getMagicDb().getCommonCards().getCard("Start").getName());
        AssertJUnit.assertEquals("Fire // Ice", FModel.getMagicDb().getCommonCards().getCard("Fire").getName());
    }

    @Test
    public void bothHalves() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card card = addCardToZone(NAME, ai, ZoneType.Hand);
        AssertJUnit.assertEquals(5, card.getCMC());

        SpellAbility start = card.getState(CardStateName.LeftSplit).getFirstAbility();
        start.setActivatingPlayer(ai);
        AbilityUtils.resolve(start);
        List<Card> warriors = ai.getCreaturesInPlay().stream().filter(c -> c.getType().hasCreatureType("Warrior")).toList();
        AssertJUnit.assertEquals(2, warriors.size());
        AssertJUnit.assertTrue(warriors.get(0).hasKeyword(Keyword.VIGILANCE));
        AssertJUnit.assertTrue(warriors.get(0).getColor().hasWhite());

        Card elf = addCard("Llanowar Elves", opp);
        SpellAbility fire = card.getState(CardStateName.RightSplit).getFirstAbility();
        fire.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(card.getState(CardStateName.RightSplit).getType().isInstant());
        fire.getTargets().add(elf);
        fire.addDividedAllocation(elf, 1);
        fire.getTargets().add(opp);
        fire.addDividedAllocation(opp, 1);
        int life = opp.getLife();
        AbilityUtils.resolve(fire);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(elf.isInZone(ZoneType.Graveyard));
        AssertJUnit.assertEquals(life - 1, opp.getLife());
    }
}
