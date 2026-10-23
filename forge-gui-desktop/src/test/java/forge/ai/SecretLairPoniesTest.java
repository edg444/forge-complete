package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.effects.ToyEffect;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Secret Lair's Applejack, Fluttershy and Pinkie Pie (SLD 1537-1539), checked against Scryfall 2026-10-23 (no rulings).
 */
public class SecretLairPoniesTest extends AITest {

    private void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    private void cast(final Game game, final Player p, final Card spell) {
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        final Card onStack = game.getAction().moveToStack(spell, sa);
        sa.setHostCard(onStack);
        game.getStack().add(sa);
        resolveAll(game);
    }

    @Test
    public void testPinkiePie() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card pinkie = addCard("Pinkie Pie", ai);
        game.getAction().checkStateEffects(true);

        cast(game, ai, addCardToZone("Fencing Ace", ai, ZoneType.Hand));
        final List<Card> treasures = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getType().hasSubtype("Treasure"));
        AssertJUnit.assertEquals("a smile in its art", 1, treasures.size());
        AssertJUnit.assertTrue("tapped", treasures.get(0).isTapped());
        cast(game, ai, addCardToZone("Grizzly Bears", ai, ZoneType.Hand));
        AssertJUnit.assertEquals("no smile, no Treasure", 1, CardLists.count(ai.getCardsIn(ZoneType.Battlefield), c -> c.getType().hasSubtype("Treasure")));

        // every creature you control is in your party, and it's always full
        final Card archpriest = addCardToZone("Archpriest of Iona", ai, ZoneType.Hand);
        final SpellAbility asks = archpriest.getFirstSpellAbility();
        asks.setActivatingPlayer(ai);
        AssertJUnit.assertEquals(3, AbilityUtils.calculateAmount(archpriest, "X", asks));
        AssertJUnit.assertEquals(4, AbilityUtils.calculateAmount(archpriest, "FullParty", asks));
        game.getAction().exile(pinkie, null, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("a Human Soldier and a Bear: no Clerics, Rogues, Warriors or Wizards", 0, AbilityUtils.calculateAmount(archpriest, "X", asks));
        AssertJUnit.assertEquals(0, AbilityUtils.calculateAmount(archpriest, "FullParty", asks));
    }

    @Test
    public void testFluttershy() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card flutter = addCard("Fluttershy", ai);
        flutter.setSickness(false);
        addCard("Island", ai);
        final Card lions = addCard("Savannah Lions", opp);
        final Card bears = addCard("Grizzly Bears", opp);
        game.getAction().checkStateEffects(true);

        final SpellAbility sa = flutter.getSpellAbilities().stream().filter(SpellAbility::isActivatedAbility).findFirst().get();
        sa.setActivatingPlayer(ai);
        sa.getTargets().add(opp);
        sa.getSubAbility().getSubAbility().getSubAbility().getTargets().add(bears);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertEquals("a tail in its art", 1, lions.getCounters(forge.game.card.CounterEnumType.P1P1));
        AssertJUnit.assertEquals(0, bears.getCounters(forge.game.card.CounterEnumType.P1P1));
        AssertJUnit.assertTrue("stared down", bears.hasKeyword("CARDNAME can't attack or block."));
        AssertJUnit.assertFalse(lions.hasKeyword("CARDNAME can't attack or block."));
    }

    @Test
    public void testApplejack() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, ai);
        final Card applejack = addCard("Applejack", ai);
        for (int i = 0; i < 10; i++) {
            addCardToZone("Grizzly Bears", ai, ZoneType.Library);
        }
        final List<String> box = ToyEffect.toyBox(ai, applejack);
        AssertJUnit.assertEquals("the AI's four toys", 4, box.size());
        final SpellAbility toy = applejack.getTriggers().iterator().next().ensureAbility();
        toy.setActivatingPlayer(ai);
        for (int i = 0; i < 5; i++) {
            AbilityUtils.resolve(toy);
            resolveAll(game);
        }
        final List<Card> tokens = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.isToken() && c.isCreature());
        AssertJUnit.assertEquals("each toy once while its token stays", 4, tokens.size());
        for (final String t : box) {
            final String[] f = t.split("\\|", -1);
            final Card token = CardLists.filter(tokens, c -> c.getName().equals(f[0])).getFirst();
            AssertJUnit.assertEquals(2, token.getNetPower());
            AssertJUnit.assertEquals(2, token.getNetToughness());
            AssertJUnit.assertEquals(f[0] + " wings", Boolean.parseBoolean(f[3]), token.hasKeyword(Keyword.FLYING));
            for (final String type : f[2].split(" ")) {
                AssertJUnit.assertTrue(token.getType().hasCreatureType(type));
            }
        }
        final long neither = box.stream().filter(t -> t.endsWith("|false|false")).count();
        AssertJUnit.assertEquals("a Food for each toy with no wings or horn", neither,
                CardLists.count(ai.getCardsIn(ZoneType.Battlefield), c -> c.getType().hasSubtype("Food")));
    }
}
