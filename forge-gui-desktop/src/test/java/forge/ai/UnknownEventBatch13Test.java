package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Another Night in Vegas, Yet Another Night in Vegas, A Chaotic Night in Vegas, The Archenemy's Charm, Monster Mash-Up
 * and The Zassacre Zirl (UNK black rares), checked against Scryfall 2026-10-21 (no rulings).
 */
public class UnknownEventBatch13Test extends AITest {

    @Test
    public void testYetAnotherNightResets() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card night = addCard("Yet Another Night in Vegas", ai);
        final SpellAbility charm = night.getTriggers().get(0).ensureAbility();
        final SpellAbility sights = charm.getAdditionalAbilityList("Choices").get(0);
        night.addChosenModes(sights, "See the sights", false);
        AssertJUnit.assertEquals(List.of("See the sights"), night.getChosenModes(sights, "ThisGame"));
        final SpellAbility again = charm.getAdditionalAbilityList("Choices").get(3);
        again.setActivatingPlayer(ai);
        final int life = ai.getLife();
        AbilityUtils.resolve(again);
        AssertJUnit.assertEquals(life - 5, ai.getLife());
        final List<String> left = night.getChosenModes(sights, "ThisGame");
        AssertJUnit.assertTrue("all modes reset", left == null || left.isEmpty());
    }

    @Test
    public void testAnotherNightBreakfast() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card night = addCard("Another Night in Vegas", ai);
        final SpellAbility breakfast = night.getTriggers().get(0).ensureAbility().getAdditionalAbilityList("Choices").get(3);
        breakfast.setActivatingPlayer(ai);
        AbilityUtils.resolve(breakfast);
        AssertJUnit.assertEquals(2, ai.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getType().hasSubtype("Food")).count());
        AssertJUnit.assertTrue(game.getCardState(night).isInZone(ZoneType.Graveyard));
    }

    @Test
    public void testChaoticNightHasThirteenModes() {
        final Game game = initAndCreateGame();
        final Card night = addCard("A Chaotic Night in Vegas", game.getPlayers().get(1));
        AssertJUnit.assertEquals(13, night.getTriggers().get(0).ensureAbility().getAdditionalAbilityList("Choices").size());
    }

    @Test
    public void testArchenemysCharm() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card charm = addCardToZone("The Archenemy's Charm", ai, ZoneType.Hand);
        final SpellAbility useful = charm.getFirstSpellAbility().getAdditionalAbilityList("Choices").get(2);
        useful.setActivatingPlayer(ai);
        AbilityUtils.resolve(useful);
        AssertJUnit.assertTrue("the scheme was set in motion", ai.getCardsIn(ZoneType.Command).stream()
                .anyMatch(c -> c.getName().equals("Make Yourself Useful")));
    }

    @Test
    public void testMonsterMashUp() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card a = addCardToZone("Grizzly Bears", ai, ZoneType.Graveyard);
        final Card b = addCardToZone("Hill Giant", ai, ZoneType.Graveyard);
        final Card mash = addCardToZone("Monster Mash-Up", ai, ZoneType.Hand);
        final SpellAbility sa = mash.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        sa.getTargets().add(a);
        sa.getTargets().add(b);
        AbilityUtils.resolve(sa);
        final Card combined = ai.getCardsIn(ZoneType.Battlefield).stream().filter(Card::isCreature).findFirst().orElseThrow();
        AssertJUnit.assertEquals("2 + 3 power", 5, combined.getNetPower());
    }

    @Test
    public void testZassacreZirlUncommanders() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card one = addCard("Grizzly Bears", human);
        final Card two = addCardToZone("Hill Giant", human, ZoneType.Command);
        human.addCommander(one);
        human.addCommander(two);
        final Card zirl = addCard("The Zassacre Zirl", ai);
        final SpellAbility destroy = zirl.getTriggers().get(0).ensureAbility();
        destroy.setActivatingPlayer(ai);
        destroy.getTargets().add(one);
        final SpellAbility stop = destroy.getSubAbility();
        stop.setActivatingPlayer(ai);
        AbilityUtils.resolve(stop);
        AssertJUnit.assertFalse("two commanders: it stops being one", human.getCommanders().contains(one));
        AssertJUnit.assertTrue(human.getCommanders().contains(two));
    }
}
