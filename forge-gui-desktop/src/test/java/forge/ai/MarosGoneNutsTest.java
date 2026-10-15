package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityDoubleDoubling;
import forge.game.zone.ZoneType;

/**
 * Maro's Gone Nuts (CMB1 #81), checked against Scryfall and its 4 rulings 2026-10-15: effects that double - "double",
 * "double that much", "twice as many" replacements - quadruple, and octuple with two out.
 */
public class MarosGoneNutsTest extends AITest {

    private static void cast(final Game game, final Player p, final Card inHand, final GameObject target) {
        Card onStack = game.getAction().moveToStack(inHand, null);
        SpellAbility sa = onStack.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        if (target != null) {
            sa.getTargets().add(target);
        }
        game.getStack().add(sa);
        game.getStack().resolveStack();
        game.getAction().checkStateEffects(true);
    }

    private Game setUp() {
        Game game = initAndCreateGame();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, game.getPlayers().get(1));
        return game;
    }

    @Test
    public void twoFourEight() {
        Game game = setUp();
        Player ai = game.getPlayers().get(1);
        AssertJUnit.assertEquals(2, StaticAbilityDoubleDoubling.factor(game));
        addCard("Maro's Gone Nuts", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(4, StaticAbilityDoubleDoubling.factor(game));
        addCard("Maro's Gone Nuts", game.getPlayers().get(0));
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("an opponent's counts too", 8, StaticAbilityDoubleDoubling.factor(game));
    }

    @Test
    public void doubleThatDamageQuadruples() {
        Game game = setUp();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        addCard("Furnace of Rath", ai);
        addCard("Maro's Gone Nuts", ai);
        game.getAction().checkStateEffects(true);
        cast(game, ai, addCardToZone("Shock", ai, ZoneType.Hand), opp);
        AssertJUnit.assertEquals(12, opp.getLife());
    }

    @Test
    public void twiceThatManyTokensQuadruples() {
        Game game = setUp();
        Player ai = game.getPlayers().get(1);
        addCard("Doubling Season", ai);
        addCard("Maro's Gone Nuts", ai);
        game.getAction().checkStateEffects(true);
        cast(game, ai, addCardToZone("Raise the Alarm", ai, ZoneType.Hand), null);
        AssertJUnit.assertEquals(8, ai.getCreaturesInPlay().size());
    }

    @Test
    public void doubleThePowerQuadruples() {
        Game game = setUp();
        Player ai = game.getPlayers().get(1);
        Card bears = addCard("Grizzly Bears", ai);
        addCard("Maro's Gone Nuts", ai);
        game.getAction().checkStateEffects(true);
        cast(game, ai, addCardToZone("Unleash Fury", ai, ZoneType.Hand), bears);
        AssertJUnit.assertEquals(8, bears.getNetPower());
        AssertJUnit.assertEquals(2, bears.getNetToughness());
    }

    @Test
    public void doubleTheCountersQuadruples() {
        Game game = setUp();
        Player ai = game.getPlayers().get(1);
        Card bears = addCard("Grizzly Bears", ai);
        bears.addCounterInternal(CounterEnumType.P1P1, 1, ai, false, null, null);
        Card vorel = addCard("Vorel of the Hull Clade", ai);
        addCard("Maro's Gone Nuts", ai);
        game.getAction().checkStateEffects(true);
        SpellAbility ab = vorel.getSpellAbilities().stream().filter(SpellAbility::isActivatedAbility).findFirst().get();
        ab.setActivatingPlayer(ai);
        ab.getTargets().add(bears);
        forge.game.ability.AbilityUtils.resolve(ab);
        AssertJUnit.assertEquals(4, bears.getCounters(CounterEnumType.P1P1));
    }

    @Test
    public void doubleYourLifeAndTwiceAsMuchManaQuadruple() {
        Game game = setUp();
        Player ai = game.getPlayers().get(1);
        addCard("Maro's Gone Nuts", ai);
        addCard("Mana Reflection", ai);
        Card forest = addCard("Forest", ai);
        game.getAction().checkStateEffects(true);
        cast(game, ai, addCardToZone("Beacon of Immortality", ai, ZoneType.Hand), ai);
        AssertJUnit.assertEquals(80, ai.getLife());

        SpellAbility tap = forest.getManaAbilities().getFirst();
        tap.setActivatingPlayer(ai);
        tap.getManaPart().produceMana(tap);
        AssertJUnit.assertEquals(4, ai.getManaPool().totalMana());
    }

    @Test
    public void notWhenItOnlyMakesTwiceAsMany() {
        Game game = setUp();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        addCard("Maro's Gone Nuts", ai);
        game.getAction().checkStateEffects(true);
        // "deals twice that much damage" here is an amount, not an effect that doubles
        AssertJUnit.assertEquals("Count$RememberedSize/Twice", addCardToZone("Borborygmos and Fblthp", ai, ZoneType.Hand).getSVar("X"));
        AssertJUnit.assertEquals(20, opp.getLife());
    }
}
