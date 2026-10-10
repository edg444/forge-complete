package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Staff of the Letter Magus (Unstable) - Oracle text checked against Scryfall 2026-10-09 (no rulings). Y is a consonant
 * here, every Y counting (user, 2026-10-09).
 */
public class StaffOfTheLetterMagusTest extends AITest {

    private void cast(final Game game, final Player p, final String name, final String land) {
        addCard(land, p);
        final Card c = addCardToZone(name, p, ZoneType.Hand);
        final SpellAbility sa = c.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(p, sa, null));
        playUntilStackClear(game);
    }

    @Test
    public void lifeForEachTimeTheLetterAppears() {
        final Game game = initAndCreateGame();
        final Player opp = game.getPlayers().get(0);
        final Player me = game.getPlayers().get(1);
        final Card staff = addCard("Staff of the Letter Magus", me);
        staff.setChosenType("L");

        // any player's spell: "Llanowar Elves" has three
        cast(game, opp, "Llanowar Elves", "Forest");
        AssertJUnit.assertEquals(23, me.getLife());
        AssertJUnit.assertEquals(20, opp.getLife());

        // none in "Shock"
        cast(game, me, "Shock", "Mountain");
        AssertJUnit.assertEquals(23, me.getLife());
    }

    @Test
    public void yCounts() {
        final Game game = initAndCreateGame();
        final Player me = game.getPlayers().get(1);
        final Card staff = addCard("Staff of the Letter Magus", me);
        staff.setChosenType("Y");
        cast(game, me, "Holy Day", "Plains");
        AssertJUnit.assertEquals(22, me.getLife());
    }

    @Test
    public void choosesAnAllowedConsonantAsItEnters() {
        final Game game = initAndCreateGame();
        final Player me = game.getPlayers().get(1);
        addCardToZone("Llanowar Elves", me, ZoneType.Hand);
        final Card staff = addCardToZone("Staff of the Letter Magus", me, ZoneType.Hand);
        final Card played = game.getAction().moveToPlay(staff, null, null);
        AssertJUnit.assertTrue(played.hasChosenType());
        AssertJUnit.assertTrue("BCDFGHJKLMPQVWXYZ".contains(played.getChosenType()));
        // the AI takes the letter its spells' names have most of
        AssertJUnit.assertEquals("L", played.getChosenType());
    }
}
