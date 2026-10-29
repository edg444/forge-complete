package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CounterType;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/**
 * Mystery Booster 2 playtest cards (MB2), part 1, checked against Scryfall 2026-10-22 with their rulings.
 */
public class MysteryBooster2PlaytestTest extends AITest {

    private static SpellAbility svar(final Card c, final String name, final Player p) {
        final SpellAbility sa = AbilityFactory.getAbility(c.getSVar(name), c);
        sa.setActivatingPlayer(p);
        return sa;
    }

    private void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    private void castWithTarget(final Game game, final Player p, final Card spell, final Card target) {
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        if (target != null) {
            sa.getTargets().add(target);
        }
        final Card onStack = game.getAction().moveToStack(spell, sa);
        sa.setHostCard(onStack);
        game.getStack().add(sa);
        resolveAll(game);
    }

    @Test
    public void testWrathOfLeknif() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card bear = addCard("Grizzly Bears", ai);
        final Card forest = addCard("Forest", ai);
        forest.tap(null, null);
        castWithTarget(game, ai, addCardToZone("Wrath of Leknif", ai, ZoneType.Hand), null);
        AssertJUnit.assertFalse(game.getCardState(bear).isInPlay());
        AssertJUnit.assertFalse("untapped", forest.isTapped());
    }

    @Test
    public void testTextOnlyCards() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card abbot = addCard("Abbot of the Sacred Meeple", ai);
        AssertJUnit.assertTrue(abbot.getAbilityText().contains("Sinecure (This creature can occupy"));
        final Card spuzzem = addCard("Spuzzem Strategist", ai);
        AssertJUnit.assertTrue(spuzzem.getType().hasCreatureType("Spuzzem"));
        AssertJUnit.assertEquals(4, spuzzem.getNetPower());
    }

    @Test
    public void testDairyCow() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        addCard("Forest", ai);
        addCard("Forest", ai);
        castWithTarget(game, ai, addCardToZone("Dairy Cow", ai, ZoneType.Hand), null);
        final Card cow = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getName().equals("Dairy Cow")).get(0);
        AssertJUnit.assertEquals("five per Forest", 10, cow.getCounters(CounterType.getType("MILK")));
        AssertJUnit.assertTrue(cow.getType().hasCreatureType("Cow"));
    }

    @Test
    public void testRinAndSeri() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Rin and Seri, Inseparabler", ai);
        final Card cat = addCard("Savannah Lions", ai);
        final Card dog = addCard("Jackal Pup", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(cat.getType().hasCreatureType("Dog"));
        AssertJUnit.assertTrue(cat.hasKeyword(Keyword.HASTE));
        AssertJUnit.assertTrue(dog.getType().hasCreatureType("Jackal"));
    }

    @Test
    public void testTlDr() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player human = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card vanillaish = addCard("Serra Angel", human);
        final Card looter = addCard("Merfolk Looter", human);
        AssertJUnit.assertFalse("flying and vigilance are keywords", vanillaish.isValid("Card.HasNonKeywordAbility", ai, vanillaish, null));
        AssertJUnit.assertTrue(looter.isValid("Card.HasNonKeywordAbility", ai, looter, null));
        castWithTarget(game, ai, addCardToZone("TL;DR", ai, ZoneType.Hand), vanillaish);
        AssertJUnit.assertTrue(vanillaish.isInPlay());
        castWithTarget(game, ai, addCardToZone("TL;DR", ai, ZoneType.Hand), looter);
        AssertJUnit.assertTrue(game.getCardState(looter).isInZone(ZoneType.Exile));
    }

    @Test
    public void testSubgoyf() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card goyf = addCard("Subgoyf", ai);
        addCardToZone("Plains", ai, ZoneType.Graveyard);
        addCardToZone("Bonesplitter", ai, ZoneType.Graveyard);
        addCardToZone("Grizzly Bears", ai, ZoneType.Graveyard);
        addCardToZone("Pacifism", ai, ZoneType.Graveyard);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("Plains, Equipment, Aura - not Bear", 3, goyf.getNetPower());
        AssertJUnit.assertEquals(4, goyf.getNetToughness());
    }

    @Test
    public void testPlainWalker() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player human = game.getPlayers().get(0);
        final Card walker = addCard("Plain Walker", ai);
        final Card blocker = addCard("Grizzly Bears", human);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(forge.game.combat.CombatUtil.canBlock(walker, blocker));
        addCard("Ajani Goldmane", human).addCounterInternal(forge.game.card.CounterEnumType.LOYALTY, 4, human, false, null, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse("planeswalkerwalk", forge.game.combat.CombatUtil.canBlock(walker, blocker));
    }

    @Test
    public void testForbidden() {
        initAndCreateGame();
        final Deck deck = new Deck("forbidden");
        deck.getMain().add(FModel.getMagicDb().getCommonCards().getCard("Forest"), 59);
        deck.getMain().add(FModel.getMagicDb().getCommonCards().getCard("Can't Quite Recall"), 1);
        AssertJUnit.assertNotNull(DeckFormat.Constructed.getDeckConformanceProblem(deck));
        deck.getMain().remove(FModel.getMagicDb().getCommonCards().getCard("Can't Quite Recall"), 1);
        deck.getMain().add(FModel.getMagicDb().getCommonCards().getCard("Forest"), 1);
        deck.getOrCreate(forge.deck.DeckSection.Sideboard).add(FModel.getMagicDb().getCommonCards().getCard("Can't Quite Recall"), 1);
        AssertJUnit.assertNull("the sideboard isn't the starting deck", DeckFormat.Constructed.getDeckConformanceProblem(deck));
    }

    @Test
    public void testMuragandaEldrazi() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player human = game.getPlayers().get(0);
        final Card eldrazi = addCard("Muraganda Eldrazi", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue("deworded", eldrazi.hasNoAbilities());
        AssertJUnit.assertTrue(eldrazi.getColor().isColorless());
        final Card angel = addCard("Serra Angel", human);
        final SpellAbility counter = svar(eldrazi, "TrigCounter", ai);
        counter.getTargets().add(angel);
        AbilityUtils.resolve(counter);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(angel.hasKeyword(Keyword.FLYING));
        final SpellAbility pump = AbilityFactory.getAbility("DB$ Pump | Defined$ CardUID_" + angel.getId() + " | KW$ Trample", angel);
        pump.setActivatingPlayer(human);
        AbilityUtils.resolve(pump);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse("can't gain abilities", angel.hasKeyword(Keyword.TRAMPLE));
        game.getAction().exile(game.getCardState(eldrazi), null, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse("with or without Muraganda Eldrazi", angel.hasKeyword(Keyword.FLYING));
    }

    @Test
    public void testSliverOfHope() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card hope = addCard("Sliver of Hope", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(hope.hasKeyword(Keyword.HOPE));
        final SpellAbility shock = AbilityFactory.getAbility("DB$ DealDamage | Defined$ CardUID_" + hope.getId() + " | NumDmg$ 2", hope);
        shock.setActivatingPlayer(ai);
        AbilityUtils.resolve(shock);
        AssertJUnit.assertEquals("not attacking: no hope", 2, hope.getDamage());
    }

    @Test
    public void testTwoByFour() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card bear = addCard("Grizzly Bears", ai);
        final Card two = addCardToZone("Two by Four", ai, ZoneType.Hand);
        final SpellAbility power = svar(two, "DBPower", ai);
        power.getTargets().add(bear);
        AbilityUtils.resolve(power);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(4, bear.getNetPower());
        AssertJUnit.assertEquals(2, bear.getNetToughness());
        final SpellAbility giant = AbilityFactory.getAbility("DB$ Pump | Defined$ CardUID_" + bear.getId() + " | NumAtt$ +1", bear);
        giant.setActivatingPlayer(ai);
        AbilityUtils.resolve(giant);
        AssertJUnit.assertEquals("raising still applies on top", 5, bear.getNetPower());
        bear.subtractCounter(CounterType.getType("Base Power 4"), 1, ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(3, bear.getNetPower());
    }
}
