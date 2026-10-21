package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.card.CardStateName;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Omniclown Colossus // Pie-roclasm (UNF #116), Rock Star (UNF #122) and Ticking Mime Bomb (UNF #124), checked against
 * Scryfall and their rulings 2026-10-21.
 */
public class UnfinityRedBatch3Test extends AITest {

    private void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    @Test
    public void testPieRoclasmSparesClowns() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", p);
        final Card tapper = addCard("T.A.P.P.E.R.", p);
        final Card giant = addCard("Hill Giant", p);
        final Card colossus = addCardToZone("Omniclown Colossus", p, ZoneType.Hand);
        SpellAbility pie = null;
        for (final SpellAbility sa : colossus.getState(CardStateName.Secondary).getSpellAbilities()) {
            pie = sa;
        }
        pie.setActivatingPlayer(p);
        AbilityUtils.resolve(pie);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(game.getCardState(bears).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue(game.getCardState(giant).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue("a Clown", tapper.isInPlay());
    }

    @Test
    public void testTickingMimeBomb() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card angel = addCard("Serra Angel", opp);
        addCard("T.A.P.P.E.R.", ai);
        final Card bomb = addCard("Ticking Mime Bomb", ai);
        final SpellAbility etb = bomb.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        AbilityUtils.resolve(etb);
        AssertJUnit.assertEquals("two Robots, twice that", 4, angel.getDamage());
    }

    @Test
    public void testOpeningCeremony() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card spell = addCardToZone("Opening Ceremony", ai, ZoneType.Hand);
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertEquals("W U B R G C", 6, ai.getManaPool().totalMana());
        final long pack = ai.getCardsIn(ZoneType.Exile).size();
        AssertJUnit.assertTrue("a booster pack's worth in exile: " + pack, pack >= 8);
        final Card nonland = ai.getCardsIn(ZoneType.Exile).stream().filter(c -> !c.isLand()).findFirst().orElse(null);
        AssertJUnit.assertNotNull(nonland);
        AssertJUnit.assertFalse("castable this turn", nonland.mayPlay(ai).isEmpty());
    }

    @Test
    public void testRockStar() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 7; i++) {
            addCardToZone(i % 2 == 0 ? "Island" : "Grizzly Bears", ai, ZoneType.Library);
        }
        addCardToZone("Mountain", ai, ZoneType.Hand);
        final Card star = addCard("Rock Star", ai);
        final SpellAbility etb = star.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        final int library = ai.getCardsIn(ZoneType.Library).size();
        AbilityUtils.resolve(etb);
        resolveAll(game);
        AssertJUnit.assertEquals("one card of the five came out", library - 1, ai.getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertEquals(0, star.getRememberedCount());
    }
}
