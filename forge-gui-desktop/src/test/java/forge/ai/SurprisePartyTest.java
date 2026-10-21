package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.player.HiddenPrintedToken;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Surprise Party (UNF #29), checked against Scryfall and its 6 rulings 2026-10-21: printed Clown Robot tokens hidden on
 * the table from the start of the game (user, 2026-10-21), one per spot, spotted by an opponent pointing at the spot;
 * the ones hidden and unspotted as it's cast come out when it enters.
 */
public class SurprisePartyTest extends AITest {

    private SpellAbility hideAbility(final Player p) {
        for (final Card eff : p.getCardsIn(ZoneType.Command)) {
            if (eff.getName().equals("Printed Tokens")) {
                final SpellAbility sa = eff.getSpellAbilities().get(0);
                sa.setActivatingPlayer(p);
                return sa;
            }
        }
        throw new AssertionError("no Printed Tokens effect");
    }

    @Test
    public void testADeckWithItCanHideTokensFromTheStart() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCardToZone("Surprise Party", p, ZoneType.Library);
        addCardToZone("Surprise Party", p, ZoneType.Library);
        game.getAction().createPrintedTokenHiding();
        AssertJUnit.assertEquals("one per token kind", 1,
                p.getCardsIn(ZoneType.Command).stream().filter(c -> c.getName().equals("Printed Tokens")).count());
        AssertJUnit.assertTrue(opp.getCardsIn(ZoneType.Command).stream().noneMatch(c -> c.getName().equals("Printed Tokens")));

        AbilityUtils.resolve(hideAbility(p));
        AssertJUnit.assertEquals(1, p.getHiddenPrintedTokens().size());
        AssertJUnit.assertTrue("the opponent can look for it",
                opp.getCardsIn(ZoneType.Command).stream().anyMatch(c -> c.getName().equals("Hidden Tokens")));
        // with no permanents, plain sight is the only spot, and it's taken
        AbilityUtils.resolve(hideAbility(p));
        AssertJUnit.assertEquals("they can't touch", 1, p.getHiddenPrintedTokens().size());
    }

    @Test
    public void testOnlyTokensHiddenAndUnspottedAtCastComeOut() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        for (int i = 0; i < 4; i++) {
            addCard("Plains", p);
        }
        final Card party = addCardToZone("Surprise Party", p, ZoneType.Hand);
        game.getAction().createPrintedTokenHiding();
        for (int i = 0; i < 3; i++) {
            AbilityUtils.resolve(hideAbility(p));
        }
        AssertJUnit.assertEquals(3, p.getHiddenPrintedTokens().size());
        final HiddenPrintedToken spottedBefore = p.getHiddenPrintedTokens().get(0);
        final HiddenPrintedToken spottedAfter = p.getHiddenPrintedTokens().get(1);
        spottedBefore.setSpotted();

        final SpellAbility atCast = party.getTriggers().get(0).ensureAbility();
        atCast.setActivatingPlayer(p);
        AbilityUtils.resolve(atCast);
        spottedAfter.setSpotted();
        AbilityUtils.resolve(hideAbility(p));
        AssertJUnit.assertEquals("hidden after it was cast", 4, p.getHiddenPrintedTokens().size());

        final Card inPlay = game.getAction().moveToPlay(party, null, null);
        final SpellAbility surprise = inPlay.getTriggers().get(1).ensureAbility();
        surprise.setActivatingPlayer(p);
        AbilityUtils.resolve(surprise);
        game.getAction().checkStateEffects(true);

        final long robots = p.getCreaturesInPlay().stream().filter(c -> c.getName().equals("Clown Robot Token")).count();
        AssertJUnit.assertEquals("the unspotted one and the one spotted after the cast", 2, robots);
        AssertJUnit.assertEquals(2, p.getHiddenPrintedTokens().size());
        final Card robot = p.getCreaturesInPlay().stream().filter(c -> c.getName().equals("Clown Robot Token")).findFirst().get();
        AssertJUnit.assertEquals("Robots get +1/+0", 2, robot.getNetPower());
        AssertJUnit.assertTrue(robot.hasKeyword(Keyword.VIGILANCE));
    }

    @Test
    public void testPointingAtTheRightSpotSpotsIt() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCardToZone("Surprise Party", p, ZoneType.Library);
        game.getAction().createPrintedTokenHiding();
        AbilityUtils.resolve(hideAbility(p));
        final Card look = opp.getCardsIn(ZoneType.Command).stream().filter(c -> c.getName().equals("Hidden Tokens")).findFirst().get();
        final SpellAbility seek = look.getSpellAbilities().get(0);
        seek.setActivatingPlayer(opp);
        // plain sight is the only spot, so the AI's random point can't miss
        AbilityUtils.resolve(seek);
        AssertJUnit.assertTrue(p.getHiddenPrintedTokens().get(0).isSpotted());
    }
}
