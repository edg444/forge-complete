package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Ice Age script review and R&D's Secret Lair printed text (2026-10-23). */
public class IceAgeAuditTest extends AITest {

    private static Card printing(final String name, final String set, final Player owner, final ZoneType zone) {
        final Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(name, set), owner);
        c.setGameTimestamp(owner.getGame().getNextTimestamp());
        owner.getZone(zone).add(c);
        return c;
    }

    private static void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    @Test
    public void testDwarvenArmoryWorksInAnyUpkeep() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card armory = addCard("Dwarven Armory", p);
        game.getPhaseHandler().devModeSet(PhaseType.UPKEEP, opp);
        SpellAbility sa = null;
        for (SpellAbility s : armory.getSpellAbilities()) {
            if (s.getApi() == ApiType.PutCounter) {
                sa = s;
            }
        }
        sa.setActivatingPlayer(p);
        AssertJUnit.assertTrue(sa.getRestrictions().canPlay(armory, sa));
    }

    @Test
    public void testEnduringRenewalDrawsThenDiscardsAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("R&D's Secret Lair", p);
        printing("Enduring Renewal", "ICE", p, ZoneType.Battlefield);
        Card bear = addCardToZone("Grizzly Bears", p, ZoneType.Library);
        game.getAction().checkStateEffects(true);
        p.drawCard();
        resolveAll(game);
        AssertJUnit.assertEquals(1, p.getNumDrawnThisTurn());
        AssertJUnit.assertTrue(p.getCardsIn(ZoneType.Graveyard).contains(bear));
    }

    @Test
    public void testHecatombStaysAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("R&D's Secret Lair", p);
        addCard("Grizzly Bears", p);
        addCard("Grizzly Bears", p);
        Card heca = printing("Hecatomb", "ICE", p, ZoneType.Hand);
        heca = game.getAction().moveToPlay(heca, p, null, null);
        resolveAll(game);
        AssertJUnit.assertTrue(heca.isInPlay());
        AssertJUnit.assertTrue(p.getCreaturesInPlay().isEmpty());
    }

    @Test
    public void testPitTrapNeedsAttackerOnYouAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card trap = printing("Pit Trap", "ICE", p, ZoneType.Battlefield);
        addCard("R&D's Secret Lair", p);
        game.getAction().checkStateEffects(true);
        int usable = 0;
        for (SpellAbility sa : trap.getSpellAbilities()) {
            if (sa.getApi() != ApiType.Destroy) {
                continue;
            }
            sa.setActivatingPlayer(p);
            if (sa.getRestrictions().canPlay(trap, sa)) {
                usable++;
                AssertJUnit.assertEquals("Creature.attackingYou+withoutFlying", sa.getTargetRestrictions().getValidTgts()[0]);
            }
        }
        AssertJUnit.assertEquals(1, usable);
    }
}
