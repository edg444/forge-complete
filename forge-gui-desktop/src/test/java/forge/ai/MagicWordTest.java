package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.cost.CostFlavorAction;
import forge.game.cost.CostPart;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Magic Word: the whisper cost names "the chosen word" in its text, and the word itself when you pay it. */
public class MagicWordTest extends AITest {

    @Test
    public void testWhisperPromptNamesTheWord() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card word = addCardToZone("Magic Word", me, ZoneType.Battlefield);
        SpellAbility tap = null;
        for (SpellAbility sa : word.getSpellAbilities()) {
            if (sa.isActivatedAbility()) {
                tap = sa;
            }
        }
        CostFlavorAction whisper = null;
        for (CostPart part : tap.getPayCosts().getCostParts()) {
            if (part instanceof CostFlavorAction) {
                whisper = (CostFlavorAction) part;
            }
        }
        AssertJUnit.assertTrue(tap.getDescription().startsWith("Whisper the chosen word: Tap enchanted creature."));
        AssertJUnit.assertEquals("Whisper the chosen word", whisper.getPrompt(tap));

        word.setChosenType("xyzzy");
        AssertJUnit.assertEquals("Whisper \"xyzzy\"", whisper.getPrompt(tap));
    }
}
