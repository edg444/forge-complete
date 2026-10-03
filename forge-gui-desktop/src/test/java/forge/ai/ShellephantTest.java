package forge.ai;

import java.util.ArrayList;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.AbilityStatic;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Shellephant (Unstable) - Oracle text, the three Scryfall rulings and the Unstable FAQ entry checked 2026-10-03.
 */
public class ShellephantTest extends AITest {

    private static SpellAbility ability(Card c, ApiType api) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == api) {
                return sa;
            }
        }
        return null;
    }

    /** Activates the {0} ability choosing mode 0 (1/4) or 1 (3/3). */
    private static void define(Card shell, Player p, int mode) {
        SpellAbility charm = ability(shell, ApiType.Charm);
        charm.setActivatingPlayer(p);
        List<AbilitySub> choices = CharmEffect.makePossibleOptions(charm);
        charm.setSubAbility(null);
        CharmEffect.chainAbilities(charm, new ArrayList<>(List.of(choices.get(mode))));
        AbilityUtils.resolve(charm);
    }

    @Test
    public void testDefinedInHandStaysDefined() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card shell = addCardToZone("Shellephant", me, ZoneType.Hand);
        AssertJUnit.assertEquals(0, shell.getNetToughness());
        define(shell, me, 0);
        AssertJUnit.assertEquals(1, shell.getNetPower());
        AssertJUnit.assertEquals(4, shell.getNetToughness());
        Card onField = game.getAction().moveToPlay(shell, me, null, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(onField.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertEquals(4, onField.getNetToughness());
        define(onField, me, 1);
        AssertJUnit.assertEquals(3, onField.getNetPower());
        AssertJUnit.assertEquals(3, onField.getNetToughness());
    }

    @Test
    public void testUndefinedIsZeroZero() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card shell = addCardToZone("Shellephant", me, ZoneType.Hand);
        Card onField = game.getAction().moveToPlay(shell, me, null, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(game.getCardState(onField).isInZone(ZoneType.Graveyard));
    }

    @Test
    public void testAiDefinesItOnceAsAThreeThree() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card shell = addCardToZone("Shellephant", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        playUntilPhase(game, PhaseType.END_OF_TURN);
        Card now = game.getCardState(shell);
        AssertJUnit.assertEquals(3, now.getNetPower());
        AssertJUnit.assertEquals(3, now.getNetToughness());
    }

    @Test
    public void testTypesChangeAnywhereAndStay() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card shell = addCardToZone("Shellephant", me, ZoneType.Hand);
        AssertJUnit.assertTrue(shell.getType().hasCreatureType("Turtle"));
        AssertJUnit.assertTrue(shell.getType().hasCreatureType("Elephant"));
        SpellAbility types = ability(shell, ApiType.GenericChoice);
        // the type line is a special action, not an ability: it never uses the stack
        AssertJUnit.assertTrue(types instanceof AbilityStatic);
        SpellAbility turtle = types.getAdditionalAbilityList("Choices").get(0);
        turtle.setActivatingPlayer(me);
        AbilityUtils.resolve(turtle);
        AssertJUnit.assertTrue(shell.getType().hasCreatureType("Turtle"));
        AssertJUnit.assertFalse(shell.getType().hasCreatureType("Elephant"));
        define(shell, me, 1);
        Card onField = game.getAction().moveToPlay(shell, me, null, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(onField.getType().hasCreatureType("Turtle"));
        AssertJUnit.assertFalse(onField.getType().hasCreatureType("Elephant"));
    }
}
