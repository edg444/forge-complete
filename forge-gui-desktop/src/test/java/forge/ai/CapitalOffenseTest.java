package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardFactoryUtil;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * capital offense: -X/-X, X = capital letters in the target's rules text. Reminder and flavor text don't count;
 * ability words and every capital do (Unstable rulings). Oracle text below checked against Scryfall 2026-09-29.
 */
public class CapitalOffenseTest extends AITest {

    @Test
    public void testWhatCounts() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        AssertJUnit.assertEquals(0, CardFactoryUtil.getCapitalLetterCount(addCard("Grizzly Bears", me)));
        // "{T}: Add {G}." - the symbols print as icons, so only the A
        AssertJUnit.assertEquals(1, CardFactoryUtil.getCapitalLetterCount(addCard("Llanowar Elves", me)));
        // "Haste (This creature can attack and {T} ...)" - the reminder text's T doesn't count
        AssertJUnit.assertEquals(1, CardFactoryUtil.getCapitalLetterCount(addCard("Raging Goblin", me)));
        // "Landfall — Whenever a land ..." - the ability word counts
        AssertJUnit.assertEquals(2, CardFactoryUtil.getCapitalLetterCount(addCard("Scythe Leopard", me)));
        AssertJUnit.assertEquals(0, CardFactoryUtil.getCapitalLetterCount(
                addCardToZone("capital offense", me, ZoneType.Hand)));
    }

    @Test
    public void testShrinksByTheTargetsCount() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card leopard = addCard("Scythe Leopard", opp);
        Card bears = addCard("Grizzly Bears", opp);

        SpellAbility sa = addCardToZone("capital offense", me, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(me);
        sa.getTargets().add(bears);
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(2, bears.getNetToughness());

        sa.resetTargets();
        sa.getTargets().add(leopard);
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(game.getCardState(leopard).isInZone(ZoneType.Graveyard));
    }

    @Test
    public void testAiTargetsWhatItKills() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Grizzly Bears", opp);
        SpellAbility sa = addCardToZone("capital offense", me, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(me);
        // no capitals, so X would be 0
        AssertJUnit.assertFalse(SpellApiToAi.Converter.get(sa).canPlayWithSubs(me, sa).willingToPlay());

        Card leopard = addCard("Scythe Leopard", opp);
        sa.resetTargets();
        AssertJUnit.assertTrue(SpellApiToAi.Converter.get(sa).canPlayWithSubs(me, sa).willingToPlay());
        AssertJUnit.assertEquals(leopard, sa.getTargets().getFirstTargetedCard());
    }
}
