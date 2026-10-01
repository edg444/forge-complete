package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

/**
 * Ninja (Unstable): "You may activate this card's augment ability any time you could cast an instant. / Whenever
 * this creature deals combat damage to a player, / Augment {2}{B} ({2}{B}, Reveal this card from your hand: Combine
 * it with target host. Augment only as—oh, never mind.)" - Oracle text and rulings checked against Scryfall
 * 2026-10-01.
 */
public class NinjaAugmentTest extends AITest {

    private static SpellAbility augmentAbility(Card c) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == ApiType.Augment) {
                return sa;
            }
        }
        return null;
    }

    private void lands(Player p, int n) {
        for (int i = 0; i < n; i++) {
            addCard("Swamp", p);
        }
    }

    @Test
    public void testTextReadsLikeOracle() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card ninja = addCardToZone("Ninja", me, ZoneType.Hand);
        AssertJUnit.assertEquals("Whenever this creature deals combat damage to a player, | You may activate this "
                + "card's augment ability any time you could cast an instant. | Augment {2}{B} ({2}{B}, Reveal this card "
                + "from your hand: Combine it with target host. Augment only as—oh, never mind.)",
                String.join(" | ", ninja.getAbilityText().trim().split("[\\r\\n]+")));
        // any other augment keeps the usual reminder, and the raw keyword line isn't shown
        Card rhino = addCardToZone("Rhino-", me, ZoneType.Hand);
        AssertJUnit.assertEquals("Whenever this creature blocks, | Augment {3}{W} ({3}{W}, Reveal this card from your "
                + "hand: Combine it with target host. Augment only as a sorcery.)",
                String.join(" | ", rhino.getAbilityText().trim().split("[\\r\\n]+")));
    }

    @Test
    public void testAugmentAnyTimeYouCouldCastAnInstant() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(me, 4);
        addCard("Adorable Kitten", me);
        SpellAbility ninja = augmentAbility(addCardToZone("Ninja", me, ZoneType.Hand));
        SpellAbility rhino = augmentAbility(addCardToZone("Rhino-", me, ZoneType.Hand));
        ninja.setActivatingPlayer(me);
        rhino.setActivatingPlayer(me);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, me);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(ninja.canPlay());
        AssertJUnit.assertTrue(rhino.canPlay());

        // the opponent's turn: only the Ninja
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(ninja.canPlay());
        AssertJUnit.assertFalse(rhino.canPlay());
    }

    @Test
    public void testNinjaKittenKeepsAttacking() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(me, 3);
        Card kitten = addCard("Adorable Kitten", me);
        kitten.setSickness(false);
        Card ninja = addCardToZone("Ninja", me, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, me, false);
        Combat combat = new Combat(me);
        combat.addAttacker(kitten, opp);
        combat.setBlocked(kitten, false);
        game.getPhaseHandler().setCombat(combat);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(combat.isUnblocked(kitten));

        // the AI waits for blocks and then puts it on the unblocked attacker
        SpellAbility sa = augmentAbility(ninja);
        sa.setActivatingPlayer(me);
        AiAbilityDecision decision = SpellApiToAi.Converter.get(sa).canPlayWithSubs(me, sa);
        AssertJUnit.assertTrue(decision.toString() + " canPlay=" + sa.canPlay(), decision.willingToPlay());
        AssertJUnit.assertEquals(kitten, sa.getTargets().getFirstTargetedCard());

        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("Ninja Kitten", kitten.getName());
        AssertJUnit.assertEquals(2, kitten.getNetPower());
        AssertJUnit.assertTrue(combat.isAttacking(kitten));

        // the Ninja's condition finished with the Kitten's effect
        Trigger damage = null;
        for (Trigger t : kitten.getTriggers()) {
            if (t.getMode() == TriggerType.DamageDone) {
                damage = t;
            }
        }
        AssertJUnit.assertNotNull(damage);
        AssertJUnit.assertTrue(damage.ensureAbility() != null);
        AssertJUnit.assertTrue(damage.toString().startsWith("Whenever this creature deals combat damage to a player, roll a six-sided die."));
    }

    @Test
    public void testAiWaitsForBlocks() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(me, 3);
        addCard("Adorable Kitten", me).setSickness(false);
        SpellAbility sa = augmentAbility(addCardToZone("Ninja", me, ZoneType.Hand));
        sa.setActivatingPlayer(me);
        SpellAbilityAi logic = SpellApiToAi.Converter.get(sa);

        // the Kitten can attack, so not before combat
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, me);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(logic.canPlayWithSubs(me, sa).willingToPlay());
        // after combat, as any augment would be
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, me);
        AssertJUnit.assertTrue(logic.canPlayWithSubs(me, sa).willingToPlay());
        // on the opponent's turn, at its end
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        AssertJUnit.assertFalse(logic.canPlayWithSubs(me, sa).willingToPlay());
        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        AssertJUnit.assertTrue(logic.canPlayWithSubs(me, sa).willingToPlay());
    }
}
