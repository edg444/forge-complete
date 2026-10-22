package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CounterType;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityCastAs;
import forge.game.staticability.StaticAbilityUnspentMana;
import forge.game.zone.ZoneType;

/**
 * Noyd, Keeper of the Void, Sharp Eraser, Banned Eldraine Card, Rafi, Retro Racer, The Egotistical Velociraptor, The
 * Ancient Dingus and Fifth Stage of Magic Design (UNK rares), checked against Scryfall 2026-10-22 (no rulings).
 */
public class UnknownEventBatch53Test extends AITest {

    private static SpellAbility svar(final Card c, final String name, final Player p) {
        final SpellAbility sa = AbilityFactory.getAbility(c.getSVar(name), c);
        sa.setActivatingPlayer(p);
        return sa;
    }

    private static int food(final Player p) {
        return CardLists.filter(p.getCardsIn(ZoneType.Battlefield), c -> c.getType().hasSubtype("Food")).size();
    }

    @Test
    public void testNoydTimer() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card noyd = addCard("Noyd, Keeper of the Void", ai);
        final Card bears = addCard("Grizzly Bears", ai);
        AbilityUtils.resolve(svar(noyd, "TrigTimer", ai));
        AssertJUnit.assertTrue(ai.hasTimerRunning());
        AbilityUtils.resolve(svar(noyd, "TrigFood", ai));
        AssertJUnit.assertEquals(1, food(ai));
        game.getAction().checkRealTimeTimers();
        AssertJUnit.assertTrue("thirty minutes haven't passed", noyd.isInZone(ZoneType.Battlefield));
        ai.startTimer(0, noyd);
        game.getAction().checkRealTimeTimers();
        AssertJUnit.assertEquals(0, food(ai));
        AssertJUnit.assertTrue(CardLists.filter(ai.getCardsIn(ZoneType.Exile), c -> c.getName().startsWith("Noyd")).size() == 1);
        AssertJUnit.assertTrue(bears.isInZone(ZoneType.Battlefield));
        final SpellAbility makeFood = AbilityFactory.getAbility("DB$ Token | TokenScript$ c_a_food_sac", bears);
        makeFood.setActivatingPlayer(ai);
        AbilityUtils.resolve(makeFood);
        AssertJUnit.assertEquals("no Food tokens for the rest of the game", 0, food(ai));
    }

    @Test
    public void testSharpEraserAiErasesDrawbacks() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card eraser = addCard("Sharp Eraser", ai);
        final Card wall = addCardToZone("Wall of Wood", ai, ZoneType.Library);
        AssertJUnit.assertTrue(wall.hasKeyword(Keyword.DEFENDER));
        eraser.addRemembered(wall);
        AbilityUtils.resolve(svar(eraser, "DBErase", ai));
        AssertJUnit.assertFalse("defender erased", wall.hasKeyword(Keyword.DEFENDER));
        final Card moved = game.getAction().moveToPlay(wall, ai, null, forge.game.ability.AbilityKey.newMap());
        AssertJUnit.assertFalse("still erased in another zone", moved.hasKeyword(Keyword.DEFENDER));
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Library);
        eraser.clearRemembered();
        eraser.addRemembered(bolt);
        AbilityUtils.resolve(svar(eraser, "DBErase", ai));
        AssertJUnit.assertEquals("nothing to erase", 1, bolt.getSpellAbilities().size());
    }

    @Test
    public void testBannedEldraineCard() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card banned = addCardToZone("Banned Eldraine Card", ai, ZoneType.Hand);
        final java.util.List<SpellAbility> options = StaticAbilityCastAs.getCastAsSpells(banned, ai);
        final java.util.Set<String> names = new java.util.HashSet<>();
        options.forEach(s -> names.add(s.getCastAs()));
        AssertJUnit.assertEquals(6, names.size());
        final SpellAbility clover = options.stream().filter(s -> "Lucky Clover".equals(s.getCastAs())).findFirst().orElseThrow();
        AssertJUnit.assertEquals("{2} plus its {1}", 3, clover.getPayCosts().getTotalMana().getCMC());
        final SpellAbility sanctuary = options.stream().filter(s -> "Mystic Sanctuary".equals(s.getCastAs())).findFirst().orElseThrow();
        AssertJUnit.assertEquals(1, sanctuary.getPayCosts().getTotalMana().getCMC());
        banned.setSplitStateToPlayAbility(clover);
        AssertJUnit.assertEquals("Lucky Clover", banned.getName());
        final Card inGraveyard = game.getAction().moveToGraveyard(banned, null);
        AssertJUnit.assertEquals("for the rest of the game, in all zones", "Lucky Clover", inGraveyard.getName());
    }

    @Test
    public void testRafiRules() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card rafi = addCard("Rafi, Retro Racer", ai);
        AssertJUnit.assertFalse(StaticAbilityUnspentMana.hasManaBurn(human));
        AbilityUtils.resolve(svar(rafi, "DBRetroRules", ai));
        AbilityUtils.resolve(svar(rafi, "DBRetroRules", ai));
        AssertJUnit.assertTrue(StaticAbilityUnspentMana.hasManaBurn(human));
        AssertJUnit.assertEquals("one effect", 1, CardLists.filter(ai.getCardsIn(ZoneType.Command), c -> c.getName().equals("Retro Rules")).size());
        final SpellAbility mana = rafi.getManaAbilities().get(0);
        mana.setActivatingPlayer(ai);
        AbilityUtils.resolve(mana);
        AssertJUnit.assertEquals(3, human.getManaPool().totalMana());
        AssertJUnit.assertEquals(3, ai.getManaPool().totalMana());
    }

    @Test
    public void testVelociraptorTakeover() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        fillLibrary(ai, 6);
        addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        final Card raptor = addCard("The Egotistical Velociraptor", ai);
        final PlayerController before = ai.getController();
        AbilityUtils.resolve(svar(raptor, "TrigTakeover", ai));
        AssertJUnit.assertNotSame("someone else is playing", before, ai.getController());
        AssertJUnit.assertEquals(4, ai.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(24, ai.getLife());
    }

    @Test
    public void testAncientDingus() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        addCard("The Ancient Dingus", ai);
        final Card source = addCard("Grizzly Bears", human);
        final SpellAbility two = AbilityFactory.getAbility("DB$ Token | TokenScript$ c_a_food_sac | TokenAmount$ 2", source);
        two.setActivatingPlayer(human);
        AbilityUtils.resolve(two);
        AssertJUnit.assertEquals(0, food(human));
        AssertJUnit.assertEquals(16, human.getLife());
        AssertJUnit.assertEquals(2, human.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void testFifthStageCounters() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card stage = addCard("Fifth Stage of Magic Design", ai);
        stage.addCounterInternal(CounterType.getType("RESONANCE"), 1, ai, false, null, null);
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        AssertJUnit.assertTrue("a counter, so it stays", game.getStack().isEmpty());
        stage.subtractCounter(CounterType.getType("RESONANCE"), 1, ai);
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
        }
        AssertJUnit.assertTrue("no counters: sacrificed", stage.isInZone(ZoneType.Graveyard) || !stage.isInZone(ZoneType.Battlefield));
    }
}
