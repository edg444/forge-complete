package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The Taught by ___ commander enchantments (UNK), checked against Scryfall 2026-10-22 (no rulings): cast on a commander
 * in the command zone, they stay attached as it moves between face-up zones.
 */
public class UnknownEventBatch37Test extends AITest {

    @Test
    public void testCommanderEnchantmentFollowsItsCommander() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        Card commander = addCardToZone("Grizzly Bears", ai, ZoneType.Command);
        ai.addCommander(commander);
        commander.setCommander(true);
        final Card serra = addCardToZone("Taught by Serra", ai, ZoneType.Hand);
        final SpellAbility cast = serra.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        AssertJUnit.assertTrue("castable on a commander in your command zone", cast.canTarget(commander));
        cast.getTargets().add(commander);
        AbilityUtils.resolve(cast);
        Card aura = game.getCardState(serra);
        AssertJUnit.assertTrue("it waits in the command zone with its commander", aura.isInZone(ZoneType.Command));
        AssertJUnit.assertEquals(commander, aura.getEntityAttachedTo());

        // cast the commander: the Aura stays put while it's a spell, then comes along onto the battlefield
        commander = game.getAction().moveToStack(commander, null);
        aura = game.getCardState(serra);
        AssertJUnit.assertTrue(aura.isInZone(ZoneType.Command));
        AssertJUnit.assertSame(commander, aura.getEntityAttachedTo());
        commander = game.getAction().moveToPlay(commander, ai, null, null);
        aura = game.getCardState(serra);
        AssertJUnit.assertTrue(aura.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertSame(commander, aura.getEntityAttachedTo());
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(commander.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertTrue(game.getCardState(serra).isInZone(ZoneType.Battlefield));

        // it dies: both go to the graveyard, still together
        commander = game.getAction().moveToGraveyard(commander, null);
        aura = game.getCardState(serra);
        AssertJUnit.assertTrue(aura.isInZone(ZoneType.Graveyard));
        AssertJUnit.assertSame(commander, aura.getEntityAttachedTo());

        // the library is face down: it doesn't follow (no longer a commander, so it isn't sent to the command zone)
        commander.setCommander(false);
        commander = game.getAction().moveToLibrary(commander, 0, null, null);
        AssertJUnit.assertTrue(commander.isInZone(ZoneType.Library));
        aura = game.getCardState(serra);
        AssertJUnit.assertTrue(aura.isInZone(ZoneType.Graveyard));
        AssertJUnit.assertFalse(aura.isAttachedToEntity());
    }

    @Test
    public void testOnlyCommanders() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", human);
        final Card theirCommander = addCardToZone("Llanowar Elves", human, ZoneType.Command);
        human.addCommander(theirCommander);
        theirCommander.setCommander(true);
        final Card vito = addCardToZone("Taught by Vito", ai, ZoneType.Hand);
        final SpellAbility cast = vito.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        AssertJUnit.assertFalse(cast.canTarget(bears));
        AssertJUnit.assertFalse("not someone else's command zone", cast.canTarget(theirCommander));
    }
}
