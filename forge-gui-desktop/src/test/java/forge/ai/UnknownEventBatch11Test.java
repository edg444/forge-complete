package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.MagicColor;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Colorless Ultimatum, Battle Myrsphere, Chancellor of the Mulligan, Wurmcoil Broodmother, Mindslaver Toolkit,
 * Guildmark, Trampled Lotus and Seer of the Bright Side (UNK RA07-RB02), checked against Scryfall 2026-10-21 (no
 * rulings).
 */
public class UnknownEventBatch11Test extends AITest {

    private static SpellAbility activated(final Card c) {
        return c.getSpellAbilities().stream().filter(SpellAbility::isActivatedAbility).findFirst().orElseThrow();
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

    @Test
    public void testColorlessUltimatum() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 5; i++) {
            addCardToZone("Island", human, ZoneType.Library);
        }
        final Card bears = addCard("Grizzly Bears", ai);
        final Card spell = addCardToZone("Colorless Ultimatum", ai, ZoneType.Hand);
        AssertJUnit.assertEquals(7, spell.getCMC());
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertEquals("half of five, rounded up", 3, human.getCardsIn(ZoneType.Exile).size());
        AssertJUnit.assertTrue(bears.hasKeyword(Keyword.ANNIHILATOR));
    }

    @Test
    public void testWurmcoilBroodmother() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card mother = addCard("Wurmcoil Broodmother", ai);
        game.getAction().destroy(mother, null, false, null);
        final SpellAbility dies = game.getCardState(mother).getTriggers().get(0).ensureAbility();
        dies.setActivatingPlayer(ai);
        AbilityUtils.resolve(dies);
        AssertJUnit.assertTrue(ai.getCardsIn(ZoneType.Battlefield).stream().anyMatch(c -> c.isToken() && c.getName().equals("Wurmcoil Engine")));
        AssertJUnit.assertTrue(ai.getCardsIn(ZoneType.Battlefield).stream().anyMatch(c -> c.isToken() && c.getName().equals("Wurmcoil Larva")));
    }

    @Test
    public void testGuildmark() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCardToZone("Lightning Bolt", ai, ZoneType.Library);
        addCardToZone("Giant Growth", ai, ZoneType.Library);
        addCardToZone("Llanowar Elves", ai, ZoneType.Library);
        final Card mark = addCardToZone("Guildmark", ai, ZoneType.Hand);
        game.getAction().moveToPlay(mark, ai, null, null);
        AssertJUnit.assertEquals("joins the guild of its deck's colors", "Gruul", ai.getGuild());
        final Card onField = game.getCardState(mark);
        final SpellAbility mana = onField.getManaAbilities().get(0);
        mana.setActivatingPlayer(ai);
        AssertJUnit.assertEquals("R G", mana.getManaPart().getComboColors(mana));
    }

    @Test
    public void testTrampledLotus() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        addCard("Mountain", human);
        addCard("Forest", human);
        addCard("Island", human);
        addCard("Plains", human);
        final Card lotus = addCard("Trampled Lotus", ai);
        final SpellAbility sa = activated(lotus);
        sa.setActivatingPlayer(ai);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertEquals("three of their lands for mana", 3, ai.getManaPool().totalMana());
        AssertJUnit.assertEquals(3, human.getCardsIn(ZoneType.Battlefield).stream().filter(Card::isTapped).count());
    }

    @Test
    public void testSeerOfTheBrightSide() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card victim = addCard("Grizzly Bears", human);
        final Card seer = addCardToZone("Seer of the Bright Side", ai, ZoneType.Hand);
        game.getAction().moveToPlay(seer, ai, null, null);
        final Card onField = game.getCardState(seer);
        final CounterType cage = CounterType.getType("CAGE");
        AssertJUnit.assertEquals(2, onField.getCounters(cage));
        final SpellAbility upkeep = onField.getTriggers().get(0).ensureAbility();
        upkeep.setActivatingPlayer(ai);
        AbilityUtils.resolve(upkeep);
        resolveAll(game);
        AssertJUnit.assertTrue("one cage left: nothing yet", victim.isInPlay());
        AbilityUtils.resolve(upkeep);
        resolveAll(game);
        AssertJUnit.assertEquals(0, onField.getCounters(cage));
        AssertJUnit.assertTrue("the last cage counter: destroyed", game.getCardState(victim).isInZone(ZoneType.Graveyard));
    }

    @Test
    public void testMindslaverToolkitGivesItself() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card kit = addCard("Mindslaver Toolkit", ai);
        final SpellAbility give = activated(kit);
        give.setActivatingPlayer(ai);
        give.getTargets().add(human);
        AbilityUtils.resolve(give);
        AssertJUnit.assertEquals(human, kit.getController());
        AssertJUnit.assertEquals(MagicColor.COLORLESS, MagicColor.COLORLESS);
    }
}
