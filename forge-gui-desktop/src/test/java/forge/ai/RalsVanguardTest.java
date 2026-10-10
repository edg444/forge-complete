package forge.ai;

import java.util.Collections;
import java.util.EnumSet;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.deck.CardPool;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameType;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Ral's Vanguard (Mystery Booster playtest card), Oracle text and rulings checked against Scryfall 2026-10-10:
 * the requirement must be met as the game begins and only counts the starting deck (not the sideboard); copies of
 * spells don't draw extra or deal extra damage. Hand +1 also raises the maximum hand size (CR 902.5b).
 */
public class RalsVanguardTest extends AITest {

    private static PaperCard paper(String name) {
        return FModel.getMagicDb().getCommonCards().getCard(name);
    }

    private static RegisteredPlayer vanguardPlayer(String... main) {
        Deck deck = new Deck("test");
        for (String name : main) {
            deck.getMain().add(paper(name), 4);
        }
        deck.getOrCreate(forge.deck.DeckSection.Sideboard).add(paper("Grizzly Bears"), 1);
        CardPool avatar = new CardPool();
        avatar.add(FModel.getMagicDb().getVariantCards().getCard("Ral's Vanguard"));
        return RegisteredPlayer.forVariants(2, EnumSet.of(GameType.Vanguard), deck, Collections.emptyList(), false,
                Collections.emptyList(), avatar);
    }

    @Test
    public void testRequirement() {
        initAndCreateGame();
        RegisteredPlayer ok = vanguardPlayer("Lightning Bolt", "Opt", "Island");
        AssertJUnit.assertEquals(1, ok.getVanguardAvatars().size());
        AssertJUnit.assertEquals(15, ok.getStartingLife());
        AssertJUnit.assertEquals(8, ok.getStartingHand());

        RegisteredPlayer creature = vanguardPlayer("Lightning Bolt", "Grizzly Bears", "Island");
        AssertJUnit.assertTrue(creature.getVanguardAvatars().isEmpty());
        AssertJUnit.assertEquals(1, creature.getUnmetVanguardAvatars().size());
        AssertJUnit.assertEquals(20, creature.getStartingLife());
        AssertJUnit.assertEquals(7, creature.getStartingHand());
    }

    private Card vanguard(Player p) {
        Card v = Card.fromPaperCard(FModel.getMagicDb().getVariantCards().getCard("Ral's Vanguard"), p);
        p.getZone(ZoneType.Command).add(v);
        p.getGame().getAction().checkStateEffects(true);
        return v;
    }

    private static void resolveFromHand(Card spell, Player p, Object target) {
        SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        if (target instanceof Player) {
            sa.getTargets().add((Player) target);
        } else if (target instanceof Card) {
            sa.getTargets().add((Card) target);
        }
        AbilityUtils.resolve(sa);
    }

    @Test
    public void testExtraDraw() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        fillLibrary(p, 10);
        vanguard(p);
        Card divination = addCardToZone("Divination", p, ZoneType.Hand);
        int before = p.getCardsIn(ZoneType.Hand).size();
        resolveFromHand(divination, p, null);
        // Divination draws two, plus one; it's still counted as in hand since the test resolves it in place
        AssertJUnit.assertEquals(before + 3, p.getCardsIn(ZoneType.Hand).size());

        int handNow = p.getCardsIn(ZoneType.Hand).size();
        p.drawCards(1);
        AssertJUnit.assertEquals(handNow + 1, p.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void testExtraDamage() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        vanguard(p);
        Card shock = addCardToZone("Shock", p, ZoneType.Hand);
        resolveFromHand(shock, p, opp);
        AssertJUnit.assertEquals(17, opp.getLife());

        Card giant = addCard("Hill Giant", opp);
        Card shock2 = addCardToZone("Shock", p, ZoneType.Hand);
        resolveFromHand(shock2, p, giant);
        AssertJUnit.assertEquals(3, giant.getDamage());

        // the opponent's spells aren't "spells you cast"
        Card theirs = addCardToZone("Shock", opp, ZoneType.Hand);
        resolveFromHand(theirs, opp, p);
        AssertJUnit.assertEquals(18, p.getLife());
    }
}
