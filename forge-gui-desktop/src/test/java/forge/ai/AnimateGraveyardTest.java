package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.GamePieceType;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Animate Graveyard (UNF #65), checked against Scryfall and its 10 rulings 2026-10-21: the graveyard becomes a creature on
 * the battlefield, as big as it is, with the text boxes of its creature cards - and it's still a graveyard.
 */
public class AnimateGraveyardTest extends AITest {

    private Card animate(final Game game, final Player p) {
        final Card aura = addCardToZone("Animate Graveyard", p, ZoneType.Hand);
        final SpellAbility sa = aura.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);
        return p.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getGamePieceType() == GamePieceType.GRAVEYARD)
                .findFirst().orElse(null);
    }

    @Test
    public void testTheGraveyardIsACreature() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        addCardToZone("Serra Angel", p, ZoneType.Graveyard);
        addCardToZone("Lightning Bolt", p, ZoneType.Graveyard);
        addCardToZone("Island", p, ZoneType.Graveyard);
        final Card yard = animate(game, p);
        AssertJUnit.assertNotNull(yard);
        AssertJUnit.assertTrue(yard.isCreature());
        AssertJUnit.assertEquals(3, yard.getNetPower());
        AssertJUnit.assertTrue("Serra Angel's text box", yard.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertTrue(yard.getType().getCreatureTypes().isEmpty());

        addCardToZone("Grizzly Bears", p, ZoneType.Graveyard);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("cards still go to the graveyard, and it grows", 4, yard.getNetToughness());
        AssertJUnit.assertEquals(4, p.getCardsIn(ZoneType.Graveyard).size());
    }

    @Test
    public void testIfItWouldLeaveTheAuraIsExiledInstead() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        addCardToZone("Serra Angel", p, ZoneType.Graveyard);
        final Card yard = animate(game, p);
        final Card aura = yard.getAttachedCards().get(0);
        game.getAction().destroy(yard, null, true, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(game.getCardState(aura).isInZone(ZoneType.Exile));
        AssertJUnit.assertFalse("just a graveyard again", yard.isInPlay());
        AssertJUnit.assertEquals("the graveyard didn't die", 1, p.getCardsIn(ZoneType.Graveyard).size());
    }
}
