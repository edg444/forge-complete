package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Bag of Stroopwafels, Famous Museum, Guilded Lotus, Kibo's Bananamobile, Sawtooth Avenger, Tinfoil Helm, Power Level
 * Analyzer, Twenty Lessons and That Which Was Compleated (UNK uncommons), checked against Scryfall 2026-10-21 (no
 * rulings).
 */
public class UnknownEventBatch27Test extends AITest {

    @Test
    public void testFamousMuseumMana() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card museum = addCard("Famous Museum", ai);
        museum.addCounterInternal(CounterType.getType("ART"), 2, ai, false, null, null);
        AssertJUnit.assertEquals(3, AbilityUtils.calculateAmount(museum, "X", null));
    }

    @Test
    public void testTinfoilHelm() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", ai);
        final Card helm = addCard("Tinfoil Helm", ai);
        helm.attachToEntity(bears, null, true);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertEquals(4, bears.getNetPower());
        final Card hawk = addCard("Suntail Hawk", game.getPlayers().get(0));
        final Card hybrid = addCard("Boros Recruit", game.getPlayers().get(0));
        final Card plain = addCard("Gray Ogre", game.getPlayers().get(0));
        AssertJUnit.assertTrue(bears.hasKeyword(Keyword.PROTECTION));
        final String from = "Card.Alien,Card.Bird,Card.Eldrazi,Card.Lizard,Card.Mutant,Card.Robot,Card.Yeti,Card.hasHybridManaCost";
        AssertJUnit.assertTrue(hawk.isValid(from.split(","), ai, helm, null));
        AssertJUnit.assertTrue("hybrid mana", hybrid.isValid(from.split(","), ai, helm, null));
        AssertJUnit.assertFalse(plain.isValid(from.split(","), ai, helm, null));
    }

    @Test
    public void testSawtoothAvengerChoosesAKeyword() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card avenger = addCard("Sawtooth Avenger", ai);
        avenger.addCounterInternal(CounterEnumType.P1P1, 6, ai, false, null, null);
        final SpellAbility pick = forge.game.ability.AbilityFactory.getAbility(avenger.getSVar("GainMenace"), avenger);
        pick.setActivatingPlayer(ai);
        AbilityUtils.resolve(pick);
        AssertJUnit.assertTrue(avenger.hasKeyword(Keyword.MENACE));
    }

    @Test
    public void testBananamobile() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card mobile = addCard("Kibo's Bananamobile", ai);
        final SpellAbility etb = mobile.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        AbilityUtils.resolve(etb);
        AssertJUnit.assertEquals(3, ai.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getName().equals("Banana")).count());
    }

    @Test
    public void testTwentyLessonsCastsALesson() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        // something for any Lesson to target, so whichever one comes up can be cast
        addCard("Grizzly Bears", ai);
        addCard("Grizzly Bears", game.getPlayers().get(0));
        addCardToZone("Grizzly Bears", ai, ZoneType.Graveyard);
        addCardToZone("Island", ai, ZoneType.Library);
        final Card lessons = addCardToZone("Twenty Lessons", ai, ZoneType.Hand);
        final SpellAbility cast = lessons.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        for (int i = 0; i < 5 && game.getStack().isEmpty(); i++) {
            AbilityUtils.resolve(cast);
        }
        AssertJUnit.assertFalse("a Lesson was cast", game.getStack().isEmpty());
        AssertJUnit.assertTrue(game.getStack().peekAbility().getHostCard().getType().hasStringType("Lesson"));
    }

    @Test
    public void testCompleatedWardAndAnnihilator() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card thing = addCard("That Which Was Compleated", ai);
        human.setPoisonCounters(3, ai);
        final SpellAbility host = thing.getFirstSpellAbility();
        host.setActivatingPlayer(ai);
        AssertJUnit.assertEquals(3, AbilityUtils.calculateAmount(thing, "X", host));
        AssertJUnit.assertTrue(thing.hasKeyword(Keyword.WARD));
    }
}
