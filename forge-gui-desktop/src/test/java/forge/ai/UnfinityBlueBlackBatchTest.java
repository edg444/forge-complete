package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Super-Duper Lost (UNF #59), Treacherous Trapezist (UNF #60) and Disemvowel (UNF #71), checked against Scryfall and
 * their rulings 2026-10-21.
 */
public class UnfinityBlueBlackBatchTest extends AITest {

    @Test
    public void testSuperDuperLostTucksItBeneathXCards() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 6; i++) {
            addCardToZone("Island", opp, ZoneType.Library);
        }
        final Card angel = addCard("Serra Angel", opp);
        final Card spell = addCardToZone("Super-Duper Lost", ai, ZoneType.Hand);
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        for (int i = 0; i < 5; i++) {
            addCard("Island", ai);
        }
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa));
        AssertJUnit.assertTrue(sa.getTargets().contains(angel));
        AbilityUtils.resolve(sa);
        final int doors = spell.getChosenNumber();
        AssertJUnit.assertTrue("a simulated room has 0 to 3 doors", doors >= 0 && doors <= 3);
        AssertJUnit.assertEquals(doors, opp.getCardsIn(ZoneType.Library).indexOf(game.getCardState(angel)));
    }

    @Test
    public void testAlliteration() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card trapezist = addCard("Treacherous Trapezist", p);
        final Card crusader = addCard("Goblin Guide", p);
        final Card bears = addCard("Grizzly Bears", p);
        final Card decisions = addCardToZone("Decisions, Decisions", p, ZoneType.Hand);
        final Card cuisine = addCardToZone("Lightning Bolt", p, ZoneType.Hand);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(crusader.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertFalse(bears.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertTrue("the same word twice counts", decisions.isValid("Card.Alliterative", p, trapezist, null));
        AssertJUnit.assertFalse("different initials", cuisine.isValid("Card.Alliterative", p, trapezist, null));
    }

    @Test
    public void testDisemvowel() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card angel = addCard("Serra Angel", opp);
        final Card spell = addCardToZone("Disemvowel", p, ZoneType.Hand);
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        sa.getTargets().add(angel);
        final int life = opp.getLife();
        AbilityUtils.resolve(sa);
        AssertJUnit.assertTrue(game.getCardState(angel).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertEquals("Serra Angel: E and A", life - 2, opp.getLife());
    }
}
