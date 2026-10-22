package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The Keeper of Kaldra, The Good Gamers and Kallist Rhoka (UNK rares), checked against Scryfall 2026-10-22 (no
 * rulings).
 */
public class UnknownEventBatch46Test extends AITest {

    private static SpellAbility svar(final Card c, final String name, final Player p) {
        final SpellAbility sa = AbilityFactory.getAbility(c.getSVar(name), c);
        sa.setActivatingPlayer(p);
        return sa;
    }

    @Test
    public void testKeeperOfKaldra() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card keeper = addCard("The Keeper of Kaldra", ai);
        AbilityUtils.resolve(svar(keeper, "TrigCompanions", ai));
        final List<Card> kaldra = CardLists.filter(ai.getCardsIn(ZoneType.Command), c -> c.getName().endsWith("of Kaldra"));
        AssertJUnit.assertEquals(3, kaldra.size());
        AssertJUnit.assertEquals("each its own companion", 3,
                CardLists.filter(ai.getCardsIn(ZoneType.Command), c -> c.getName().contains("Companion Effect")).size());
        AbilityUtils.resolve(svar(keeper, "TrigCompanions", ai));
        AssertJUnit.assertEquals("only the first time", 3,
                CardLists.filter(ai.getCardsIn(ZoneType.Command), c -> c.getName().endsWith("of Kaldra")).size());

        final Card avatar = addCard("Grizzly Bears", ai);
        avatar.addChangedName("Kaldra", false, game.getNextTimestamp(), 0);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertTrue(avatar.hasKeyword(forge.game.keyword.Keyword.HEXPROOF));
    }

    @Test
    public void testGoodGamersCopiesAndNames() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card gamers = addCard("The Good Gamers", ai);
        final SpellAbility copy = svar(gamers, "TrigCopy", ai);
        copy.setSVar("X", "Number$2");
        AbilityUtils.resolve(copy);
        final List<Card> all = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getType().hasCreatureType("Gamer"));
        AssertJUnit.assertEquals(3, all.size());
        AssertJUnit.assertEquals("unique names", 3, all.stream().map(Card::getName).distinct().count());
    }

    @Test
    public void testGoodGamersDamageIsCommanderDamage() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card commander = addCardToZone("Grizzly Bears", ai, ZoneType.Command);
        commander.setCommander(true);
        ai.addCommander(commander);
        final Card gamers = addCard("The Good Gamers", ai);
        human.addDamageAfterPrevention(3, gamers, null, false, null);
        AssertJUnit.assertEquals(3, human.getCommanderDamage(commander));
    }

    @Test
    public void testKallistRhokaSwaps() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card wurm = addCard("Craw Wurm", human);
        final Card kallist = addCardToZone("Kallist Rhoka", ai, ZoneType.Hand);
        final SpellAbility swap = AbilityFactory.getAbility("DB$ Clone | Defined$ Remembered | CloneTarget$ Self | Swap$ True", kallist);
        swap.setActivatingPlayer(ai);
        kallist.addRemembered(wurm);
        AbilityUtils.resolve(swap);
        AssertJUnit.assertEquals("Craw Wurm", kallist.getName());
        AssertJUnit.assertEquals("Kallist Rhoka", wurm.getName());
        AssertJUnit.assertEquals(2, wurm.getNetPower());
        AssertJUnit.assertTrue(wurm.hasKeyword(forge.game.keyword.Keyword.DEATHTOUCH));
    }

    @Test
    public void testKallistRhokaStaysTheCopyAsItResolves() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card wurm = addCard("Craw Wurm", human);
        final Card inHand = addCardToZone("Kallist Rhoka", ai, ZoneType.Hand);
        final Card onStack = game.getAction().moveTo(ZoneType.Stack, inHand, null, forge.game.ability.AbilityKey.newMap());
        final SpellAbility swap = AbilityFactory.getAbility("DB$ Clone | Defined$ Remembered | CloneTarget$ Self | Swap$ True", onStack);
        swap.setActivatingPlayer(ai);
        onStack.addRemembered(wurm);
        AbilityUtils.resolve(swap);
        final Card resolved = game.getAction().moveToPlay(onStack, ai, onStack.getFirstSpellAbility(), forge.game.ability.AbilityKey.newMap());
        AssertJUnit.assertEquals("Craw Wurm", resolved.getName());
        AssertJUnit.assertEquals(6, resolved.getNetPower());
    }
}
