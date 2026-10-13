package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.StaticData;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Witty Demon (CMB1 #50), checked against Scryfall and rulings 2026-10-13: a starting deck at least 13 cards over the
 * format minimum (60 in Constructed) tutors, otherwise it deals 4 damage to you.
 */
public class WittyDemonTest extends AITest {

    private Player enterWithDeckOf(int deckSize) {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        ai.getRegisteredPlayer().getDeck().getMain().clear();
        ai.getRegisteredPlayer().getDeck().getMain().add(StaticData.instance().getCommonCards().getCard("Swamp"), deckSize);
        fillLibrary(ai, 5);
        Card demon = addCard("Witty Demon", ai);
        SpellAbility trig = demon.getTriggers().get(0).ensureAbility();
        trig.setActivatingPlayer(ai);
        forge.game.ability.AbilityUtils.resolve(trig);
        return ai;
    }

    @Test
    public void seventyThreeCardsTutors() {
        Player ai = enterWithDeckOf(73);
        AssertJUnit.assertEquals(20, ai.getLife());
        AssertJUnit.assertEquals(1, ai.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void seventyTwoCardsHurts() {
        Player ai = enterWithDeckOf(72);
        AssertJUnit.assertEquals(16, ai.getLife());
        AssertJUnit.assertEquals(0, ai.getCardsIn(ZoneType.Hand).size());
    }
}
