package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Park Re-Entry (UNF #22), checked against Scryfall and its 9 rulings 2026-10-21: up to two creature cards that each
 * have a hat and/or mana value 3 or less. A hat Forge doesn't know of is the caster's word, asked as it resolves.
 */
public class ParkReEntryTest extends AITest {

    private SpellAbility cast(final Player p, final Card... targets) {
        final Card spell = addCardToZone("Park Re-Entry", p, ZoneType.Hand);
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        for (final Card t : targets) {
            sa.getTargets().add(t);
        }
        return sa;
    }

    @Test
    public void testSmallOnesComeBackAndBigOnesNeedAHat() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card bears = addCardToZone("Grizzly Bears", p, ZoneType.Graveyard);
        final Card angel = addCardToZone("Serra Angel", p, ZoneType.Graveyard);
        AbilityUtils.resolve(cast(p, bears, angel));
        AssertJUnit.assertTrue(game.getCardState(bears).isInZone(ZoneType.Battlefield));
        AssertJUnit.assertTrue("the AI answers no for a hat it can't see",
                game.getCardState(angel).isInZone(ZoneType.Graveyard));
    }

    @Test
    public void testAKnownHatBringsBackABigOne() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card angel = addCardToZone("Serra Angel", p, ZoneType.Graveyard);
        angel.setHatInArtClaim(true);
        AbilityUtils.resolve(cast(p, angel));
        AssertJUnit.assertTrue(game.getCardState(angel).isInZone(ZoneType.Battlefield));
    }

    @Test
    public void testTheAiTargetsOnlyWhatItCanVouchFor() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card angel = addCardToZone("Serra Angel", ai, ZoneType.Graveyard);
        final Card bears = addCardToZone("Grizzly Bears", ai, ZoneType.Graveyard);
        final Card elves = addCardToZone("Llanowar Elves", ai, ZoneType.Graveyard);
        for (int i = 0; i < 5; i++) {
            addCard("Plains", ai);
        }
        final SpellAbility sa = cast(ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa));
        AssertJUnit.assertEquals(2, sa.getTargets().size());
        AssertJUnit.assertTrue(sa.getTargets().contains(bears));
        AssertJUnit.assertTrue(sa.getTargets().contains(elves));
        AssertJUnit.assertFalse(sa.getTargets().contains(angel));
    }
}
