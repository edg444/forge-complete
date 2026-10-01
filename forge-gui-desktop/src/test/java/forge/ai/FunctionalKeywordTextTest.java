package forge.ai;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.deck.Deck;
import forge.game.Game;
import forge.game.Match;
import forge.game.ability.effects.RollDiceEffect;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Sentence keywords the engine finds by their exact K: line text, which the in-game text retemplating had
 * rewritten to Oracle wording (2026-09-30). Each must still work, and still read like Oracle in game.
 */
public class FunctionalKeywordTextTest extends AITest {

    private static String text(Card c) {
        return c.getAbilityText();
    }

    @Test
    public void testAmberPrisonMayStayTapped() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card prison = addCard("Amber Prison", me);
        AssertJUnit.assertTrue(prison.hasKeyword("You may choose not to untap CARDNAME during your untap step."));
        AssertJUnit.assertTrue(text(prison), text(prison).contains("You may choose not to untap this artifact during your untap step."));
    }

    @Test
    public void testLegendaryNicknameIsKept() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card hivis = addCard("Hivis of the Scale", me);
        AssertJUnit.assertTrue(hivis.hasKeyword("You may choose not to untap CARDNAME during your untap step."));
        AssertJUnit.assertTrue(text(hivis), text(hivis).contains("You may choose not to untap Hivis during your untap step."));
    }

    @Test
    public void testLureMakesEveryAbleBlockerBlockIt() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card boar = addCard("Nessian Boar", me);
        Card bear = addCard("Grizzly Bears", opp);
        boar.setSickness(false);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, me);
        Combat combat = new Combat(me);
        combat.addAttacker(boar, opp);
        game.getPhaseHandler().setCombat(combat);
        AssertJUnit.assertTrue(CombatUtil.mustBlockAnAttacker(bear, combat, null));
        AssertJUnit.assertTrue(text(boar), text(boar).contains("All creatures able to block this creature do so."));
    }

    @Test
    public void testMustBeBlockedIfAble() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card stalker = addCard("Canopy Stalker", me);
        AssertJUnit.assertTrue(stalker.hasKeyword("CARDNAME must be blocked if able."));
        AssertJUnit.assertTrue(text(stalker), text(stalker).contains("This creature must be blocked if able."));
    }

    @Test
    public void testAnteCardIsRemovedFromTheDeck() throws Exception {
        initAndCreateGame();
        PaperCard contract = FModel.getMagicDb().getCommonCards().getCard("Contract from Below");
        Deck deck = new Deck();
        deck.getMain().add(contract, 1);
        deck.getMain().add(FModel.getMagicDb().getCommonCards().getCard("Grizzly Bears"), 1);
        Method removed = Match.class.getDeclaredMethod("getRemovedAnteCards", Deck.class);
        removed.setAccessible(true);
        AssertJUnit.assertEquals(Set.of(contract), removed.invoke(null, deck));
    }

    @Test
    public void testAnteSorceryReadsLikeOracle() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card contract = addCardToZone("Contract from Below", me, ZoneType.Hand);
        AssertJUnit.assertTrue(text(contract), text(contract).contains(
                "Remove this card from your deck before playing if you're not playing for ante."));
    }

    @Test
    public void testXenosquirrelsCanModifyARoll() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card squirrels = addCard("Xenosquirrels", me);
        squirrels.addCounterInternal(CounterEnumType.P1P1, 1, me, false, null, null);
        List<Card> modifiers = RollDiceEffect.getIncrementCards(me,
                "After you roll a die, you may remove a +1/+1 counter from Xenosquirrels. If you do, increase or decrease the result by 1.",
                "After you roll a die, you may pay 1 life. If you do, increase or decrease the result by 1. Do this only once each turn.");
        AssertJUnit.assertTrue(modifiers.contains(squirrels));
        AssertJUnit.assertTrue(text(squirrels), text(squirrels).contains(
                "After you roll a die, you may remove a +1/+1 counter from this creature. If you do, increase or decrease the result by 1."));
    }

    @Test
    public void testStartYourEnginesGivesSpeed() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card bashtronaut = addCard("Burnout Bashtronaut", me);
        AssertJUnit.assertTrue(bashtronaut.hasKeyword(Keyword.START_YOUR_ENGINES));
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(1, me.getSpeed());
        AssertJUnit.assertTrue(text(bashtronaut), text(bashtronaut).contains("Start your engines!"));
    }

    @Test
    public void testCollectiveBrutalityEscalatesByDiscarding() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card brutality = addCardToZone("Collective Brutality", me, ZoneType.Hand);
        AssertJUnit.assertTrue(brutality.hasKeyword(Keyword.ESCALATE));
        AssertJUnit.assertTrue(text(brutality), text(brutality).contains("Escalate—Discard a card."));
    }
}
