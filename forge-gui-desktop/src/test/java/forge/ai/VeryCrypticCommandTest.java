package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/** Very Cryptic Command's six printings, each its own "Choose two -". */
public class VeryCrypticCommandTest extends AITest {

    private Card printing(String name, String set, String cn, Player p, ZoneType zone, boolean onTop) {
        PaperCard pc = FModel.getMagicDb().getCommonCards().getCard(name, set, cn);
        AssertJUnit.assertNotNull(name + " " + set + " " + cn, pc);
        Card c = Card.fromPaperCard(pc, p);
        c.setGameTimestamp(p.getGame().getNextTimestamp());
        if (onTop) {
            p.getZone(zone).add(c, 0);
        } else {
            p.getZone(zone).add(c);
        }
        return c;
    }

    private Card command(String cn, Player p) {
        return printing("Very Cryptic Command", "UST", cn, p, ZoneType.Hand, false);
    }

    private static AbilitySub mode(Card command, int i) {
        SpellAbility charm = command.getFirstSpellAbility();
        AssertJUnit.assertEquals(ApiType.Charm, charm.getApi());
        List<AbilitySub> choices = charm.getAdditionalAbilityList("Choices");
        AssertJUnit.assertEquals(4, choices.size());
        return choices.get(i);
    }

    /** Choose one mode on the Charm, as casting it does, and resolve the Charm. */
    private static void resolveMode(Card command, int i, Player me, Object target) {
        SpellAbility charm = command.getFirstSpellAbility();
        charm.setActivatingPlayer(me);
        AbilitySub m = mode(command, i);
        m.resetTargets();
        if (target instanceof Card c) {
            m.getTargets().add(c);
        } else if (target instanceof Player p) {
            m.getTargets().add(p);
        }
        charm.setSubAbility(null);
        CharmEffect.chainAbilities(charm, new java.util.ArrayList<>(List.of(m)));
        AbilityUtils.resolve(charm);
    }

    @Test
    public void testEveryPrintingIsItsOwnCommand() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        String[] firstModes = {"Switch target creature's power", "Untap two target permanents",
                "Draw a card from an opponent's library", "Return target permanent to its controller's hand",
                "Counter target black-bordered spell", "Scry 3"};
        String[] cns = {"49a", "49b", "49c", "49d", "49e", "49f"};
        for (int i = 0; i < cns.length; i++) {
            Card c = command(cns[i], me);
            AssertJUnit.assertTrue(cns[i], c.getOracleText().startsWith("Choose two"));
            AssertJUnit.assertTrue(cns[i] + ": " + mode(c, 0).getDescription(),
                    mode(c, 0).getDescription().startsWith(firstModes[i]));
        }
    }

    @Test
    public void testWayneEnglandDrawsAnother() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        fillLibrary(me, 3);
        // 49a's own art is by Wayne England; the other printings' is by Zoltan Boros
        printing("Very Cryptic Command", "UST", "49a", me, ZoneType.Library, true);
        Card command = command("49a", me);
        final int hand = me.getCardsIn(ZoneType.Hand).size();

        resolveMode(command, 2, me, null);
        AssertJUnit.assertEquals(hand + 2, me.getCardsIn(ZoneType.Hand).size());

        // a card by anyone else draws just the one
        printing("Very Cryptic Command", "UST", "49b", me, ZoneType.Library, true);
        resolveMode(command, 2, me, null);
        AssertJUnit.assertEquals(hand + 3, me.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void testTapOneWordNames() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card thopter = addCard("Ornithopter", opp);
        Card bears = addCard("Grizzly Bears", opp);
        resolveMode(command("49b", me), 1, me, opp);
        AssertJUnit.assertTrue(thopter.isTapped());
        AssertJUnit.assertFalse(bears.isTapped());
    }

    @Test
    public void testReturnToItsControllersHand() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card bears = addCard("Grizzly Bears", opp);
        bears.setController(me, game.getNextTimestamp());
        resolveMode(command("49d", me), 0, me, bears);
        Card back = game.getCardState(bears);
        AssertJUnit.assertTrue(me.getZone(ZoneType.Hand).contains(back));
        AssertJUnit.assertEquals(opp, back.getOwner());
    }

    @Test
    public void testTurnOver() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card command = command("49d", me);
        Card bears = addCard("Grizzly Bears", me);
        resolveMode(command, 3, me, bears);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(bears.isFaceDown());
        AssertJUnit.assertEquals(2, bears.getNetPower());

        // and back again
        resolveMode(command, 3, me, bears);
        AssertJUnit.assertFalse(bears.isFaceDown());
        AssertJUnit.assertEquals("Grizzly Bears", bears.getName());

        Card werewolf = addCard("Delver of Secrets", me);
        resolveMode(command, 3, me, werewolf);
        AssertJUnit.assertTrue(werewolf.isTransformed());
    }
}
