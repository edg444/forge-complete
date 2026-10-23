package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CounterEnumType;
import forge.game.combat.CombatUtil;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Mystery Booster 2 playtest cards (MB2), part 2, checked against Scryfall 2026-10-22 with their rulings.
 */
public class MysteryBooster2Playtest2Test extends AITest {

    private static SpellAbility svar(final Card c, final String name, final Player p) {
        final SpellAbility sa = AbilityFactory.getAbility(c.getSVar(name), c);
        sa.setActivatingPlayer(p);
        return sa;
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
    public void testNaturalize2() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card nat = addCardToZone("Naturalize 2", ai, ZoneType.Hand);
        human.createMonarchEffect("MB2");
        game.setMonarch(human);
        AbilityUtils.resolve(svar(nat, "DBTracker", ai));
        AssertJUnit.assertNull(game.getMonarch());
        final Card ring = addCard("Sol Ring", human);
        final SpellAbility obj = svar(nat, "DBObject", ai);
        obj.getTargets().add(ring);
        AbilityUtils.resolve(obj);
        AssertJUnit.assertFalse(game.getCardState(ring).isInPlay());
    }

    @Test
    public void testMicroscope() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card scope = addCard("Microscope", ai);
        final Card ring = addCardToZone("Sol Ring", ai, ZoneType.Graveyard);
        SpellAbility germ = null;
        for (final SpellAbility sa : scope.getSpellAbilities()) {
            if (sa.getApi() == forge.game.ability.ApiType.Animate) {
                germ = sa;
            }
        }
        germ.setActivatingPlayer(ai);
        germ.getTargets().add(ring);
        AbilityUtils.resolve(germ);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(ring.isCreature());
        AssertJUnit.assertTrue(ring.getType().hasCreatureType("Germ"));
        AssertJUnit.assertTrue(ring.isArtifact());
        AssertJUnit.assertTrue(ring.getColor().hasBlack());
    }

    @Test
    public void testLazotepArchway() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card archway = addCardToZone("Lazotep Archway", ai, ZoneType.Graveyard);
        AssertJUnit.assertTrue(archway.hasKeyword(Keyword.ETERNALIZE));
    }

    @Test
    public void testProcessingPlant() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        fillLibrary(human, 3);
        final Card plant = addCard("Processing Plant", ai);
        AssertJUnit.assertTrue(plant.getType().hasSubtype("Ulamog's"));
        AssertJUnit.assertTrue(plant.getType().hasSubtype("Power-Plant"));
        final int exiled = human.getCardsIn(ZoneType.Exile).size();
        AbilityUtils.resolve(svar(plant, "TrigProcess", ai));
        AssertJUnit.assertEquals("nothing of theirs in exile: their top card is exiled", exiled + 1, human.getCardsIn(ZoneType.Exile).size());
        plant.tap(true, null, null);
        final Card theirs = addCardToZone("Grizzly Bears", human, ZoneType.Exile);
        final int exiledBefore = human.getCardsIn(ZoneType.Exile).size();
        final int yardBefore = human.getCardsIn(ZoneType.Graveyard).size();
        AbilityUtils.resolve(svar(plant, "TrigProcess", ai));
        // either of their two exiled cards may be the one put back
        if (human.getCardsIn(ZoneType.Graveyard).size() == yardBefore + 1) {
            AssertJUnit.assertFalse("put back, so it untaps", plant.isTapped());
        } else {
            AssertJUnit.assertTrue("otherwise their top card is exiled", plant.isTapped() && human.getCardsIn(ZoneType.Exile).size() == exiledBefore + 1);
        }
    }

    @Test
    public void testPenumbraUmbra() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card giant = addCard("Hill Giant", ai);
        final Card umbra = addCard("Penumbra Umbra", ai);
        umbra.attachToEntity(giant, null);
        game.getAction().checkStateEffects(true);
        final SpellAbility kill = AbilityFactory.getAbility("DB$ Destroy | Defined$ CardUID_" + giant.getId(), giant);
        kill.setActivatingPlayer(ai);
        AbilityUtils.resolve(kill);
        resolveAll(game);
        AssertJUnit.assertTrue("umbra armor", giant.isInPlay());
        final Card token = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.isToken() && c.getName().equals("Hill Giant")).get(0);
        AssertJUnit.assertTrue(token.getColor().hasBlack());
        AssertJUnit.assertFalse(token.getColor().hasRed());
    }

    @Test
    public void testNarod() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Narod, the Beige Flower", ai);
        final Card giant = addCard("Hill Giant", ai);
        final Card token = addToken("c_1_1_shapeshifter_changeling", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("mana value 4", 4, giant.getNetCombatDamage());
        AssertJUnit.assertEquals(3, giant.getNetPower());
        AssertJUnit.assertEquals(0, token.getNetCombatDamage());
    }

    @Test
    public void testHish() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        addCard("Hish of the Snake Cult", ai);
        final Card serpent = addCard("Sea Serpent", ai);
        final Card small = addCard("Grizzly Bears", human);
        final Card big = addCard("Hill Giant", human);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(serpent.getType().hasCreatureType("Snake"));
        AssertJUnit.assertTrue(serpent.hasKeyword(Keyword.DEATHTOUCH));
        AssertJUnit.assertTrue(serpent.hasKeyword(Keyword.POISONOUS));
        AssertJUnit.assertFalse("daunt", CombatUtil.canBlock(serpent, small));
        AssertJUnit.assertTrue(CombatUtil.canBlock(serpent, big));
    }

    @Test
    public void testWowzer() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card wowzer = addCard("Wowzer, the Aspirational", ai);
        final SpellAbility sa = wowzer.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertEquals(0, AbilityUtils.calculateAmount(wowzer, "HasAll", sa));
        ai.addCounterInternal(CounterEnumType.ENERGY, 1, ai, false, null, null);
        for (final String token : new String[] {"c_a_blood_draw", "c_a_clue_draw", "c_a_food_sac", "c_a_map_sac_explore", "c_a_powerstone", "c_a_treasure_sac"}) {
            addToken(token, ai);
        }
        ai.createMonarchEffect("MB2");
        game.setMonarch(ai);
        ai.setBlessing(true, null);
        game.getAction().takeInitiative(ai, "MB2");
        AssertJUnit.assertEquals(1, AbilityUtils.calculateAmount(wowzer, "HasAll", sa));
    }
}
