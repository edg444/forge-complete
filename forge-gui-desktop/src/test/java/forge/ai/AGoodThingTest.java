package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;

/**
 * A Good Thing (CMB1 #86), checked against Scryfall and its ruling 2026-10-15: doubling your life total is gaining (or
 * losing) the difference, and your own spells and abilities can't get rid of it.
 */
public class AGoodThingTest extends AITest {

    private static void upkeep(final Card goodThing, final Player you) {
        for (Trigger t : goodThing.getTriggers()) {
            SpellAbility sa = t.ensureAbility();
            sa.setActivatingPlayer(you);
            AbilityUtils.resolve(sa);
        }
    }

    private static SpellAbility spell(final Game game, final Player p, final String name) {
        Card c = p.getGame().getAction().moveToStack(
                p.getGame().getCardState(p.getZone(ZoneType.Hand).getCards().stream().filter(x -> x.getName().equals(name)).findFirst().get()), null);
        SpellAbility sa = c.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        return sa;
    }

    @Test
    public void doublesAndThenYouLose() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.UPKEEP, ai);
        Card good = addCard("A Good Thing", ai);
        upkeep(good, ai);
        AssertJUnit.assertEquals(40, ai.getLife());
        AssertJUnit.assertFalse(ai.hasLost());

        ai.setLife(-3, null);
        upkeep(good, ai);
        AssertJUnit.assertEquals("twice as far below 0", -6, ai.getLife());

        ai.setLife(600, null);
        upkeep(good, ai);
        AssertJUnit.assertEquals(1200, ai.getLife());
        AssertJUnit.assertTrue(ai.hasLost());
    }

    @Test
    public void revivalRevengeDoublesToo() {
        // the same SetLife from your life total, which used to set it to 0
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card rr = addCardToZone("Revival // Revenge", ai, ZoneType.Hand);
        SpellAbility revenge = null;
        for (SpellAbility sa : rr.getAllSpellAbilities()) {
            if (sa.getApi() == forge.game.ability.ApiType.SetLife) {
                revenge = sa;
            }
        }
        AssertJUnit.assertNotNull(revenge);
        revenge.setActivatingPlayer(ai);
        for (SpellAbility part = revenge; part != null; part = part.getSubAbility()) {
            if (part.usesTargeting()) {
                part.getTargets().add(game.getPlayers().get(0));
            }
        }
        AbilityUtils.resolve(revenge);
        AssertJUnit.assertEquals(40, ai.getLife());
    }

    @Test
    public void itsALifeGain() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.UPKEEP, ai);
        Card good = addCard("A Good Thing", ai);
        addCard("Boon Reflection", ai);
        game.getAction().checkStateEffects(true);
        upkeep(good, ai);
        AssertJUnit.assertEquals("gains 20, doubled by Boon Reflection", 60, ai.getLife());
    }

    @Test
    public void yourOwnSpellsCantGetRidOfIt() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card good = addCard("A Good Thing", ai);
        addCardToZone("Disenchant", ai, ZoneType.Hand);
        addCardToZone("Disenchant", opp, ZoneType.Hand);
        addCardToZone("Tranquility", ai, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        SpellAbility mine = spell(game, ai, "Disenchant");
        AssertJUnit.assertFalse("can't target", mine.canTarget(good));
        SpellAbility theirs = spell(game, opp, "Disenchant");
        AssertJUnit.assertTrue(theirs.canTarget(good));

        AssertJUnit.assertFalse("can't exile", good.canExiledBy(mine, true));
        AssertJUnit.assertTrue(good.canExiledBy(theirs, true));
        AssertJUnit.assertFalse("can't sacrifice", good.canBeSacrificedBy(mine, true));

        SpellAbility wipe = spell(game, ai, "Tranquility");
        AssertJUnit.assertFalse("can't destroy", game.getAction().destroy(good, wipe, true, null));
        AssertJUnit.assertTrue(good.isInPlay());
        AssertJUnit.assertTrue("an opponent's can", game.getAction().destroy(good, theirs, true, null));
        AssertJUnit.assertFalse(game.getCardState(good).isInPlay());
    }
}
