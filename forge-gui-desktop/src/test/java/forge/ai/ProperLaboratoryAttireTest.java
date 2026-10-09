package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Proper Laboratory Attire (Unstable) - Oracle text and its two Scryfall rulings checked 2026-10-09.
 */
public class ProperLaboratoryAttireTest extends AITest {

    @Test
    public void protectionFromDieRolls() {
        final Game game = initAndCreateGame();
        final Player opp = game.getPlayers().get(0);
        final Player me = game.getPlayers().get(1);
        final Card bear = addCard("Runeclaw Bear", me);
        bear.setSickness(false);
        final Card attire = addCard("Proper Laboratory Attire", me);
        attire.attachToEntity(bear, null, true);

        final Card buzzbark = addCard("Ol' Buzzbark", opp);       // rolls dice
        final Card wall = addCard("Wall of Fortune", opp);         // lets a player reroll - that counts too
        final Card squirrel = addCard("Snickering Squirrel", opp); // only changes results - doesn't count
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertEquals(4, bear.getNetPower());
        AssertJUnit.assertEquals(3, bear.getNetToughness());

        final Combat combat = new Combat(me);
        combat.addAttacker(bear, opp);
        AssertJUnit.assertFalse(CombatUtil.canBlock(bear, buzzbark, combat));
        AssertJUnit.assertFalse(CombatUtil.canBlock(bear, wall, combat));
        AssertJUnit.assertTrue(CombatUtil.canBlock(bear, squirrel, combat));

        // a spell that rolls a die can't target it
        final Card hammer = addCardToZone("Hammer Helper", opp, ZoneType.Hand);
        final SpellAbility sa = hammer.getFirstSpellAbility();
        sa.setActivatingPlayer(opp);
        AssertJUnit.assertFalse(bear.canBeTargetedBy(sa));
    }
}
