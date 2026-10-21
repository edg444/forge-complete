package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.combat.CombatUtil;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityCantBeCast;

/**
 * Gobsmacked (UNF #11), checked against Scryfall and its 3 rulings 2026-10-21. Whether you can see the chosen person
 * is the Aura's chosen mode: a human reports changes (honor system); the AI's simulated person comes and goes at its
 * upkeeps (user ruling).
 */
public class GobsmackedTest extends AITest {

    private SpellAbility honorAction(final Card c, final Player p, final String words) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.hasParam("SpecialAction") && sa.getDescription().contains(words)) {
                sa.setActivatingPlayer(p);
                return sa;
            }
        }
        throw new AssertionError("no honor-system action: " + words);
    }

    private boolean locked(final Card c, final Player p) {
        final SpellAbility sa = c.getSpellAbilities().get(0);
        sa.setActivatingPlayer(p);
        return StaticAbilityCantBeCast.cantBeActivatedAbility(sa, c, p);
    }

    @Test
    public void testOnlyWhileYouCanSeeThem() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card sorcerer = addCard("Prodigal Sorcerer", opp);
        sorcerer.setSickness(false);
        final Card gob = addCard("Gobsmacked", p);
        gob.attachToEntity(sorcerer, null);
        gob.setChosenMode("Visible");
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(CombatUtil.canAttack(sorcerer));
        AssertJUnit.assertTrue(locked(sorcerer, opp));
        AssertJUnit.assertFalse("already in view", honorAction(gob, p, "again").canPlay());

        final SpellAbility gone = honorAction(gob, p, "anymore");
        AssertJUnit.assertTrue(gone.canPlay());
        AbilityUtils.resolve(gone);
        AssertJUnit.assertEquals("Hidden", gob.getChosenMode());
        AssertJUnit.assertTrue(CombatUtil.canAttack(sorcerer));
        AssertJUnit.assertFalse(locked(sorcerer, opp));
        AssertJUnit.assertFalse(honorAction(gob, p, "anymore").canPlay());

        AbilityUtils.resolve(honorAction(gob, p, "again"));
        AssertJUnit.assertEquals("Visible", gob.getChosenMode());
        AssertJUnit.assertFalse(CombatUtil.canAttack(sorcerer));
    }

    @Test
    public void testTheAiChoosesSomeoneItCanSee() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card gob = addCard("Gobsmacked", ai);
        final SpellAbility choose = AbilityFactory.getAbility(gob.getSVar("ChoosePerson"), gob);
        choose.setActivatingPlayer(ai);
        AbilityUtils.resolve(choose);
        AssertJUnit.assertEquals("Visible", gob.getChosenMode());
        AssertJUnit.assertFalse(((PlayerControllerAi) ai.getController()).getAi()
                .canPlaySa(honorAction(gob, ai, "anymore")) == AiPlayDecision.WillPlay);
    }

    @Test
    public void testTheAisPersonComesAndGoes() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card gob = addCard("Gobsmacked", ai);
        gob.setChosenMode("Visible");
        final SpellAbility wander = gob.getTriggers().get(0).ensureAbility();
        wander.setActivatingPlayer(ai);

        gob.setSVar("WanderRoll", "Number$5");
        AbilityUtils.resolve(wander);
        AssertJUnit.assertEquals("stays", "Visible", gob.getChosenMode());
        gob.setSVar("WanderRoll", "Number$11");
        AbilityUtils.resolve(wander);
        AssertJUnit.assertEquals("a high roll doesn't send them away", "Visible", gob.getChosenMode());
        gob.setSVar("WanderRoll", "Number$1");
        AbilityUtils.resolve(wander);
        AssertJUnit.assertEquals("wanders off, and doesn't come straight back", "Hidden", gob.getChosenMode());
        gob.setSVar("WanderRoll", "Number$10");
        AbilityUtils.resolve(wander);
        AssertJUnit.assertEquals("Hidden", gob.getChosenMode());
        gob.setSVar("WanderRoll", "Number$11");
        AbilityUtils.resolve(wander);
        AssertJUnit.assertEquals("comes back", "Visible", gob.getChosenMode());
    }
}
