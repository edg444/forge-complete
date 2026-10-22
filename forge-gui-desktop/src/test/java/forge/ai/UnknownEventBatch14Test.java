package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Guild Pact, The Five Stages of Grief, Call Up Emrakul to Help, Everyone's an Impostor, Pulse of the Hunter Maze, The
 * Paradise Bird, Phyrexian Cytoshaper, Maître Tree, Grek the Ogre and Land Station (UNK), checked against Scryfall
 * 2026-10-21 (no rulings).
 */
public class UnknownEventBatch14Test extends AITest {

    private static SpellAbility activated(final Card c) {
        return c.getSpellAbilities().stream().filter(SpellAbility::isActivatedAbility).findFirst().orElseThrow();
    }

    @Test
    public void testGuildPactMana() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card pact = addCardToZone("Guild Pact", ai, ZoneType.Hand);
        final SpellAbility wu = pact.getFirstSpellAbility().getAdditionalAbilityList("Choices").get(0);
        wu.setActivatingPlayer(ai);
        AbilityUtils.resolve(wu);
        AssertJUnit.assertEquals(2, ai.getManaPool().totalMana());
    }

    @Test
    public void testEveryonesAnImpostor() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        // conspiracies are variant cards
        final Card conspiracy = Card.fromPaperCard(forge.StaticData.instance().getVariantCards().getCard("Everyone's an Impostor"), ai);
        game.getAction().moveToCommand(conspiracy, null);
        final Card bears = addCard("Grizzly Bears", game.getPlayers().get(0));
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertTrue(bears.hasKeyword(Keyword.CHANGELING));
        AssertJUnit.assertTrue(bears.getType().hasCreatureType("Elf"));
    }

    @Test
    public void testPulseReturns() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        ai.setCounters(CounterEnumType.POISON, 3, null, false);
        final Card pulse = addCardToZone("Pulse of the Hunter Maze", ai, ZoneType.Hand);
        final SpellAbility sa = pulse.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        sa.getTargets().add(human);
        AbilityUtils.calculateAmount(pulse, "Mine", sa);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertEquals(1, human.getPoisonCounters());
        AssertJUnit.assertEquals("3 is more than 1", 1, AbilityUtils.calculateAmount(pulse, "Mine", sa) > AbilityUtils.calculateAmount(pulse, "Theirs", sa) ? 1 : 0);
    }

    @Test
    public void testParadiseBird() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card bird = addCard("The Paradise Bird", ai);
        addCard("Forest", ai);
        final SpellAbility sa = activated(bird);
        sa.setActivatingPlayer(ai);
        AbilityUtils.resolve(sa);
        final Card token = ai.getCardsIn(ZoneType.Battlefield).stream().filter(Card::isToken).findFirst().orElseThrow();
        AssertJUnit.assertEquals("Birds of Paradise", token.getName());
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertEquals("other Birds get +1/+1", 1, token.getNetPower());
    }

    @Test
    public void testCytoshaperArtifactEvolve() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card cyto = addCard("Phyrexian Cytoshaper", ai);
        final SpellAbility etb = cyto.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        AbilityUtils.resolve(etb);
        final Card incubator = ai.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getType().hasSubtype("Incubator")).findFirst().orElseThrow();
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertTrue("the Incubator has artifact evolve", incubator.getTriggers().stream()
                .anyMatch(t -> t.getParam("TriggerDescription").startsWith("Artifact evolve")));
        AssertJUnit.assertEquals(1, incubator.getCounters(CounterEnumType.P1P1));
    }

    @Test
    public void testGrekCountsTokenNames() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card grek = addCard("Grek the Ogre", ai);
        final SpellAbility attack = grek.getTriggers().get(0).ensureAbility();
        attack.setActivatingPlayer(ai);
        for (final SpellAbility choice : attack.getAdditionalAbilityList("Choices")) {
            choice.setActivatingPlayer(ai);
            AbilityUtils.resolve(choice);
        }
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertEquals("six different tokens", 4 + 6, grek.getNetPower());
    }

    @Test
    public void testFiveStagesAndEmrakul() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card saga = addCardToZone("The Five Stages of Grief", ai, ZoneType.Hand);
        AssertJUnit.assertEquals(5, saga.getTriggers().size());
        final Card call = addCardToZone("Call Up Emrakul to Help", ai, ZoneType.Hand);
        AssertJUnit.assertTrue(call.hasKeyword(Keyword.KICKER));
        final Card station = addCardToZone("Land Station", ai, ZoneType.Hand);
        AssertJUnit.assertTrue(station.hasKeyword(Keyword.STATION));
        final Card tree = addCardToZone("Maître Tree", ai, ZoneType.Hand);
        AssertJUnit.assertEquals(1, tree.getTriggers().size());
    }
}
