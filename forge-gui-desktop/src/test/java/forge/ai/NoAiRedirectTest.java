package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.ability.RedirectCombatAi;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** AILogic$ RedirectCombat (RedirectCombatAi): damage is moved once blocks are known, for what the move changes. */
public class NoAiRedirectTest extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        sa.resetTargets();
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    private static SpellAbility activated(Card c) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility() && !sa.isManaAbility()) {
                return sa;
            }
        }
        return null;
    }

    private void lands(Player p, String name, int n) {
        for (int i = 0; i < n; i++) {
            addCard(name, p);
        }
    }

    private Combat blocks(Game game, Player attacker, Card att, Player defender, Card blocker) {
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, attacker, false, 1);
        Combat combat = new Combat(attacker);
        combat.addAttacker(att, defender);
        if (blocker != null) {
            combat.addBlocker(att, blocker);
        }
        combat.setBlocked(att, blocker != null);
        game.getPhaseHandler().setCombat(combat);
        return combat;
    }

    // Mirror Strike: "All combat damage that would be dealt to you this turn by target unblocked creature is dealt to
    // its controller instead." - three points either way isn't worth a card, a lethal hit is
    @Test
    public void testMirrorStrikeTurnsLethalAround() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Plains", 4);
        Card giant = addCard("Hill Giant", opp);
        SpellAbility sa = addCardToZone("Mirror Strike", ai, ZoneType.Hand).getFirstSpellAbility();

        blocks(game, opp, giant, ai, null);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        ai.setLife(3, null);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(giant, sa.getTargetCard());
    }

    // Sivvi's Valor: "All damage that would be dealt to target creature this turn is dealt to you instead."
    @Test
    public void testSivvisValorSavesTheBlocker() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Plains", 3);
        Card angel = addCard("Serra Angel", ai);
        Card wurm = addCard("Craw Wurm", opp);
        SpellAbility sa = addCardToZone("Sivvi's Valor", ai, ZoneType.Hand).getFirstSpellAbility();

        blocks(game, opp, wurm, ai, angel);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(angel, sa.getTargetCard());
    }

    // Daughter of Autumn: "{W}: The next 1 damage that would be dealt to target white creature this turn is dealt to
    // Daughter of Autumn instead." - activated until the blocker survives, and no further
    @Test
    public void testDaughterOfAutumnStacksUntilEnough() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Plains", 4);
        Card daughter = addCard("Daughter of Autumn", ai);
        Card unicorn = addCard("Pearled Unicorn", ai);
        Card giant = addCard("Hill Giant", opp);
        SpellAbility sa = activated(daughter);

        blocks(game, opp, giant, ai, unicorn);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(unicorn, sa.getTargetCard());
        AbilityUtils.resolve(sa);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AbilityUtils.resolve(sa);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Lancers en-Kor: "{0}: The next 1 damage that would be dealt to this creature this turn is dealt to target
    // creature you control instead."
    @Test
    public void testEnKorShiftsDamageOffItself() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card lancers = addCard("Lancers en-Kor", ai);
        Card wall = addCard("Wall of Stone", ai);
        Card giant = addCard("Hill Giant", opp);
        SpellAbility sa = activated(lancers);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        blocks(game, opp, giant, ai, lancers);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(wall, sa.getTargetCard());
    }

    // Reflect Damage: "The next time a source of your choice would deal damage this turn, that damage is dealt to that
    // source's controller instead." - the source it means is the one ChooseSource is told
    @Test
    public void testReflectDamagePicksTheAttacker() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Plains", 2);
        lands(ai, "Island", 2);
        ai.setLife(3, null);
        Card giant = addCard("Hill Giant", opp);
        Card reflect = addCardToZone("Reflect Damage", ai, ZoneType.Hand);
        SpellAbility sa = reflect.getFirstSpellAbility();

        blocks(game, opp, giant, ai, null);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(giant, RedirectCombatAi.chosenSource(reflect));
    }

    // Kor Chant: "All damage that would be dealt this turn to target creature you control by a source of your choice is
    // dealt to another target creature instead."
    @Test
    public void testKorChantSendsTheBlowToTheirCreature() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Plains", 3);
        Card angel = addCard("Serra Angel", ai);
        Card wurm = addCard("Craw Wurm", opp);
        Card bears = addCard("Grizzly Bears", opp);
        Card chant = addCardToZone("Kor Chant", ai, ZoneType.Hand);
        SpellAbility sa = chant.getFirstSpellAbility();

        blocks(game, opp, wurm, ai, angel);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(angel, sa.getTargetCard());
        AssertJUnit.assertEquals(bears, sa.getSubAbility().getSubAbility().getTargetCard());
        AssertJUnit.assertEquals(wurm, RedirectCombatAi.chosenSource(chant));
    }

    // Soltari Guerrillas: "{0}: The next time this creature would deal combat damage to an opponent this turn, it deals
    // that damage to target creature instead." - a kill, but never at the price of the lethal hit
    @Test
    public void testSoltariGuerrillasKillsUnlessLethal() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card soltari = addCard("Soltari Guerrillas", ai);
        soltari.setSickness(false);
        Card bears = addCard("Grizzly Bears", opp);
        SpellAbility sa = activated(soltari);

        blocks(game, ai, soltari, opp, null);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(bears, sa.getTargetCard());

        opp.setLife(3, null);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
    }
}
