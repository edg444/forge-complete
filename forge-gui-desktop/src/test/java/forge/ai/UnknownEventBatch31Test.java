package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.effects.ChooseTeamEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CounterEnumType;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Tax Draw, Tax Sweeper, Tax Hounds, Devoted Jeskai, Tricky Mage, Team Mage, Dialogue Tree, Good Knight, Rosewater's
 * Nemesis, Echoing Echo, Out of Town, Brave Falconhawk, Rideable Mantis, Xerex Squire and Farseeing Flockmate (UNK
 * uncommons), checked against Scryfall 2026-10-22 (no rulings).
 */
public class UnknownEventBatch31Test extends AITest {

    @Test
    public void testTaxCycle() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card commander = addCardToZone("Grizzly Bears", ai, ZoneType.Command);
        ai.addCommander(commander);
        ai.incCommanderCast(commander);
        for (final String name : new String[] {"Tax Draw", "Tax Sweeper", "Tax Hounds"}) {
            final Card card = addCardToZone(name, ai, ZoneType.Hand);
            final SpellAbility cast = card.getFirstSpellAbility();
            cast.setActivatingPlayer(ai);
            AssertJUnit.assertEquals(name, 2, AbilityUtils.calculateAmount(card, "X", cast));
        }
    }

    @Test
    public void testDevotionToJeskai() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card jeskai = addCard("Devoted Jeskai", ai);
        addCard("Ajani Vengeant", ai);
        addCard("Llanowar Elves", ai);
        final SpellAbility etb = jeskai.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        AssertJUnit.assertEquals("Jeskai's {U} and Ajani's {R}{W}, not the Elves' {G}", 3, AbilityUtils.calculateAmount(jeskai, "X", etb));
    }

    @Test
    public void testTrickyMageByTeam() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        ai.setMatchNote(ChooseTeamEffect.NOTE, ChooseTeamEffect.PHYREXIAN);
        addCardToZone("Ornithopter", ai, ZoneType.Library);
        final Card big = addCardToZone("Wurmcoil Engine", ai, ZoneType.Library);
        final Card mage = addCard("Tricky Mage", ai);
        final SpellAbility etb = mage.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        AbilityUtils.resolve(etb);
        AssertJUnit.assertTrue(big.isInZone(ZoneType.Hand));
        AssertJUnit.assertEquals(1, ai.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void testTeamMageByRaceTeam() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        ai.setMatchNote(ChooseTeamEffect.RACE_NOTE, ChooseTeamEffect.SPEED_DEMONS);
        final Card small = addCardToZone("Ornithopter", ai, ZoneType.Library);
        addCardToZone("Wurmcoil Engine", ai, ZoneType.Library);
        final Card mage = addCard("Team Mage", ai);
        final SpellAbility etb = mage.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        AbilityUtils.resolve(etb);
        AssertJUnit.assertTrue(small.isInZone(ZoneType.Hand));
        AssertJUnit.assertEquals(1, ai.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void testDialogueTreeRepeatsItself() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card tree = addCardToZone("Dialogue Tree", ai, ZoneType.Graveyard);
        final SpellAbility exile = tree.getFirstSpellAbility().getSubAbility().getAdditionalAbility("Execute")
                .getAdditionalAbilityList("Choices").get(1);
        exile.setActivatingPlayer(ai);
        AbilityUtils.resolve(exile);
        AssertJUnit.assertTrue(tree.isInZone(ZoneType.Exile) || game.getCardState(tree).isInZone(ZoneType.Exile));
    }

    @Test
    public void testGoodKnightAndNemesis() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        ai.setMatchNote(ChooseTeamEffect.NOTE, ChooseTeamEffect.PHYREXIAN);
        final Card knight = addCardToZone("Good Knight", ai, ZoneType.Hand);
        addCard("Plains", ai);
        addCard("Plains", ai);
        game.getPhaseHandler().devModeSet(forge.game.phase.PhaseType.MAIN1, ai);
        final SpellAbility cast = knight.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(forge.game.staticability.StaticAbilityCantBeCast.cantBeCastAbility(cast, knight, ai));
        ai.setMatchNote(ChooseTeamEffect.NOTE, ChooseTeamEffect.MIRRAN);
        AssertJUnit.assertFalse(forge.game.staticability.StaticAbilityCantBeCast.cantBeCastAbility(cast, knight, ai));
        final int base = ai.getPoisonCountersToLose();
        addCard("Rosewater's Nemesis", ai);
        AssertJUnit.assertEquals(base + 3, ai.getPoisonCountersToLose());
    }

    @Test
    public void testEchoingEchoSharesByName() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", human);
        final Card otherBears = addCard("Grizzly Bears", ai);
        final Card elves = addCard("Llanowar Elves", human);
        final Card echo = addCardToZone("Echoing Echo", ai, ZoneType.Hand);
        final SpellAbility cast = echo.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        cast.getTargets().add(bears);
        AbilityUtils.resolve(cast);
        AssertJUnit.assertTrue(bears.hasKeyword(Keyword.ECHO));
        AssertJUnit.assertTrue(otherBears.hasKeyword(Keyword.ECHO));
        AssertJUnit.assertFalse(elves.hasKeyword(Keyword.ECHO));
    }

    @Test
    public void testOutOfTownLeavesWhenATownArrives() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", human);
        final Card out = addCard("Out of Town", ai);
        final SpellAbility etb = out.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        etb.getTargets().add(bears);
        AbilityUtils.resolve(etb);
        AssertJUnit.assertTrue(game.getCardState(bears).isInZone(ZoneType.Exile));
        final Card inn = addCard("Adventurer's Inn", human);
        AssertJUnit.assertTrue(inn.isValid("Town.ControlledBy RememberedOwner", ai, out, null));
        final Card myInn = addCard("Adventurer's Inn", ai);
        AssertJUnit.assertFalse(myInn.isValid("Town.ControlledBy RememberedOwner", ai, out, null));
    }

    @Test
    public void testBraveFalconhawkBall() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card hawk = addCard("Brave Falconhawk", ai);
        final SpellAbility etb = hawk.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        AbilityUtils.resolve(etb);
        final Card ball = ai.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getName().equals("Contested Game Ball")).findFirst().orElseThrow();
        AssertJUnit.assertTrue(ball.isToken());
        ball.addCounterInternal(CounterEnumType.POINT, 3, ai, false, null, null);
        final SpellAbility attack = hawk.getTriggers().get(1).ensureAbility();
        attack.setActivatingPlayer(ai);
        AssertJUnit.assertEquals(3, AbilityUtils.calculateAmount(hawk, "X", attack));
    }

    @Test
    public void testRideableMantisSaddlers() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card mantis = addCard("Rideable Mantis", ai);
        final Card blue = addCard("Coral Merfolk", ai);
        mantis.addSaddledByThisTurn(new CardCollection(blue));
        final SpellAbility attack = mantis.getTriggers().get(0).ensureAbility();
        attack.setActivatingPlayer(ai);
        AssertJUnit.assertEquals(1, AbilityUtils.calculateAmount(mantis, "Blue", attack));
        AssertJUnit.assertEquals(0, AbilityUtils.calculateAmount(mantis, "Red", attack));
    }

    @Test
    public void testXerexSquireHypotenuse() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card squire = addCard("Xerex Squire", ai);
        final Card wurm = addCard("Craw Wurm", ai);
        squire.setChosenCards(new CardCollection(wurm));
        final SpellAbility attack = squire.getTriggers().get(0).ensureAbility();
        attack.setActivatingPlayer(ai);
        AssertJUnit.assertEquals("6/4: sqrt(52) = 7.2, rounded up", 8, AbilityUtils.calculateAmount(squire, "X", attack));
        final Card bears = addCard("Grizzly Bears", ai);
        squire.setChosenCards(new CardCollection(bears));
        AssertJUnit.assertEquals("2/2: sqrt(8) = 2.8", 3, AbilityUtils.calculateAmount(squire, "X", attack));
    }

    @Test
    public void testFarseeingFlockmateBacksUp() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card flock = addCard("Farseeing Flockmate", ai);
        final Card bears = addCard("Grizzly Bears", ai);
        final SpellAbility etb = flock.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        etb.getTargets().add(bears);
        AbilityUtils.resolve(etb);
        AssertJUnit.assertEquals(1, bears.getCounters(forge.game.card.CounterType.getType("Flying")));
        AssertJUnit.assertTrue(bears.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertTrue(bears.getTriggers().anyMatch(t -> t.getParam("TriggerDescription").contains("surveil")));
    }
}
