package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CounterEnumType;
import forge.game.combat.Combat;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

/**
 * Mystery Booster 2 playtest cards (MB2), part 8, checked against Scryfall 2026-10-22 with their rulings.
 */
public class MysteryBooster2Playtest8Test extends AITest {

    private void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    private void cast(final Game game, final Player p, final Card spell) {
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        final Card onStack = game.getAction().moveToStack(spell, sa);
        sa.setHostCard(onStack);
        game.getStack().add(sa);
        resolveAll(game);
    }

    private void lands(final Player p, final String name, final int n) {
        for (int i = 0; i < n; i++) {
            addCard(name, p);
        }
    }

    @Test
    public void testFlavorDisasterNegamorph() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card bears = addCard("Grizzly Bears", ai);
        lands(ai, "Forest", 2);
        final Card disaster = addCard("Flavor Disaster", ai);
        AssertJUnit.assertTrue(disaster.hasKeyword(Keyword.NEGAMORPH));
        disaster.turnFaceDown(true);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(2, disaster.getNetPower());
        final SpellAbility up = disaster.getAllPossibleAbilities(ai, true).stream()
                .filter(s -> s.isKeyword(Keyword.NEGAMORPH) && s.getApi() != null).findFirst().orElse(null);
        AssertJUnit.assertNotNull("can be turned face up for its negamorph cost", up);
        up.setActivatingPlayer(ai);
        AbilityUtils.resolve(up);
        resolveAll(game);
        AssertJUnit.assertFalse(disaster.isFaceDown());
        AssertJUnit.assertEquals("a -1/-1 counter", 1, disaster.getCounters(CounterEnumType.M1M1));
        AssertJUnit.assertTrue("the turned-face-up trigger pumped something: " + bears.getNetPower() + "/" + disaster.getNetPower(),
                bears.getNetPower() == 5 || disaster.getNetPower() == 6);
    }

    @Test
    public void testImmersturmBattlefieldHost() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card realm = addCard("Immersturm Battlefield", ai);
        final Card giant = addCard("Hill Giant", ai);
        final SpellAbility host = realm.getSpellAbilities().stream().filter(s -> "Host".equals(s.getParam("PrecostDesc"))).findFirst().orElseThrow();
        host.setActivatingPlayer(ai);
        host.getTargets().add(giant);
        AbilityUtils.resolve(host);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(5, giant.getNetPower());
        AssertJUnit.assertTrue(giant.hasKeyword(Keyword.HASTE));
        game.getAction().moveToHand(giant, null);
        resolveAll(game);
        final Card again = addCard("Hill Giant", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("hosting ends when it leaves", 3, again.getNetPower());
    }

    @Test
    public void testVuzzleSpaceship() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card ship = addCardToZone("Vuzzle Spaceship", ai, ZoneType.Hand);
        cast(game, ai, ship);
        final Card now = game.getCardState(ship);
        AssertJUnit.assertEquals(6, now.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertTrue(now.hasKeyword(Keyword.FLYING));
        final Player opp = game.getPlayers().get(0);
        final Card bolt = addCardToZone("Lightning Bolt", opp, ZoneType.Hand);
        final SpellAbility boltSa = bolt.getFirstSpellAbility();
        boltSa.setActivatingPlayer(opp);
        boltSa.getTargets().add(now);
        final Card boltOnStack = game.getAction().moveToStack(bolt, boltSa);
        boltSa.setHostCard(boltOnStack);
        game.getStack().add(boltSa);
        resolveAll(game);
        AssertJUnit.assertEquals("counters removed instead of damage", 3, now.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(0, now.getDamage());
        AssertJUnit.assertFalse("thrusters off", now.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertTrue(now.isInPlay());
    }

    @Test
    public void testFludgeShufflesGunk() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        cast(game, ai, addCardToZone("Fludge, Gunk Guardian", ai, ZoneType.Hand));
        final long gunk = CardLists.count(opp.getCardsIn(ZoneType.Library), c -> c.getName().equals("Gunk"));
        AssertJUnit.assertEquals(3, gunk);
        final Card one = CardLists.filter(opp.getCardsIn(ZoneType.Library), c -> c.getName().equals("Gunk")).getFirst();
        AssertJUnit.assertFalse("a card, not a token", one.isToken());
        AssertJUnit.assertEquals(opp, one.getOwner());
        AssertJUnit.assertTrue(one.hasKeyword(Keyword.CYCLING));
        AssertJUnit.assertEquals(0, CardLists.count(ai.getCardsIn(ZoneType.Library), c -> c.getName().equals("Gunk")));
        cast(game, ai, addCardToZone("Mold Shambler", ai, ZoneType.Hand));
        AssertJUnit.assertEquals("a Fungus too", 6, CardLists.count(opp.getCardsIn(ZoneType.Library), c -> c.getName().equals("Gunk")));
    }

    @Test
    public void testGladeOfThePumpSpells() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card glade = addCardToZone("Glade of the Pump Spells", ai, ZoneType.Hand);
        AssertJUnit.assertEquals(3, glade.getCMC());
        AssertJUnit.assertTrue(glade.isGreen());
        final SpellAbility play = glade.getAllPossibleAbilities(ai, true).stream().filter(SpellAbility::isLandAbility).findFirst().orElseThrow();
        play.setActivatingPlayer(ai);
        play.resolve();
        AssertJUnit.assertTrue("no mana, no land play", game.getCardState(glade).isInZone(ZoneType.Hand));
        AssertJUnit.assertEquals(0, ai.getLandsPlayedThisTurn());
        lands(ai, "Forest", 3);
        final Card bears = addCard("Grizzly Bears", ai);
        final SpellAbility paid = glade.getAllPossibleAbilities(ai, true).stream().filter(SpellAbility::isLandAbility).findFirst().orElseThrow();
        paid.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(paid.canPlay());
        paid.resolve();
        resolveAll(game);
        AssertJUnit.assertTrue(game.getCardState(glade).isInPlay());
        AssertJUnit.assertEquals("the forests paid for it", 3, CardLists.count(ai.getCardsIn(ZoneType.Battlefield), c -> c.isLand() && c.isTapped()));
        AssertJUnit.assertEquals(4, bears.getNetPower());
    }

    @Test
    public void testPyromancy101Teach() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card student = addCard("Grizzly Bears", ai);
        student.setSickness(false);
        final Card pyro = addCardToZone("Pyromancy 101", ai, ZoneType.Hand);
        final SpellAbility sa = pyro.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        sa.getTargets().add(opp);
        final Card onStack = game.getAction().moveToStack(pyro, sa);
        sa.setHostCard(onStack);
        game.getStack().add(sa);
        resolveAll(game);
        AssertJUnit.assertEquals(19, opp.getLife());
        final Card exiled = game.getCardState(pyro);
        AssertJUnit.assertTrue("exiled, not in the graveyard: " + exiled.getZone(), exiled.isInZone(ZoneType.Exile));
        AssertJUnit.assertEquals(student, exiled.getTaughtTo());
        game.getAction().checkStateEffects(true);
        final SpellAbility taught = student.getSpellAbilities().stream().filter(s -> s.getDescription().contains("Copy the exiled card")).findFirst().orElse(null);
        AssertJUnit.assertNotNull("the student has the ability", taught);
        final Card bystander = addCard("Hill Giant", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue("only the student", bystander.getSpellAbilities().stream().noneMatch(s -> s.getDescription().contains("Copy the exiled card")));
    }

    @Test
    public void testPyromancy101TaughtToNobody() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card pyro = addCardToZone("Pyromancy 101", ai, ZoneType.Hand);
        final SpellAbility sa = pyro.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        sa.getTargets().add(opp);
        final Card onStack = game.getAction().moveToStack(pyro, sa);
        sa.setHostCard(onStack);
        game.getStack().add(sa);
        resolveAll(game);
        final Card exiled = game.getCardState(pyro);
        AssertJUnit.assertTrue(exiled.isInZone(ZoneType.Exile));
        AssertJUnit.assertNull(exiled.getTaughtTo());
    }

    @Test
    public void testZoneOfFlame() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card zone = addCardToZone("Zone of Flame", ai, ZoneType.Hand);
        cast(game, ai, zone);
        final Card now = game.getCardState(zone);
        AssertJUnit.assertTrue("an Aura enchanting a zone stays: " + now.getZone(), now.isInPlay());
        AssertJUnit.assertEquals(ZoneType.Stack, now.getEnchantedZone());
        final int life = opp.getLife();
        cast(game, ai, addCardToZone("Grizzly Bears", ai, ZoneType.Hand));
        AssertJUnit.assertTrue("onto the stack and off it: " + opp.getLife(), opp.getLife() <= life - 1);
    }

    @Test
    public void testMeanderedTowershell() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card bears = addCard("Grizzly Bears", ai);
        bears.setSickness(false);
        final Card shell = addCard("Meandered Towershell", ai);
        shell.attachToEntity(bears, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(bears.hasKeyword("Landwalk:Island"));
        Trigger meander = null;
        for (final Trigger t : bears.getTriggers()) {
            if (t.getParam("TriggerDescription").startsWith("Whenever this creature attacks, exile it")) {
                meander = t;
            }
        }
        AssertJUnit.assertNotNull(meander);
        final SpellAbility exile = meander.ensureAbility();
        exile.setActivatingPlayer(ai);
        AbilityUtils.resolve(exile);
        resolveAll(game);
        AssertJUnit.assertTrue(game.getCardState(bears).isInZone(ZoneType.Exile));
        AssertJUnit.assertTrue(game.getCardState(shell).isInZone(ZoneType.Exile));
        // "on your next turn": activated as that turn begins
        game.getTriggerHandler().handlePlayerDefinedDelTriggers(ai);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, ai);
        final Combat combat = new Combat(ai);
        game.getPhaseHandler().setCombat(combat);
        game.getTriggerHandler().runTrigger(TriggerType.Phase, AbilityKey.mapFromPlayer(ai), false);
        resolveAll(game);
        final Card back = game.getCardState(bears);
        AssertJUnit.assertTrue("back: " + back.getZone(), back.isInPlay());
        AssertJUnit.assertTrue(back.isTapped());
        AssertJUnit.assertTrue("attacking", combat.isAttacking(back));
        AssertJUnit.assertEquals("the Aura on it again", back, game.getCardState(shell).getAttachedTo());
        AssertJUnit.assertEquals(opp, combat.getDefenderPlayerByAttacker(back));
    }
}
