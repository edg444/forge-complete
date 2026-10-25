package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.ability.SacrificePayoffAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;

/** Second pass, batch 97. */
public class NoAiBatch97Test extends AITest {

    // Disciple of Bolas: of its creatures, the one whose power buys the most life and cards for what it's worth
    @Test
    public void testDiscipleOfBolasSacrificesTheBigOne() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card disciple = addCard("Disciple of Bolas", ai);
        Card wurm = addCard("Craw Wurm", ai);
        addCard("Llanowar Elves", ai);
        SpellAbility sa = null;
        for (Trigger t : disciple.getTriggers()) {
            if (t.ensureAbility() != null) {
                sa = t.ensureAbility();
            }
        }
        sa.setActivatingPlayer(ai);

        AssertJUnit.assertTrue(SpellApiToAi.Converter.get(sa).doTrigger(ai, sa, true));
        AssertJUnit.assertEquals(wurm, SacrificePayoffAi.planned(sa).get(0));
    }
}
