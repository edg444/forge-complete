package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.effects.ChooseTeamEffect;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Unclaimed Blessing, Life at Stake, Mark Ritual, Long-Term Phyresis Study, Common Curve Filler and May the Mana Be
 * with You (UNK CB11-CC03), checked against Scryfall 2026-10-21 (no rulings).
 */
public class UnknownEventBatch4Test extends AITest {

    private static void runPreOpeningHandActions(final Game game, final Player p) throws Exception {
        final java.lang.reflect.Method pre = game.getAction().getClass()
                .getDeclaredMethod("runPreOpeningHandActions", Player.class);
        pre.setAccessible(true);
        pre.invoke(game.getAction(), p);
    }

    @Test
    public void testUnclaimedBlessingTeams() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", ai);
        ai.setMatchNote(ChooseTeamEffect.NOTE, ChooseTeamEffect.MIRRAN);
        final Card blessing = addCardToZone("Unclaimed Blessing", ai, ZoneType.Hand);
        final SpellAbility sa = blessing.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        sa.getTargets().add(bears);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertEquals(1, bears.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertTrue(bears.hasKeyword(Keyword.LIFELINK));
        AssertJUnit.assertFalse(bears.hasKeyword(Keyword.DEATHTOUCH));

        ai.setMatchNote(ChooseTeamEffect.NOTE, ChooseTeamEffect.PHYREXIAN);
        final Card second = addCardToZone("Unclaimed Blessing", ai, ZoneType.Hand);
        final SpellAbility sa2 = second.getFirstSpellAbility();
        sa2.setActivatingPlayer(ai);
        sa2.getTargets().add(bears);
        AbilityUtils.resolve(sa2);
        AssertJUnit.assertEquals(1, bears.getCounters(CounterEnumType.M1M1));
        AssertJUnit.assertTrue(bears.hasKeyword(Keyword.DEATHTOUCH));
    }

    @Test
    public void testTeamChosenOnceAndKept() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card blessing = addCardToZone("Unclaimed Blessing", ai, ZoneType.Hand);
        final SpellAbility sa = blessing.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        final String team = ChooseTeamEffect.chooseIfNeeded(ai, sa);
        AssertJUnit.assertNotNull(team);
        for (int i = 0; i < 10; i++) {
            AssertJUnit.assertEquals(team, ChooseTeamEffect.chooseIfNeeded(ai, sa));
        }
        AssertJUnit.assertEquals(1, AbilityUtils.calculateAmount(blessing, "Count$UnknownTeam." + team, sa));
    }

    @Test
    public void testLifeAtStakeAiBids() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", ai);
        final Card stake = addCardToZone("Life at Stake", ai, ZoneType.Hand);
        final SpellAbility sa = stake.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        sa.getTargets().add(bears);
        final int life = ai.getLife();
        AbilityUtils.resolve(sa);
        // only the AI chose (it controls the creature too): it was highest, so the creature is exiled
        AssertJUnit.assertTrue(game.getCardState(bears).isInZone(ZoneType.Exile));
        AssertJUnit.assertTrue("lost the number it chose", ai.getLife() <= life);
        AssertJUnit.assertEquals(20, human.getLife());
    }

    @Test
    public void testMarkRitualSignedIsRed() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card ritual = addCardToZone("Mark Ritual", ai, ZoneType.Hand);
        AssertJUnit.assertTrue(ritual.getColor().hasBlack());
        ritual.signByDesigner();
        AssertJUnit.assertTrue(ritual.isSigned());
        AssertJUnit.assertTrue(ritual.getColor().hasRed());
        AssertJUnit.assertFalse(ritual.getColor().hasBlack());
        AssertJUnit.assertEquals("{1}{R}", ritual.getManaCost().getSimpleString());
        final SpellAbility sa = ritual.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertEquals(3, ai.getManaPool().getAmountOfColor(forge.card.MagicColor.RED));
    }

    @Test
    public void testMarkRitualAiUnsigned() throws Exception {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card ritual = addCardToZone("Mark Ritual", ai, ZoneType.Library);
        runPreOpeningHandActions(game, ai);
        AssertJUnit.assertFalse(ritual.isSigned());
        AssertJUnit.assertTrue(ritual.getColor().hasBlack());
    }

    @Test
    public void testLongTermPhyresisStudy() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final int base = ai.getPoisonCountersToLose();
        addCard("Long-Term Phyresis Study", ai);
        AssertJUnit.assertEquals(base + 1, ai.getPoisonCountersToLose());
    }

    @Test
    public void testCommonCurveFiller() throws Exception {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCardToZone("Grizzly Bears", ai, ZoneType.Library);
        addCardToZone("Hill Giant", ai, ZoneType.Library);
        addCardToZone("Serra Angel", ai, ZoneType.Library);
        final Card filler = addCardToZone("Common Curve Filler", ai, ZoneType.Library);
        runPreOpeningHandActions(game, ai);
        AssertJUnit.assertEquals("the empty slot from two to five: 3", 3, filler.getCMC());
        AssertJUnit.assertEquals(3, filler.getNetPower());
        AssertJUnit.assertEquals(3, filler.getNetToughness());
        AssertJUnit.assertEquals(ManaCost.get(3).getSimpleString(), filler.getManaCost().getSimpleString());
    }

    @Test
    public void testMayTheManaBeWithYou() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card spell = addCardToZone("May the Mana Be with You", ai, ZoneType.Hand);
        AssertJUnit.assertTrue(spell.hasKeyword(Keyword.DEMONSTRATE));
        final SpellAbility grave = spell.getFirstSpellAbility().getAdditionalAbilityList("Choices").stream()
                .filter(s -> "Watery Grave".equals(s.getParam("DefinedName"))).findFirst().orElseThrow();
        grave.setActivatingPlayer(ai);
        AbilityUtils.resolve(grave);
        final Card token = ai.getCardsIn(ZoneType.Battlefield).stream().filter(Card::isToken).findFirst().orElseThrow();
        AssertJUnit.assertEquals("Watery Grave", token.getName());
        AssertJUnit.assertTrue(token.getType().hasSubtype("Island"));
        AssertJUnit.assertTrue(token.getType().hasSubtype("Swamp"));
    }
}
