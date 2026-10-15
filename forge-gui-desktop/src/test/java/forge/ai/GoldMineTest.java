package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardFactory;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Gold Mine (CMB1 #116), checked against Scryfall and its 3 rulings 2026-10-15: legacy - marking one of its five
 * nodes is permanent on the physical card (kept in the deck file), and a copy without nodes of its own can't pay it.
 */
public class GoldMineTest extends AITest {

    private static SpellAbility markAbility(Card mine) {
        return mine.getManaAbilities().stream().filter(sa -> sa.getSubAbility() != null).findFirst().get();
    }

    @Test
    public void marksANodeForAnyColorUntilAllFive() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card mine = addCard("Gold Mine", ai);
        AssertJUnit.assertEquals(5, mine.getNodeCount());
        SpellAbility mark = markAbility(mine);
        mark.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(mark.canPlay());

        AbilityUtils.resolve(mark);
        AssertJUnit.assertEquals(1, mine.getMarkedNodes());
        AssertJUnit.assertEquals("1 of 5", mine.getView().getNodes());

        mine.setTapped(false);
        mine.setMarkedNodes(5);
        AssertJUnit.assertFalse("no unmarked node left", mark.canPlay());
        SpellAbility colorless = mine.getManaAbilities().stream().filter(sa -> sa.getSubAbility() == null).findFirst().get();
        colorless.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(colorless.canPlay());
    }

    @Test
    public void marksStayOnThePhysicalCard() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        PaperCard pc = FModel.getMagicDb().getCommonCards().getCard("Gold Mine");
        ai.getRegisteredPlayer().getDeck().getMain().add(pc);
        Card mine = addCard("Gold Mine", ai);
        mine.setPaperCard(pc);

        SpellAbility mark = markAbility(mine);
        mark.setActivatingPlayer(ai);
        AbilityUtils.resolve(mark);
        PaperCard written = (PaperCard) mine.getPaperCard();
        AssertJUnit.assertEquals(1, written.getMarkedNodes());
        AssertJUnit.assertEquals("the deck's copy is the marked one", 1,
                ai.getRegisteredPlayer().getDeck().getMain().count(written));
        AssertJUnit.assertEquals(1, Card.fromPaperCard(written, ai).getMarkedNodes());
        AssertJUnit.assertEquals(pc.copyWithMarkedNodes(1), written);
    }

    @Test
    public void aCopyWithoutNodesCantMarkOne() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card mine = addCard("Gold Mine", ai);
        Card vesuva = addCard("Vesuva", ai);
        vesuva.addCloneState(CardFactory.getCloneStates(mine, vesuva, mine.getFirstSpellAbility()), game.getNextTimestamp());
        AssertJUnit.assertEquals("Gold Mine", vesuva.getName());
        AssertJUnit.assertEquals(0, vesuva.getNodeCount());
        SpellAbility mark = markAbility(vesuva);
        mark.setActivatingPlayer(ai);
        AssertJUnit.assertFalse(mark.canPlay());
    }
}
