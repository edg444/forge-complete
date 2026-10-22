package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckSection;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/**
 * Barce, Friend Finder and The Collector (UNK mythics), checked against Scryfall 2026-10-22 (no rulings).
 */
public class UnknownEventBatch40Test extends AITest {

    private static Deck pair(final String a, final String b) {
        final Deck deck = new Deck();
        deck.getOrCreate(DeckSection.Commander).add(FModel.getMagicDb().getCommonCards().getCard(a));
        deck.getOrCreate(DeckSection.Commander).add(FModel.getMagicDb().getCommonCards().getCard(b));
        return deck;
    }

    @Test
    public void testBarcePartnersMonoLegends() {
        initAndCreateGame();
        AssertJUnit.assertNull(DeckFormat.Commander.getCommanderConformanceProblem(pair("Barce, Friend Finder", "Krenko, Mob Boss")));
        AssertJUnit.assertNotNull("two colors is too many",
                DeckFormat.Commander.getCommanderConformanceProblem(pair("Barce, Friend Finder", "Teysa Karlov")));
        AssertJUnit.assertNotNull("without Barce they don't partner",
                DeckFormat.Commander.getCommanderConformanceProblem(pair("Krenko, Mob Boss", "Kenrith, the Returned King")));
    }

    @Test
    public void testBarceReplacesACommander() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card barce = addCard("Barce, Friend Finder", ai);
        ai.addCommander(barce);
        barce.setCommander(true);
        final Card krenko = addCard("Krenko, Mob Boss", ai);
        final Card dead = game.getAction().moveToGraveyard(krenko, null);
        final SpellAbility trig = barce.getTriggers().get(0).ensureAbility();
        trig.setActivatingPlayer(ai);
        trig.setTriggeringObject(AbilityKey.NewCard, dead);
        trig.setTriggeringObject(AbilityKey.CardLKI, dead);
        AbilityUtils.resolve(trig);
        final Card inCommand = game.getCardState(dead);
        AssertJUnit.assertTrue(inCommand.isInZone(ZoneType.Command));
        AssertJUnit.assertTrue(ai.getCommanders().contains(inCommand));
        AssertJUnit.assertFalse("it replaced Barce, the only commander", ai.getCommanders().contains(barce));
    }

    @Test
    public void testCollectorLeavesTheAiUnsigned() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card collector = addCard("The Collector", ai);
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        AssertJUnit.assertFalse(bolt.isValid("Card.notKnownUnsigned", ai, collector, null));
        AssertJUnit.assertEquals("", forge.game.ability.effects.SignatureEffect.knownSigner(bolt));
        final Card bears = addCard("Grizzly Bears", human);
        final SpellAbility destroy = collector.getSpellAbilities().stream().filter(s -> s.isActivatedAbility()).findFirst().orElseThrow();
        destroy.setActivatingPlayer(ai);
        destroy.getTargets().add(bears);
        AbilityUtils.resolve(destroy);
        AssertJUnit.assertTrue("nothing discarded, nothing destroyed", bears.isInPlay());
    }
}
