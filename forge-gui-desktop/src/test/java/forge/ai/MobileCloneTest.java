package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.combat.CombatUtil;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Mobile Clone (UNF #51), checked against Scryfall and its 4 rulings 2026-10-21: a photocopy of the printed creature with
 * its counters and attachments in the shot, all of it part of the copy.
 */
public class MobileCloneTest extends AITest {

    private Card photocopy(final Game game, final Player p, final Card target) {
        final Card spell = addCardToZone("Mobile Clone", p, ZoneType.Hand);
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        sa.getTargets().add(target);
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);
        return p.getCreaturesInPlay().stream().filter(c -> c.isToken()).findFirst().orElse(null);
    }

    @Test
    public void testCountersAndAttachmentsAreInThePicture() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card bears = addCard("Grizzly Bears", opp);
        bears.addCounterInternal(CounterEnumType.P1P1, 2, opp, false, null, null);
        bears.addCounterInternal(CounterType.getType("Flying"), 1, opp, false, null, null);
        addCard("Bonesplitter", opp).attachToEntity(bears, null);
        addCard("Pacifism", p).attachToEntity(bears, null);
        game.getAction().checkStateEffects(true);

        final Card photo = photocopy(game, p, bears);
        AssertJUnit.assertNotNull(photo);
        AssertJUnit.assertEquals("Grizzly Bears", photo.getName());
        AssertJUnit.assertEquals("2 + 2 counters + 2 Bonesplitter", 6, photo.getNetPower());
        AssertJUnit.assertEquals(4, photo.getNetToughness());
        AssertJUnit.assertTrue(photo.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertFalse("no counters to move: they're in the picture", photo.hasCounters());
        AssertJUnit.assertFalse("nothing attached to it", photo.isEquipped() || photo.isEnchanted());
        photo.setSickness(false);
        AssertJUnit.assertFalse("Pacifism is in the picture", CombatUtil.canAttack(photo));
    }

    @Test
    public void testCopyEffectsDontShow() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", p);
        bears.addChangedName("Serra Angel", false, game.getNextTimestamp(), 0);
        final Card photo = photocopy(game, p, bears);
        AssertJUnit.assertEquals("the printed card", "Grizzly Bears", photo.getName());
    }
}
