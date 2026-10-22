package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityCastAs;
import forge.game.zone.ZoneType;

/**
 * Dockbreacher, New Magic Game Plus, Compleated Clone Shell and Enolc, Perfect Clone (UNK cards), checked against
 * Scryfall 2026-10-22 (no rulings).
 */
public class UnknownEventBatch50Test extends AITest {

    private void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    private static int treasures(final Player p) {
        return CardLists.filter(p.getCardsIn(ZoneType.Battlefield), c -> c.getType().hasSubtype("Treasure")).size();
    }

    @Test
    public void testDockbreacher() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        addCard("Dockbreacher", ai);
        fillLibrary(ai, 5);
        final Card source = addCard("Grizzly Bears", human);
        final SpellAbility two = AbilityFactory.getAbility("DB$ Token | TokenScript$ c_a_treasure_sac | TokenAmount$ 2", source);
        two.setActivatingPlayer(human);
        AbilityUtils.resolve(two);
        AssertJUnit.assertEquals("the first one", 1, treasures(human));
        AssertJUnit.assertEquals("instead of the second", 1, ai.getCardsIn(ZoneType.Hand).size());
        AbilityUtils.resolve(two);
        AssertJUnit.assertEquals(1, treasures(human));
        AssertJUnit.assertEquals(3, ai.getCardsIn(ZoneType.Hand).size());
        final SpellAbility mine = AbilityFactory.getAbility("DB$ Token | TokenScript$ c_a_treasure_sac | TokenAmount$ 2", source);
        mine.setActivatingPlayer(ai);
        AbilityUtils.resolve(mine);
        AssertJUnit.assertEquals("its controller's own aren't affected", 2, treasures(ai));
    }

    @Test
    public void testNewMagicGamePlus() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card forest = addCard("Forest", ai);
        final Card splitter = addCard("Bonesplitter", ai);
        final Card bears = addCard("Grizzly Bears", ai);
        final Card theirs = addCard("Mountain", human);
        final Card exiled = addCardToZone("Lightning Bolt", human, ZoneType.Exile);
        final Card plus = addCardToZone("New Magic Game Plus", ai, ZoneType.Hand);
        final Card onStack = game.getAction().moveTo(ZoneType.Stack, plus, null, AbilityKey.newMap());
        final SpellAbility sa = onStack.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertTrue(forest.isInZone(ZoneType.Battlefield) && splitter.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertTrue(theirs.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertTrue(CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getName().equals("Grizzly Bears")).isEmpty());
        AssertJUnit.assertEquals("this card stays in exile", 1,
                CardLists.filter(ai.getCardsIn(ZoneType.Exile), c -> c.getName().equals("New Magic Game Plus")).size());
        AssertJUnit.assertTrue("other exiled cards don't", human.getCardsIn(ZoneType.Exile).isEmpty());
        AssertJUnit.assertFalse(bears.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertFalse(exiled.isInZone(ZoneType.Exile));
    }

    @Test
    public void testCompleatedCloneShell() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCardToZone("Craw Wurm", ai, ZoneType.Sideboard);
        addCardToZone("Grizzly Bears", ai, ZoneType.Sideboard);
        final Card shell = addCard("Compleated Clone Shell", ai);
        final SpellAbility look = shell.getTriggers().get(0).ensureAbility();
        look.setActivatingPlayer(ai);
        AbilityUtils.resolve(look);
        AssertJUnit.assertEquals(1, ai.getCardsIn(ZoneType.Exile).size());
        final Card hidden = ai.getCardsIn(ZoneType.Exile).get(0);
        AssertJUnit.assertTrue(hidden.isFaceDown());
        AssertJUnit.assertEquals(1, ai.getCardsIn(ZoneType.Sideboard).size());
        final SpellAbility dies = shell.getTriggers().get(1).ensureAbility();
        dies.setActivatingPlayer(ai);
        AbilityUtils.resolve(dies);
        resolveAll(game);
        AssertJUnit.assertFalse("turned face up and cast", ai.getCardsIn(ZoneType.Exile).stream().anyMatch(Card::isFaceDown));
    }

    @Test
    public void testEnolcCopiesTheOtherCommander() throws Exception {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card enolc = addCardToZone("Enolc, Perfect Clone", ai, ZoneType.Command);
        final Card other = addCardToZone("Thrasios, Triton Hero", ai, ZoneType.Command);
        enolc.setCommander(true);
        other.setCommander(true);
        ai.addCommander(enolc);
        ai.addCommander(other);
        final java.lang.reflect.Method pre = game.getAction().getClass().getDeclaredMethod("runPreOpeningHandActions", Player.class);
        pre.setAccessible(true);
        pre.invoke(game.getAction(), ai);
        AssertJUnit.assertEquals("keeps its name", "Enolc, Perfect Clone", enolc.getName());
        AssertJUnit.assertEquals(1, enolc.getNetPower());
        AssertJUnit.assertEquals("{G}{U}", enolc.getManaCost().toString());
        AssertJUnit.assertTrue(enolc.getSpellAbilities().anyMatch(s -> s.isActivatedAbility()));
    }

    @Test
    public void testEnolcCastAsACommander() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card commander = addCardToZone("Thrasios, Triton Hero", ai, ZoneType.Command);
        commander.setCommander(true);
        ai.addCommander(commander);
        final Card enolc = addCardToZone("Enolc, Perfect Clone", ai, ZoneType.Hand);
        AssertJUnit.assertTrue(StaticAbilityCastAs.getCastAsSpells(enolc, ai).stream()
                .anyMatch(s -> "Thrasios, Triton Hero".equals(s.getCastAs())));
    }
}
