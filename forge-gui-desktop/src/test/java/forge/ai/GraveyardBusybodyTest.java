package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Graveyard Busybody, following its Unstable rulings: every graveyard is its controller's and no one else has
 * one, while cards still sit in (and go to) their owners' graveyards.
 */
public class GraveyardBusybodyTest extends AITest {

    private static final String[] FILLER = {"Grizzly Bears", "Shivan Dragon", "Runeclaw Bear", "Serra Angel",
            "Llanowar Elves", "Giant Growth"};

    private static void refresh(Game game) {
        game.getAction().checkStateEffects(true);
    }

    @Test
    public void testAllGraveyardsAreYours() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card mine = addCardToZone("Grizzly Bears", me, ZoneType.Graveyard);
        Card theirs = addCardToZone("Serra Angel", opp, ZoneType.Graveyard);
        refresh(game);
        AssertJUnit.assertEquals(1, me.getCardsIn(ZoneType.Graveyard).size());
        AssertJUnit.assertEquals(opp, theirs.getController());

        addCard("Graveyard Busybody", me);
        refresh(game);
        AssertJUnit.assertEquals(me, game.getGraveyardHolder());
        AssertJUnit.assertEquals(2, me.getCardsIn(ZoneType.Graveyard).size());
        AssertJUnit.assertEquals(0, opp.getCardsIn(ZoneType.Graveyard).size());
        // still separate graveyards, and still their card
        AssertJUnit.assertTrue(opp.getZone(ZoneType.Graveyard).contains(theirs));
        AssertJUnit.assertEquals(opp, theirs.getOwner());
        AssertJUnit.assertEquals(me, theirs.getController());
        AssertJUnit.assertEquals(me, mine.getController());

        // "target creature card from your graveyard" reaches into theirs
        Card raise = addCardToZone("Raise Dead", me, ZoneType.Hand);
        SpellAbility sa = raise.getFirstSpellAbility();
        sa.setActivatingPlayer(me);
        AssertJUnit.assertTrue(sa.canTarget(theirs));
        // and a card put into a graveyard still goes to its owner's
        Card dies = game.getAction().moveToGraveyard(addCard("Llanowar Elves", opp), null, null);
        AssertJUnit.assertTrue(opp.getZone(ZoneType.Graveyard).contains(dies));
    }

    @Test
    public void testOnlyYouCanUseGraveyardAbilities() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card think = addCardToZone("Think Twice", opp, ZoneType.Graveyard);
        addCard("Graveyard Busybody", me);
        refresh(game);

        // flashback is offered to me from their graveyard, and not to them
        AssertJUnit.assertTrue(think.getAllPossibleAbilities(me, true).stream().anyMatch(SpellAbility::isFlashback));
        AssertJUnit.assertTrue(think.getAllPossibleAbilities(opp, true).isEmpty());
        AssertJUnit.assertTrue(me.getCardsActivatableInExternalZones(false).contains(think));
    }

    @Test
    public void testPowerCountsFlavorTextInAllYourGraveyards() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        int flavored = 0;
        int oppFlavored = 0;
        for (String name : FILLER) {
            Card a = addCardToZone(name, me, ZoneType.Graveyard);
            Card b = addCardToZone(name, opp, ZoneType.Graveyard);
            flavored += a.getFlavorText().isEmpty() ? 0 : 2;
            oppFlavored += b.getFlavorText().isEmpty() ? 0 : 1;
        }
        AssertJUnit.assertTrue("need some flavor text in their graveyard to mean anything", oppFlavored > 0);
        Card busybody = addCard("Graveyard Busybody", me);
        refresh(game);
        AssertJUnit.assertEquals(flavored, busybody.getNetPower());
        AssertJUnit.assertEquals(flavored, busybody.getNetToughness());
    }

    @Test
    public void testNewestBusybodyWins() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (String name : FILLER) {
            addCardToZone(name, me, ZoneType.Graveyard);
        }
        Card mine = addCard("Graveyard Busybody", me);
        refresh(game);
        AssertJUnit.assertTrue(mine.isInPlay());

        addCard("Graveyard Busybody", opp);
        refresh(game);
        // I no longer have any graveyards, so mine is 0/0 and goes to the one I used to have
        AssertJUnit.assertEquals(opp, game.getGraveyardHolder());
        Card after = game.getCardState(mine);
        AssertJUnit.assertTrue(me.getZone(ZoneType.Graveyard).contains(after));
    }
    @Test
    public void testGamePlaysOnWithABusybody() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (Player p : game.getPlayers()) {
            fillLibrary(p, 20);
            for (int i = 0; i < 5; i++) {
                addCard("Island", p);
            }
            addCardToZone("Think Twice", p, ZoneType.Graveyard);
            addCardToZone("Gravedigger", p, ZoneType.Hand);
            addCardToZone("Grizzly Bears", p, ZoneType.Graveyard);
        }
        addCard("Graveyard Busybody", me);
        refresh(game);
        for (int i = 0; i < 4 && !game.isGameOver(); i++) {
            playUntilNextTurn(game);
        }
        AssertJUnit.assertEquals(me, game.getGraveyardHolder());
        AssertJUnit.assertEquals(0, opp.getCardsIn(ZoneType.Graveyard).size());
    }
}
