package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/**
 * The Colossal Dreadmaw, Just a Li'l Guy, Oozeavite, Retto, Family Racer, Your Favorite Character, Playtest Wish, Fear
 * of the Unknown and The Companion of the Wilds (UNK rares), checked against Scryfall 2026-10-22 (Dreadmaw's playtest
 * card rulings).
 */
public class UnknownEventBatch42Test extends AITest {

    private static void runPreOpeningHandActions(final Game game, final Player p) throws Exception {
        final java.lang.reflect.Method pre = game.getAction().getClass().getDeclaredMethod("runPreOpeningHandActions", Player.class);
        pre.setAccessible(true);
        pre.invoke(game.getAction(), p);
    }

    @Test
    public void testCastAsColossalDreadmaw() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("The Colossal Dreadmaw", ai);
        final Card bears = addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(bears.getAllPossibleAbilities(ai, true).stream().anyMatch(sa -> "Colossal Dreadmaw".equals(sa.getCastAs())));
    }

    @Test
    public void testJustALilGuy() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card wurm = addCard("Craw Wurm", ai);
        final Card guy = addCardToZone("Just a Li'l Guy", ai, ZoneType.Hand);
        final SpellAbility cast = guy.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        cast.getTargets().add(wurm);
        AbilityUtils.resolve(cast);
        wurm.addCounterInternal(CounterEnumType.P1P1, 3, ai, false, null, null);
        AssertJUnit.assertEquals(1, wurm.getNetPower());
        AssertJUnit.assertEquals(1, wurm.getNetToughness());
        AssertJUnit.assertTrue(wurm.hasKeyword(forge.game.keyword.Keyword.INDESTRUCTIBLE));
        wurm.lockPT(null, null);
        AssertJUnit.assertEquals("after the turn: 6/4 with three counters", 9, wurm.getNetPower());
    }

    @Test
    public void testOozeaviteSplitsOff() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        Card ooze = addCardToZone("Oozeavite", ai, ZoneType.Hand);
        ooze = game.getAction().moveToPlay(ooze, ai, null, null);
        AssertJUnit.assertEquals("four counters that are also +1/+1 counters", 4, ooze.getNetPower());
        AssertJUnit.assertEquals(1, ooze.getCounters(CounterType.getType("Vigilance")));
        final Card bolt = addCardToZone("Lightning Bolt", human, ZoneType.Hand);
        final SpellAbility cast = bolt.getFirstSpellAbility();
        cast.setActivatingPlayer(human);
        cast.getTargets().add(ooze);
        AbilityUtils.resolve(cast);
        AssertJUnit.assertEquals("no damage, and a counter moved off", 0, ooze.getDamage());
        final List<Card> tokens = ai.getCardsIn(ZoneType.Battlefield).stream().filter(Card::isToken).toList();
        AssertJUnit.assertEquals(1, tokens.size());
        AssertJUnit.assertTrue(tokens.get(0).getNetPower() >= 1);
        AssertJUnit.assertEquals(ooze.getNetPower() + tokens.get(0).getNetPower(), 4);
    }

    @Test
    public void testRettoFamily() throws Exception {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card retto = addCardToZone("Retto, Family Racer", ai, ZoneType.Library);
        addCardToZone("Craw Wurm", ai, ZoneType.Library);
        addCardToZone("Grizzly Bears", ai, ZoneType.Library);
        runPreOpeningHandActions(game, ai);
        final Object family = ai.getMatchNote("Family:" + game.getId());
        AssertJUnit.assertTrue(family instanceof List<?> l && l.contains("Craw Wurm"));
        final Card onBattlefield = game.getAction().moveToPlay(retto, ai, null, null);
        final Card wurm = addCardToZone("Craw Wurm", ai, ZoneType.Hand);
        AssertJUnit.assertTrue(wurm.isValid("Creature.YouCtrl+InFamily+Other", ai, onBattlefield, null));
    }

    @Test
    public void testFavoriteCharacter() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card yfc = addCardToZone("Your Favorite Character", ai, ZoneType.Hand);
        final SpellAbility define = AbilityFactory.getAbility("DB$ FavoriteCharacter", yfc);
        define.setActivatingPlayer(ai);
        AbilityUtils.resolve(define);
        AssertJUnit.assertFalse(yfc.getName().equals("Your Favorite Character"));
        AssertJUnit.assertTrue(yfc.hasKeyword(forge.game.keyword.Keyword.FLYING) && yfc.hasKeyword(forge.game.keyword.Keyword.TRAMPLE));
        AssertJUnit.assertEquals("the two chosen types", 2, yfc.getType().getCreatureTypes().size());
        final Card moved = game.getAction().moveToPlay(yfc, ai, null, null);
        AssertJUnit.assertTrue(moved.hasKeyword(forge.game.keyword.Keyword.FLYING));
    }

    @Test
    public void testPlaytestCardsFromOutsideTheGame() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card wolf = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Throat Wolf", "CMB1", "65"), ai);
        ai.getZone(ZoneType.Sideboard).add(wolf);
        final Card wish = addCardToZone("Playtest Wish", ai, ZoneType.Hand);
        final SpellAbility cast = wish.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        AbilityUtils.resolve(cast);
        final Card exiled = game.getCardState(wolf);
        AssertJUnit.assertTrue(exiled.isInZone(ZoneType.Exile));
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertFalse("castable from exile", exiled.mayPlay(ai).isEmpty());

        final Card wolf2 = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Throat Wolf", "CMB1", "65"), ai);
        ai.getZone(ZoneType.Sideboard).add(wolf2);
        final Card fear = addCardToZone("Fear of the Unknown", ai, ZoneType.Hand);
        final SpellAbility trig = fear.getTriggers().get(0).ensureAbility();
        trig.setActivatingPlayer(ai);
        AbilityUtils.resolve(trig);
        AssertJUnit.assertTrue(game.getCardState(wolf2).isInZone(ZoneType.Exile));
    }

    @Test
    public void testOldCompanion() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card companion = addCardToZone("The Companion of the Wilds", ai, ZoneType.Command);
        final Card effect = Player.createCompanionEffect(companion);
        AssertJUnit.assertTrue(effect.getStaticAbilities().anyMatch(st -> st.hasParam("MayPlay")));
        AssertJUnit.assertFalse(effect.getStaticAbilities().anyMatch(st -> st.hasSVar("MoveToHand")));
    }
}
