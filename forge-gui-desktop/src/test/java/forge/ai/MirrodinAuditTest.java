package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Mirrodin script review and R&D's Secret Lair printed text (2026-10-23). */
public class MirrodinAuditTest extends AITest {

    private Card equippedBear(final Game game, final Player p) {
        Card hammer = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Loxodon Warhammer", "MRD"), p);
        p.getZone(ZoneType.Battlefield).add(hammer);
        Card bear = addCard("Grizzly Bears", p);
        hammer.attachToEntity(bear, null);
        game.getAction().checkStateEffects(true);
        return bear;
    }

    @Test
    public void testLoxodonWarhammerGrantsLifelinkByOracle() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        AssertJUnit.assertTrue(equippedBear(game, p).hasKeyword(Keyword.LIFELINK));
    }

    @Test
    public void testLoxodonWarhammerGrantsALifeTriggerAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("R&D's Secret Lair", p);
        Card bear = equippedBear(game, p);
        AssertJUnit.assertFalse(bear.hasKeyword(Keyword.LIFELINK));
        AssertJUnit.assertTrue(bear.hasKeyword(Keyword.TRAMPLE));
        AssertJUnit.assertEquals(5, bear.getNetPower());
        final int life = p.getLife();
        SpellAbility hit = AbilityFactory.getAbility("DB$ DealDamage | Defined$ Player.Opponent | NumDmg$ 4", bear);
        hit.setActivatingPlayer(p);
        AbilityUtils.resolve(hit);
        AssertJUnit.assertEquals(life, p.getLife());
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
        }
        AssertJUnit.assertEquals(life + 4, p.getLife());
    }

    @Test
    public void testGroffskithurReturnsOnlyFromYourGraveyard() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card groff = addCard("Groffskithur", p);
        Card mine = addCardToZone("Groffskithur", p, ZoneType.Graveyard);
        Card theirs = addCardToZone("Groffskithur", opp, ZoneType.Graveyard);
        SpellAbility sa = groff.getTriggers().get(0).ensureAbility();
        sa.setActivatingPlayer(p);
        AssertJUnit.assertTrue(sa.canTarget(mine));
        AssertJUnit.assertFalse(sa.canTarget(theirs));
    }
}
