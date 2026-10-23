package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Unglued (with OLEP and O90P) script review and R&D's Secret Lair printed text (2026-10-23). */
public class UngluedAuditTest extends AITest {

    private static Card printing(final String name, final String set, final Player owner, final ZoneType zone) {
        final Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(name, set), owner);
        c.setGameTimestamp(owner.getGame().getNextTimestamp());
        owner.getZone(zone).add(c);
        return c;
    }

    private static int bowlOnce(final Game game, final Player p, final Player opp) {
        Card team = printing("Goblin Bowling Team", "UGL", p, ZoneType.Battlefield);
        game.getAction().checkStateEffects(true);
        SpellAbility sa = AbilityFactory.getAbility("DB$ DealDamage | Defined$ Player.Opponent | NumDmg$ 1", team);
        sa.setActivatingPlayer(p);
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        final int triggers = game.getStack().size();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
        final int lost = 20 - opp.getLife();
        AssertJUnit.assertTrue("1 plus one die roll: " + lost, lost >= 2 && lost <= 7);
        return triggers;
    }

    @Test
    public void testGoblinBowlingTeamTriggersOnceAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("R&D's Secret Lair", p);
        AssertJUnit.assertEquals(1, bowlOnce(game, p, game.getPlayers().get(0)));
    }

    @Test
    public void testGoblinBowlingTeamReplacesByOracle() {
        Game game = initAndCreateGame();
        AssertJUnit.assertEquals(0, bowlOnce(game, game.getPlayers().get(1), game.getPlayers().get(0)));
    }

    @Test
    public void testSpatulaTakesOnlyUngluedCardsAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("R&D's Secret Lair", p);
        Card spatula = printing("Spatula of the Ages", "UGL", p, ZoneType.Battlefield);
        Card imp = printing("Jumbo Imp", "UGL", p, ZoneType.Hand);
        Card buddy = printing("Bosom Buddy", "UNH", p, ZoneType.Hand);
        game.getAction().checkStateEffects(true);
        SpellAbility printed = null;
        SpellAbility oracle = null;
        for (SpellAbility s : spatula.getSpellAbilities()) {
            if (s.getDescription().contains("(As printed)")) {
                printed = s;
            } else {
                oracle = s;
            }
        }
        printed.setActivatingPlayer(p);
        oracle.setActivatingPlayer(p);
        AssertJUnit.assertTrue(printed.getRestrictions().canPlay(spatula, printed));
        AssertJUnit.assertFalse(oracle.getRestrictions().canPlay(spatula, oracle));
        final String valid = printed.getParam("ChangeType");
        AssertJUnit.assertTrue(imp.isValid(valid, p, spatula, printed));
        AssertJUnit.assertFalse(buddy.isValid(valid, p, spatula, printed));
    }
}
