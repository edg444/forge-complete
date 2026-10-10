package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import forge.card.WinningDeck;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.cost.CostAdjustment;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Metagamer (Mystery Booster playtest card): "Cards in the winning deck(s) of the latest Mythic Championship cost {1}
 * more to cast." - Oracle text and rulings checked against Scryfall 2026-10-10 (the errata ruling moves it to the
 * latest comparable series; user: the latest Pro Tour). Spells sharing a name with the deck's cards count, anyone's.
 */
public class MetagamerTest extends AITest {

    private List<String> saved;
    private String savedEvent;
    private String savedWinner;

    @AfterMethod
    public void restore() {
        if (saved != null) {
            WinningDeck.set(savedEvent, savedWinner, saved);
        }
    }

    private static int manaToPay(Card c, Player p) {
        SpellAbility spell = c.getFirstSpellAbility();
        spell.setActivatingPlayer(p);
        // raises apply to the Cost, reductions while paying it
        ManaCostBeingPaid cost = new ManaCostBeingPaid(CostAdjustment.adjust(spell.getPayCosts(), spell, false).getTotalMana());
        CostAdjustment.adjust(cost, spell, p, null, true, false);
        return cost.getConvertedManaCost();
    }

    @Test
    public void testWinningDeckCostsMore() {
        Game game = initAndCreateGame();
        forge.model.MetagamerDeckLoader.awaitRefresh();
        saved = List.copyOf(WinningDeck.getNames());
        savedEvent = WinningDeck.getEvent();
        savedWinner = WinningDeck.getWinner();
        WinningDeck.set("Test Pro Tour", "Somebody", List.of("Lightning Bolt", "Fire // Ice"));

        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Metagamer", me);
        Card myBolt = addCardToZone("Lightning Bolt", me, ZoneType.Hand);
        Card theirBolt = addCardToZone("Lightning Bolt", opp, ZoneType.Hand);
        Card shock = addCardToZone("Shock", opp, ZoneType.Hand);
        Card fireIce = addCardToZone("Fire // Ice", opp, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertEquals(2, manaToPay(myBolt, me));
        AssertJUnit.assertEquals(2, manaToPay(theirBolt, opp));
        AssertJUnit.assertEquals(1, manaToPay(shock, opp));
        AssertJUnit.assertEquals(3, manaToPay(fireIce, opp));
    }

    @Test
    public void testShippedDeckIsLoaded() {
        initAndCreateGame();
        // FModel loads the cached or shipped deck at startup
        AssertJUnit.assertFalse(WinningDeck.getNames().isEmpty());
        AssertJUnit.assertTrue(WinningDeck.getEvent(), WinningDeck.getEvent().startsWith("Pro Tour"));
    }
}
