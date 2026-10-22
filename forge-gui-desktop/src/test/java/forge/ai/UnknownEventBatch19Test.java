package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The Sixth Seraph, The Keeper of Four Scythes, The Knight of Weeks, The Highland Runner, Crux of Mirrodin, The Curve
 * Keeper, Theopholos, Order Acolyte and Agoraphobic Phyrexian (UNK white rares), checked against Scryfall 2026-10-21
 * (no rulings).
 */
public class UnknownEventBatch19Test extends AITest {

    @Test
    public void testKnightOfWeeks() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card knight = addCard("The Knight of Weeks", ai);
        knight.addCounterInternal(CounterType.getType("DAY"), 6, ai, false, null, null);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertEquals(1, knight.getNetPower());
        knight.addCounterInternal(CounterType.getType("DAY"), 1, ai, false, null, null);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertEquals("seven day counters", 8, knight.getNetPower());
    }

    @Test
    public void testCurveKeeper() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card keeper = addCard("The Curve Keeper", ai);
        addCard("Grizzly Bears", ai);
        addCard("Gray Ogre", ai);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertEquals("2 and 3 in a row; the Keeper's 6 alone", 3 + 2, keeper.getNetPower());
    }

    @Test
    public void testCruxOfMirrodin() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card memnite = addCard("Memnite", human);
        final Card pod = addCard("Birthing Pod", human);
        final Card crux = addCardToZone("Crux of Mirrodin", ai, ZoneType.Hand);
        final SpellAbility mirran = crux.getFirstSpellAbility().getAdditionalAbilityList("Choices").get(0);
        mirran.setActivatingPlayer(ai);
        AbilityUtils.resolve(mirran);
        AssertJUnit.assertTrue("Scars of Mirrodin", game.getCardState(memnite).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue("New Phyrexia isn't in the first list", pod.isInPlay());
    }

    @Test
    public void testKeeperOfFourScythes() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card keeper = addCard("The Keeper of Four Scythes", ai);
        final SpellAbility etb = keeper.getTriggers().stream().filter(t -> t.getParam("TriggerDescription").contains("four Scythe"))
                .findFirst().orElseThrow().ensureAbility();
        etb.setActivatingPlayer(ai);
        AbilityUtils.resolve(etb);
        AssertJUnit.assertEquals(4, ai.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getType().hasSubtype("Scythe")).count());
    }

    @Test
    public void testHighlandRunnerUncommanders() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card commander = addCardToZone("Grizzly Bears", ai, ZoneType.Command);
        ai.addCommander(commander);
        final Card runner = addCard("The Highland Runner", ai);
        final SpellAbility etb = runner.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        ai.setLife(7, null);
        AbilityUtils.resolve(etb);
        AssertJUnit.assertEquals(20, ai.getLife());
        AssertJUnit.assertTrue(ai.getCommanders().isEmpty());
        AssertJUnit.assertTrue("shuffled into the library", game.getCardState(commander).isInZone(ZoneType.Library));
    }
}
