package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Stronghold script review and R&D's Secret Lair printed text (2026-10-23). */
public class StrongholdAuditTest extends AITest {

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
    public void testMoxDiamondEntersBeforeItsDiscardAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("R&D's Secret Lair", p);
        Card mox = printing("Mox Diamond", "STH", p, ZoneType.Hand);
        mox = game.getAction().moveToPlay(mox, p, null, null);
        AssertJUnit.assertTrue("in play while the trigger waits", mox.isInPlay());
        resolveAll(game);
        AssertJUnit.assertTrue("no land to discard, so it's sacrificed", p.getCardsIn(ZoneType.Graveyard).contains(mox));
    }

    @Test
    public void testMoxDiamondNeverEntersByOracle() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("R&D's Secret Lair", p);
        Card mox = printing("Mox Diamond", "V10", p, ZoneType.Hand);
        game.getAction().moveToPlay(mox, p, null, null);
        AssertJUnit.assertTrue(p.getCardsIn(ZoneType.Battlefield).stream().noneMatch(c -> c.getName().equals("Mox Diamond")));
    }

    private static int scavengerCounters(final Game game, final Player p, final boolean printed) {
        Card skel = printing("Skeleton Scavengers", "STH", p, ZoneType.Battlefield);
        skel.setCounters(CounterEnumType.P1P1, 1);
        game.getAction().checkStateEffects(true);
        SpellAbility sa = null;
        for (SpellAbility s : skel.getSpellAbilities()) {
            if (s.getDescription().contains("(As printed)") == printed) {
                sa = s;
            }
        }
        sa.setActivatingPlayer(p);
        AssertJUnit.assertTrue(sa.getRestrictions().canPlay(skel, sa));
        AbilityUtils.resolve(sa);
        return skel.getCounters(CounterEnumType.P1P1);
    }

    @Test
    public void testSkeletonScavengersCounterOnResolutionAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("R&D's Secret Lair", p);
        AssertJUnit.assertEquals(2, scavengerCounters(game, p, true));
    }

    @Test
    public void testSkeletonScavengersCounterOnlyOnRegenerationByOracle() {
        Game game = initAndCreateGame();
        AssertJUnit.assertEquals(1, scavengerCounters(game, game.getPlayers().get(1), false));
    }
}
