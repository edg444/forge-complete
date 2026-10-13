package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Visitor from Planet Q (CMB1 #33), an Instant Creature. CR 304.4 keeps instants off the battlefield, which would leave
 * it (and the flash creatures it makes instants) unable to resolve as permanents; user ruling: an instant that also
 * has a permanent type enters the battlefield, and "creature cards you own" includes spells and permanents.
 */
public class VisitorFromPlanetQTest extends AITest {

    private void resolveAll(final Game game) {
        do {
            playUntilStackClear(game);
            game.getAction().checkStateEffects(true);
        } while (game.getStack().addAllTriggeredAbilitiesToStack());
    }

    private static void cast(Player p, Card c) {
        SpellAbility sa = c.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(p, sa, null));
    }

    @Test
    public void entersTheBattlefieldAtInstantSpeed() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCards("Island", 2, ai);
        Card visitor = addCardToZone("Visitor from Planet Q", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_END, opp);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue(visitor.getFirstSpellAbility().canCastTiming(ai));
        cast(ai, visitor);
        resolveAll(game);
        Card onField = game.getCardState(visitor);
        AssertJUnit.assertTrue(onField.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertTrue(onField.isInstant() && onField.isCreature());
        AssertJUnit.assertEquals(4, onField.getNetToughness());
        AssertJUnit.assertFalse(onField.isSpell());
        AssertJUnit.assertTrue(onField.getAbilityText().contains("you may draw a card, then discard a card"));
    }

    @Test
    public void flashCreaturesAreInstantsEverywhereAndLoot() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Visitor from Planet Q", ai);
        addCards("Forest", 4, ai);
        fillLibrary(ai, 10);
        Card viper = addCardToZone("Ambush Viper", ai, ZoneType.Hand);
        Card bears = addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        Card oppViper = addCardToZone("Ambush Viper", opp, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue(viper.isInstant());
        AssertJUnit.assertFalse(bears.isInstant());
        AssertJUnit.assertFalse(oppViper.isInstant());

        int library = ai.getCardsIn(ZoneType.Library).size();
        cast(ai, bears); // one card type: no loot
        resolveAll(game);
        AssertJUnit.assertEquals(library, ai.getCardsIn(ZoneType.Library).size());

        cast(ai, viper);
        AssertJUnit.assertTrue(game.getCardState(viper).isInstant()); // still an instant as a spell
        resolveAll(game);
        Card viperOnField = game.getCardState(viper);
        AssertJUnit.assertTrue(viperOnField.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertTrue(viperOnField.isInstant());
        AssertJUnit.assertEquals("looted once", library - 1, ai.getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertEquals(1, ai.getCardsIn(ZoneType.Graveyard).size());
    }
}
