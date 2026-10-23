package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Portal Second Age script review and R&D's Secret Lair printed text (2026-10-23). */
public class PortalSecondAgeAuditTest extends AITest {

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

    private int angelLife(final boolean lair, final ZoneType from) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card angel = printing("Angel of Mercy", "P02", p, from);
        game.getAction().moveToPlay(angel, p, null, null);
        resolveAll(game);
        return p.getLife();
    }

    @Test
    public void testAngelOfMercyFromHandAsPrinted() {
        AssertJUnit.assertEquals(23, angelLife(true, ZoneType.Hand));
    }

    @Test
    public void testAngelOfMercyNotFromGraveyardAsPrinted() {
        AssertJUnit.assertEquals(20, angelLife(true, ZoneType.Graveyard));
    }

    @Test
    public void testAngelOfMercyFromGraveyardByOracle() {
        AssertJUnit.assertEquals(23, angelLife(false, ZoneType.Graveyard));
    }

    @Test
    public void testReturnOfTheNightstalkersCountsAsFromHandAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("R&D's Secret Lair", p);
        printing("Brutal Nightstalker", "P02", p, ZoneType.Graveyard);
        Card ret = printing("Return of the Nightstalkers", "P02", p, ZoneType.Hand);
        SpellAbility sa = ret.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        sa.setHostCard(game.getAction().moveToStack(ret, sa));
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        AssertJUnit.assertEquals(1, game.getStack().size());
    }
}
