package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * A Real Handful (UNF #87), Scooch (UNF #91) and Aardwolf's Advantage (UNF #97), checked against Scryfall and their
 * rulings 2026-10-21.
 */
public class UnfinityBatch3Test extends AITest {

    @Test
    public void testHandsTouching() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card handful = addCard("A Real Handful", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(1, handful.getNetPower());
        handful.setChosenNumber(2);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(3, handful.getNetPower());
        AssertJUnit.assertTrue(handful.hasKeyword(Keyword.MENACE));
        AssertJUnit.assertFalse(handful.hasKeyword(Keyword.LIFELINK));
        handful.setChosenNumber(3);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(handful.hasKeyword(Keyword.LIFELINK));
        handful.setChosenNumber(0);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("the bonus turns off again", 1, handful.getNetPower());
    }

    @Test
    public void testScoochAddsToPowerForGood() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", p);
        addCardToZone("Island", p, ZoneType.Library);
        final Card scooch = addCardToZone("Scooch", p, ZoneType.Hand);
        final SpellAbility sa = scooch.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        for (final AbilitySub mode : CharmEffect.makePossibleOptions(sa)) {
            if (mode.getApi() == ApiType.Pump && !mode.isCurse()) {
                CharmEffect.chainAbilities(sa, new java.util.ArrayList<>(List.of(mode)));
            }
        }
        sa.getSubAbility().getTargets().add(bears);
        final int hand = p.getCardsIn(ZoneType.Hand).size();
        AbilityUtils.resolve(sa);
        AssertJUnit.assertEquals(3, bears.getNetPower());
        AssertJUnit.assertEquals(hand + 1, p.getCardsIn(ZoneType.Hand).size());
        game.getPhaseHandler().devModeSet(PhaseType.CLEANUP, p);
        game.getEndOfTurn().executeUntil();
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("no duration", 3, bears.getNetPower());
    }

    @Test
    public void testAardwolfsAdvantage() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card bears = addCard("Grizzly Bears", p);
        bears.setSickness(false);
        final Card elves = addCard("Llanowar Elves", opp);
        final Card spell = addCardToZone("Aardwolf's Advantage", p, ZoneType.Hand);
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(4, bears.getNetPower());
        AssertJUnit.assertFalse(bears.hasKeyword(Keyword.FIRST_STRIKE));

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, p, false);
        final Combat combat = new Combat(p);
        combat.addAttacker(bears, opp);
        combat.addBlocker(bears, elves);
        combat.setBlocked(bears, true);
        game.getPhaseHandler().setCombat(combat);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue("Llanowar comes after Grizzly", bears.hasKeyword(Keyword.FIRST_STRIKE));
    }
}
