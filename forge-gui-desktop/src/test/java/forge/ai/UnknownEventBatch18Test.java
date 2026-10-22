package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Htbr, Racetrack Referee, Stika, Playtestress, Gerrard and Hanna, Draw Team Lines, Ratatwotwo, Kylem All-Star, Save
 * Point, The Inspector Inspector and Robby, Another Dwarf (UNK white rares), checked against Scryfall 2026-10-21 (no
 * rulings).
 */
public class UnknownEventBatch18Test extends AITest {

    @Test
    public void testHtbrStopsFlashback() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Htbr, Racetrack Referee", ai);
        addCard("Mountain", ai);
        addCard("Mountain", ai);
        addCard("Mountain", ai);
        final Card flash = addCardToZone("Faithless Looting", ai, ZoneType.Graveyard);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final SpellAbility free = flash.getFirstSpellAbility().copyWithNoManaCost();
        free.setActivatingPlayer(ai);
        AssertJUnit.assertTrue("a free cast", free.isValid("Spell.AlternativeOrFree", ai, flash, null));
        AssertJUnit.assertFalse(flash.getFirstSpellAbility().isValid("Spell.AlternativeOrFree", ai, flash, null));
        AssertJUnit.assertTrue("no casting without paying the mana cost",
                forge.game.staticability.StaticAbilityCantBeCast.cantBeCastAbility(free, flash, ai));
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        final SpellAbility cast = bolt.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        AssertJUnit.assertTrue("paying the mana cost is fine", cast.canPlay());
    }

    @Test
    public void testInspectorInspector() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("The Inspector Inspector", ai);
        final Card giant = addCardToZone("Hill Giant", ai, ZoneType.Hand);
        game.getAction().moveToPlay(giant, ai, null, null);
        final Card entered = game.getCardState(giant);
        AssertJUnit.assertEquals("Thraben Inspector", entered.getName());
        AssertJUnit.assertEquals(1, entered.getNetPower());
    }

    @Test
    public void testKylemAllStar() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card kylem = addCard("Kylem All-Star", ai);
        final SpellAbility etb = kylem.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        AbilityUtils.resolve(etb);
        game.getAction().checkStaticAbilities();
        final Card glitter = ai.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getName().equals("All That Glitters")).findFirst().orElseThrow();
        AssertJUnit.assertEquals(kylem, glitter.getAttachedTo());
        AssertJUnit.assertTrue("enchantments are Gold artifacts", glitter.isArtifact() && glitter.getType().hasSubtype("Gold"));
        AssertJUnit.assertEquals("one artifact and/or enchantment", 3, kylem.getNetPower());
    }

    @Test
    public void testRatatwotwoChefRole() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", ai);
        final Card rat = addCard("Ratatwotwo", ai);
        final SpellAbility etb = rat.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        etb.getTargets().add(bears);
        AbilityUtils.resolve(etb);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertEquals("Chef Role +1/+1", 3, bears.getNetPower());
        AssertJUnit.assertTrue(bears.isValid("Creature.YouCtrl+EnchantedBy Role.namedChef", ai, rat, null));
    }

    @Test
    public void testRobbyGrantsSquad() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Robby, Another Dwarf", ai);
        final Card dwarf = addCardToZone("Dwarven Trader", ai, ZoneType.Hand);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertTrue(dwarf.hasKeyword(Keyword.SQUAD));
    }
}
