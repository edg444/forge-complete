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
 * Trigger Happy (UNF #125), Well Done (UNF #128) and Alpha Guard (UNF #129), checked against Scryfall and their
 * rulings 2026-10-21.
 */
public class UnfinityRedGreenBatchTest extends AITest {

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
    public void testTriggerHappyRetriggersAnEtb() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 3; i++) {
            addCardToZone("Island", ai, ZoneType.Library);
        }
        final Card elemental = addCard("Mulldrifter", ai);
        final Card spell = addCardToZone("Trigger Happy", ai, ZoneType.Hand);
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        sa.getTargets().add(elemental);
        final int hand = ai.getCardsIn(ZoneType.Hand).size();
        AbilityUtils.resolve(sa);
        resolveAll(game);
        AssertJUnit.assertEquals("Mulldrifter's draw two triggered", hand + 2, ai.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void testWellDoneRoastsRares() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card angel = addCard("Serra Angel", opp);
        final boolean rare = angel.isValid("Card.RarityRare,Card.RarityMythic", opp, angel, null);
        final Card spell = addCardToZone("Well Done", p, ZoneType.Hand);
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        sa.getTargets().add(angel);
        final int life = opp.getLife();
        AbilityUtils.resolve(sa);
        AssertJUnit.assertEquals(5, angel.getDamage());
        AssertJUnit.assertEquals(rare ? life - 3 : life, opp.getLife());
    }

    @Test
    public void testAlphaGuard() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card guard = addCard("Alpha Guard", p);
        guard.setChosenType("G");
        final Card goblin = addCard("_____ Goblin", p);
        final Card bears = addCard("Grizzly Bears", p);
        final Card elves = addCard("Llanowar Elves", p);
        SpellAbility untap = null;
        for (final SpellAbility s : guard.getSpellAbilities()) {
            if (s.isActivatedAbility()) {
                untap = s;
            }
        }
        untap.setActivatingPlayer(p);
        AssertJUnit.assertTrue(untap.canTarget(bears));
        AssertJUnit.assertTrue("_____ Goblin starts with G", untap.canTarget(goblin));
        AssertJUnit.assertFalse(untap.canTarget(elves));
        AssertJUnit.assertFalse("another permanent", untap.canTarget(guard));
    }
}
