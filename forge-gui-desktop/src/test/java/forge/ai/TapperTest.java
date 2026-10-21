package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * T.A.P.P.E.R. (UNF #31), checked against Scryfall and its 8 rulings 2026-10-21: its cost taps a creature you control
 * that has a hat - one Forge knows of, or one its controller has pointed out (honor system).
 */
public class TapperTest extends AITest {

    private SpellAbility ability(final Card c, final Player p, final boolean special) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (special ? sa.hasParam("SpecialAction") : sa.isActivatedAbility() && !sa.hasParam("SpecialAction")) {
                sa.setActivatingPlayer(p);
                return sa;
            }
        }
        throw new AssertionError();
    }

    private boolean canPay(final Card tapper, final Player p) {
        final SpellAbility sa = ability(tapper, p, false);
        return sa.getPayCosts().canPay(sa, p, false);
    }

    @Test
    public void testItNeedsACreatureWithAHat() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card tapper = addCard("T.A.P.P.E.R.", p);
        tapper.setSickness(false);
        final Card bears = addCard("Grizzly Bears", p);
        AssertJUnit.assertFalse("no creature with a hat", canPay(tapper, p));

        bears.setHatInArtClaim(true);
        AssertJUnit.assertTrue("one said to wear a hat counts", canPay(tapper, p));
    }

    @Test
    public void testPointingOutAHat() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card tapper = addCard("T.A.P.P.E.R.", p);
        tapper.setSickness(false);
        final Card bears = addCard("Grizzly Bears", p);
        final SpellAbility claim = ability(tapper, p, true);
        AssertJUnit.assertTrue(claim.canPlay());
        AssertJUnit.assertFalse("the AI can't see the art", ((PlayerControllerAi) p.getController()).getAi()
                .canPlaySa(claim) == AiPlayDecision.WillPlay);

        bears.setHasHatClaim(true);
        AssertJUnit.assertTrue(bears.isValid("Creature.HasHat", p, tapper, null));
        AssertJUnit.assertFalse("having one isn't wearing one", bears.isWearingHatInArt());
        AssertJUnit.assertTrue(canPay(tapper, p));
    }

    @Test
    public void testTheTapItself() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card tapper = addCard("T.A.P.P.E.R.", p);
        final Card angel = addCard("Serra Angel", opp);
        final SpellAbility tap = ability(tapper, p, false);
        tap.getTargets().add(angel);
        AbilityUtils.resolve(tap);
        AssertJUnit.assertTrue(angel.isTapped());
    }
}
