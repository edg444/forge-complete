package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Gunk Slug (CMB1 #43), checked against Scryfall and rulings 2026-10-13. Gunk token cards are cards that persist in
 * every zone. Judgment call (design intent over CR 111.2/400.3): the targeted opponent owns the Gunk shuffled into
 * their library.
 */
public class GunkSlugTest extends AITest {

    private void resolveAll(final Game game) {
        do {
            playUntilStackClear(game);
            game.getAction().checkStateEffects(true);
        } while (game.getStack().addAllTriggeredAbilitiesToStack());
    }

    @Test
    public void shufflesThreeGunkIntoTheOpponentsLibrary() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        fillLibrary(opp, 10);
        fillLibrary(ai, 10);
        addCards("Swamp", 3, ai);
        Card slug = addCardToZone("Gunk Slug", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility cast = slug.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, cast, null));
        resolveAll(game);

        AssertJUnit.assertEquals(10, ai.getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertEquals(13, opp.getCardsIn(ZoneType.Library).size());
        CardCollection gunk = CardLists.filter(opp.getCardsIn(ZoneType.Library), c -> c.getName().equals("Gunk"));
        AssertJUnit.assertEquals(3, gunk.size());
        for (Card g : gunk) {
            AssertJUnit.assertEquals(opp, g.getOwner());
            AssertJUnit.assertTrue(g.isTokenCard());
            AssertJUnit.assertFalse(g.isToken());
            AssertJUnit.assertTrue(g.isSorcery() && g.getType().hasSubtype("Gunk"));
            AssertJUnit.assertTrue(g.getColor().isColorless());
        }
    }

    @Test
    public void gunkCyclesButCantBeCast() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        fillLibrary(opp, 5);
        addCards("Swamp", 3, ai);
        addCards("Plains", 4, opp);
        Card slug = addCard("Gunk Slug", ai);
        SpellAbility make = slug.getTriggers().get(0).ensureAbility();
        make.setActivatingPlayer(ai);
        make.getTargets().add(opp);
        make.resolve();
        Card gunk = CardLists.filter(opp.getCardsIn(ZoneType.Library), c -> c.getName().equals("Gunk")).get(0);
        gunk = game.getAction().moveToHand(gunk, null);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        game.getAction().checkStateEffects(true);

        SpellAbility cycling = null;
        for (SpellAbility sa : gunk.getAllPossibleAbilities(opp, true)) {
            AssertJUnit.assertFalse("no mana cost: it can't be cast", sa.isSpell());
            if (sa.isCycling()) {
                cycling = sa;
            }
        }
        AssertJUnit.assertNotNull(cycling);
        final int library = opp.getCardsIn(ZoneType.Library).size();
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(opp, cycling, null));
        resolveAll(game);
        Card cycled = game.getCardState(gunk);
        AssertJUnit.assertTrue("a token card stays in the game", cycled.isInZone(ZoneType.Graveyard));
        AssertJUnit.assertEquals(opp, cycled.getOwner());
        AssertJUnit.assertEquals(library - 1, opp.getCardsIn(ZoneType.Library).size());
    }
}
