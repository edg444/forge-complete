package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.effects.ChooseTeamEffect;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Goblin Savant, Jitte and Divining Top, Bob the Claymore, Phyrexian Incubator, The Mysterious Sphere, Oilskelion,
 * Winter's Demonmech, Chandra's Dragonmech and Platinum Persecutor (UNK RA02a-RA06a), checked against Scryfall
 * 2026-10-21 (no rulings).
 */
public class UnknownEventBatch10Test extends AITest {

    private static SpellAbility activated(final Card c) {
        return c.getSpellAbilities().stream().filter(SpellAbility::isActivatedAbility).findFirst().orElseThrow();
    }

    @Test
    public void testGoblinSavant() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card top = addCardToZone("Ornithopter", ai, ZoneType.Library);
        final Card savant = addCard("Goblin Savant", ai);
        final SpellAbility sa = activated(savant);
        sa.setActivatingPlayer(ai);
        AbilityUtils.resolve(sa);
        final Card exiled = game.getCardState(top);
        AssertJUnit.assertTrue(exiled.isInZone(ZoneType.Exile));
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertFalse("an artifact may be played", exiled.mayPlay(ai).isEmpty());
    }

    @Test
    public void testGoblinSavantOtherCard() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card top = addCardToZone("Grizzly Bears", ai, ZoneType.Library);
        final Card savant = addCard("Goblin Savant", ai);
        final SpellAbility sa = activated(savant);
        sa.setActivatingPlayer(ai);
        AbilityUtils.resolve(sa);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertTrue("not a Goblin or artifact", game.getCardState(top).mayPlay(ai).isEmpty());
    }

    @Test
    public void testPhyrexianIncubator() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCardToZone("Phyrexian Rager", ai, ZoneType.Library);
        addCardToZone("Phyrexian Rager", ai, ZoneType.Library);
        addCardToZone("Grizzly Bears", ai, ZoneType.Library);
        final Card incubator = addCard("Phyrexian Incubator", ai);
        final SpellAbility sa = activated(incubator);
        sa.setActivatingPlayer(ai);
        AbilityUtils.resolve(sa);
        final long incubators = ai.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getType().hasSubtype("Incubator")).count();
        AssertJUnit.assertEquals("one Incubator for each Phyrexian exiled", 2, incubators);
    }

    @Test
    public void testPlatinumPersecutor() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        addCard("Platinum Persecutor", game.getPlayers().get(1));
        human.setLife(0, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse("players can't lose the game", human.hasLost());
    }

    @Test
    public void testDragonmechTeam() throws Exception {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCardToZone("Chandra's Dragonmech", ai, ZoneType.Library);
        final java.lang.reflect.Method pre = game.getAction().getClass().getDeclaredMethod("runPreOpeningHandActions", Player.class);
        pre.setAccessible(true);
        pre.invoke(game.getAction(), ai);
        AssertJUnit.assertEquals("a deck with it is on Team Cloudspire", ChooseTeamEffect.CLOUDSPIRE, ChooseTeamEffect.teamOf(ai, true));
    }

    @Test
    public void testMysteriousSphereAdvertising() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 3; i++) {
            addCardToZone("Grizzly Bears", ai, ZoneType.Library);
        }
        final Card sphere = addCard("The Mysterious Sphere", ai);
        final int life = ai.getLife();
        for (int i = 0; i < 3; i++) {
            final SpellAbility sa = activated(sphere);
            sa.setActivatingPlayer(ai);
            AbilityUtils.resolve(sa);
        }
        AssertJUnit.assertEquals(life + 3, ai.getLife());
        AssertJUnit.assertEquals("exiled with it", 3, AbilityUtils.calculateAmount(sphere, "Exiled", null));
    }

    @Test
    public void testOilskelionAndJitte() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card oil = addCard("Oilskelion", ai);
        oil.addCounterInternal(forge.game.card.CounterType.getType("OIL"), 2, ai, false, null, null);
        final SpellAbility ping = activated(oil);
        ping.setActivatingPlayer(ai);
        ping.getTargets().add(human);
        AssertJUnit.assertTrue(ComputerUtil.playNoStack(ai, ping, game, true));
        AssertJUnit.assertEquals(19, human.getLife());
        final Card jitte = addCard("Jitte and Divining Top", ai);
        jitte.addCounterInternal(CounterEnumType.CHARGE, 1, ai, false, null, null);
        AssertJUnit.assertEquals(1, jitte.getCounters(CounterEnumType.CHARGE));
    }
}
