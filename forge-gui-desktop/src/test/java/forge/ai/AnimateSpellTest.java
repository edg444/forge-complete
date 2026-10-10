package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Animate Spell (Mystery Booster playtest card), Oracle text and rulings checked against Scryfall 2026-10-10: the
 * enchanted spell's card goes onto the battlefield as a creature with P/T equal to its mana value; Animate Spell then
 * enchants that permanent; when Animate Spell leaves, the card's owner casts it (paying its costs) or sacrifices it.
 * The card's text puts an instant onto the battlefield, so it beats rule 304.4 (101.1).
 */
public class AnimateSpellTest extends AITest {

    @Test
    public void testSpellBecomesACreatureThenGoesAway() {
        play(false, false);
    }

    @Test
    public void testOwnerCastsItAgain() {
        play(true, false);
    }

    @Test
    public void testLeavingTogetherMeansNoCast() {
        play(true, true);
    }

    private void play(boolean ownerHasMana, boolean wrath) {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        Card mountain = ownerHasMana ? addCard("Mountain", opp) : null;

        Card bolt = addCardToZone("Lightning Bolt", opp, ZoneType.Hand);
        SpellAbility boltSa = bolt.getFirstSpellAbility();
        boltSa.setActivatingPlayer(opp);
        boltSa.getTargets().add(me);
        bolt = game.getAction().moveToStack(bolt, boltSa);
        boltSa.setHostCard(bolt);
        game.getStack().add(boltSa);

        Card animate = addCardToZone("Animate Spell", me, ZoneType.Hand);
        SpellAbility auraSa = animate.getFirstSpellAbility();
        auraSa.setActivatingPlayer(me);
        auraSa.getTargets().add(game.getStack().peekAbility());
        animate = game.getAction().moveToStack(animate, auraSa);
        auraSa.setHostCard(animate);
        game.getStack().add(auraSa);

        playUntilStackClear(game);

        AssertJUnit.assertEquals("the Bolt never resolved", 20, me.getLife());
        Card permanent = game.getCardState(bolt);
        AssertJUnit.assertTrue(permanent.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertTrue(permanent.isCreature() && permanent.isInstant());
        AssertJUnit.assertEquals(1, permanent.getNetPower());
        AssertJUnit.assertEquals(1, permanent.getNetToughness());
        AssertJUnit.assertEquals(opp, permanent.getController());
        Card aura = game.getCardState(animate);
        AssertJUnit.assertTrue(aura.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertEquals(permanent, aura.getAttachedTo());

        // Animate Spell goes away: with no red mana the owner can't cast the Bolt, so it's sacrificed; with a Mountain
        // they cast it from the battlefield, paying for it
        // ruling: if the creature leaves at the same time as Animate Spell, it can't be cast
        Card removal = addCardToZone(wrath ? "Akroma's Vengeance" : "Disenchant", me, ZoneType.Hand);
        SpellAbility dis = removal.getFirstSpellAbility();
        dis.setActivatingPlayer(me);
        if (!wrath) {
            dis.getTargets().add(aura);
        }
        forge.game.ability.AbilityUtils.resolve(dis);
        game.getTriggerHandler().runWaitingTriggers();
        playUntilStackClear(game);
        AssertJUnit.assertTrue(game.getCardState(bolt).isInZone(ZoneType.Graveyard));
        if (ownerHasMana && !wrath) {
            AssertJUnit.assertTrue("paid for", game.getCardState(mountain).isTapped());
            AssertJUnit.assertEquals(17, me.getLife());
        } else {
            AssertJUnit.assertEquals(20, me.getLife());
        }
    }
}
