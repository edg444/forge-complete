package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CounterType;
import forge.game.cost.CostAdjustment;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityCastAs;
import forge.game.zone.ZoneType;

/**
 * Second Stage of Magic Design, Untap, Upkeep, Draw, The Countering Runner, The Fearsome Flock, Lorthos, Tentacled
 * Terror, The Fish Brewer and Fourth Stage of Magic Design (UNK blue rares), checked against Scryfall 2026-10-22 (no
 * rulings).
 */
public class UnknownEventBatch44Test extends AITest {

    private void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    private static int reducedCost(final Card c, final Player p) {
        final SpellAbility cast = c.getFirstSpellAbility();
        cast.setActivatingPlayer(p);
        final ManaCostBeingPaid cost = new ManaCostBeingPaid(cast.getPayCosts().getTotalMana());
        CostAdjustment.adjust(cost, cast, p, null, true, false);
        return cost.getConvertedManaCost();
    }

    @Test
    public void testSecondStageSharedKeywords() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card stage = addCard("Second Stage of Magic Design", ai);
        addCard("Wind Drake", ai);
        final String valid = "Card.unsharedKeywordOrAbilityWord";
        AssertJUnit.assertFalse("shares flying", addCardToZone("Air Elemental", ai, ZoneType.Hand).isValid(valid, ai, stage, null));
        AssertJUnit.assertFalse("no keyword", addCardToZone("Grizzly Bears", ai, ZoneType.Hand).isValid(valid, ai, stage, null));
        AssertJUnit.assertFalse("only mentions flying", addCardToZone("Plummet", ai, ZoneType.Hand).isValid(valid, ai, stage, null));
        AssertJUnit.assertTrue("haste is new", addCardToZone("Raging Goblin", ai, ZoneType.Hand).isValid(valid, ai, stage, null));
        final Card cobra = addCardToZone("Lotus Cobra", ai, ZoneType.Hand);
        AssertJUnit.assertTrue("landfall is new", cobra.isValid(valid, ai, stage, null));
        addCardToZone("Steppe Lynx", ai, ZoneType.Graveyard);
        AssertJUnit.assertFalse("a card in the graveyard has landfall", cobra.isValid(valid, ai, stage, null));
    }

    @Test
    public void testUntapUpkeepDraw() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card spell = addCardToZone("Untap, Upkeep, Draw", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        for (final String mode : new String[] {"DBUntap", "DBDraw"}) {
            final SpellAbility sa = AbilityFactory.getAbility(spell.getSVar(mode), spell);
            sa.setActivatingPlayer(ai);
            AbilityUtils.resolve(sa);
        }
        AssertJUnit.assertTrue("the newest extra step comes first", game.getPhaseHandler().hasExtraPhaseAfter(PhaseType.MAIN1, PhaseType.DRAW));
        addCardToZone("Island", ai, ZoneType.Library);
        addCardToZone("Island", ai, ZoneType.Library);
        addCardToZone("Island", ai, ZoneType.Library);
        addCardToZone("Island", ai, ZoneType.Library);
        final SpellAbility draw = AbilityFactory.getAbility(spell.getSVar("DBDraw"), spell);
        draw.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(new forge.ai.ability.AddPhaseAi().canPlayWithSubs(ai, draw).willingToPlay());
    }

    @Test
    public void testCounteringRunner() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card runner = addCard("The Countering Runner", ai);
        AssertJUnit.assertEquals(0, reducedCost(addCardToZone("Counterspell", ai, ZoneType.Hand), ai));
        AssertJUnit.assertEquals("CR 118.7c: the extra {U} comes off the generic", 0, reducedCost(addCardToZone("Mana Leak", ai, ZoneType.Hand), ai));
        AssertJUnit.assertEquals(2, reducedCost(addCardToZone("Grizzly Bears", ai, ZoneType.Hand), ai));

        addCardToZone("Island", ai, ZoneType.Library);
        final Card bolt = addCardToZone("Lightning Bolt", human, ZoneType.Hand);
        final SpellAbility countered = bolt.getFirstSpellAbility();
        countered.setActivatingPlayer(human);
        final SpellAbility trig = runner.getTriggers().get(0).ensureAbility();
        trig.setActivatingPlayer(ai);
        trig.setTriggeringObject(AbilityKey.SpellAbility, countered);
        AbilityUtils.resolve(trig);
        AssertJUnit.assertEquals(1, ai.getCardsIn(ZoneType.Hand).size() - 3);
        AssertJUnit.assertEquals("no loss of life", 20, human.getLife());
        AssertJUnit.assertEquals(2, human.getPsychicDamage());
        human.setPsychicDamage(18);
        AbilityUtils.resolve(trig);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue("20 psychic damage", human.hasLost());
    }

    @Test
    public void testFearsomeFlock() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card flock = addCard("The Fearsome Flock", ai);
        final SpellAbility levelUp = flock.getSpellAbilities().stream().filter(SpellAbility::isActivatedAbility).findFirst().orElseThrow();
        AssertJUnit.assertTrue(levelUp.hasParam("SplitSecond"));
        AssertJUnit.assertTrue(levelUp.toString().startsWith("Split second level up"));

        final Card tome = addCard("Jalum Tome", human);
        final SpellAbility loot = tome.getSpellAbilities().stream().filter(SpellAbility::isActivatedAbility).findFirst().orElseThrow();
        loot.setActivatingPlayer(ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertFalse(loot.getRestrictions().checkActivatorRestrictions(tome, loot));
        flock.addCounterInternal(CounterType.getType("LEVEL"), 4, ai, false, null, null);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertTrue(loot.getRestrictions().checkActivatorRestrictions(tome, loot));
        AssertJUnit.assertEquals(4, flock.getNetToughness());

        flock.subtractCounter(CounterType.getType("LEVEL"), 3, ai);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertEquals(3, flock.getNetToughness());
        final SpellAbility steal = AbilityFactory.getAbility(flock.getSVar("DBSteal"), flock);
        steal.setActivatingPlayer(ai);
        steal.getTargets().add(tome);
        AbilityUtils.resolve(steal);
        AssertJUnit.assertEquals(ai, tome.getController());
        AssertJUnit.assertTrue(tome.getType().hasSubtype("Treasure"));
        AssertJUnit.assertFalse("keeps its abilities, gains no mana ability", tome.getManaAbilities().size() > 0);
        AssertJUnit.assertTrue(tome.getSpellAbilities().anyMatch(SpellAbility::isActivatedAbility));
    }

    @Test
    public void testLorthosMonoEminence() throws Exception {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card lorthos = addCardToZone("Lorthos, Tentacled Terror", ai, ZoneType.Command);
        lorthos.setCommander(true);
        ai.addCommander(lorthos);
        addCardToZone("Island", ai, ZoneType.Library);
        final java.lang.reflect.Method pre = game.getAction().getClass().getDeclaredMethod("runPreOpeningHandActions", Player.class);
        pre.setAccessible(true);
        pre.invoke(game.getAction(), ai);
        AssertJUnit.assertEquals(8, ai.getStartingHandSize());
        AssertJUnit.assertEquals(8, ai.getMaxHandSize());
    }

    @Test
    public void testLorthosNotMono() throws Exception {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card lorthos = addCardToZone("Lorthos, Tentacled Terror", ai, ZoneType.Command);
        lorthos.setCommander(true);
        ai.addCommander(lorthos);
        addCardToZone("Mountain", ai, ZoneType.Library);
        addCardToZone("Lightning Bolt", ai, ZoneType.Library);
        final java.lang.reflect.Method pre = game.getAction().getClass().getDeclaredMethod("runPreOpeningHandActions", Player.class);
        pre.setAccessible(true);
        pre.invoke(game.getAction(), ai);
        AssertJUnit.assertEquals(7, ai.getStartingHandSize());
    }

    @Test
    public void testLorthosAttackTapsAll() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card lorthos = addCard("Lorthos, Tentacled Terror", ai);
        final Card bears = addCard("Grizzly Bears", human);
        final Card forest = addCard("Forest", human);
        final SpellAbility tap = lorthos.getTriggers().get(0).ensureAbility();
        tap.setActivatingPlayer(ai);
        AbilityUtils.resolve(tap);
        AssertJUnit.assertTrue(bears.isTapped() && forest.isTapped());
    }

    @Test
    public void testFishBrewerTapsFish() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("The Fish Brewer", ai);
        addCard("Soul Warden", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card brewer = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getName().equals("The Fish Brewer")).get(0);
        final SpellAbility token = AbilityFactory.getAbility(brewer.getSVar("TrigToken"), brewer);
        token.setActivatingPlayer(ai);
        AbilityUtils.resolve(token);
        AbilityUtils.resolve(token);
        resolveAll(game);
        final java.util.List<Card> fish = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getType().hasCreatureType("Fish"));
        AssertJUnit.assertEquals(2, fish.size());
        AssertJUnit.assertEquals("all colors", 5, fish.get(0).getColor().countColors());
        final int life = ai.getLife();
        fish.forEach(f -> f.setTapped(false));
        game.getAction().moveToPlay(addCardToZone("Grizzly Bears", ai, ZoneType.Hand), ai, null, null);
        resolveAll(game);
        AssertJUnit.assertEquals("Soul Warden triggered three times", life + 3, ai.getLife());
        AssertJUnit.assertTrue(fish.stream().allMatch(Card::isTapped));
    }

    @Test
    public void testFourthStage() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card stage = addCard("Fourth Stage of Magic Design", ai);
        final StaticAbility stAb = stage.getStaticAbilities().get(0);
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertTrue("any type with the same mana cost", StaticAbilityCastAs.anotherCardChoices(bolt, stAb).stream()
                .anyMatch(f -> f.getName().equals("Raging Goblin")));
        AssertJUnit.assertFalse(StaticAbilityCastAs.anotherCardChoices(bolt, stAb).stream().anyMatch(f -> f.getType().isLand()));
        final java.util.List<SpellAbility> spells = StaticAbilityCastAs.getCastAsSpells(bolt, ai);
        AssertJUnit.assertFalse(spells.isEmpty());
        AssertJUnit.assertTrue(spells.stream().allMatch(s -> s.getCastAsStatic() == stAb));

        final SpellAbility asGoblin = spells.get(0);
        asGoblin.setCastAs("Raging Goblin");
        StaticAbilityCastAs.recordCast(asGoblin);
        AssertJUnit.assertTrue("once per turn", StaticAbilityCastAs.getCastAsSpells(bolt, ai).isEmpty());
        AssertJUnit.assertFalse("not the same card twice", StaticAbilityCastAs.anotherCardChoices(bolt, stAb).stream()
                .anyMatch(f -> f.getName().equals("Raging Goblin")));
    }
}
