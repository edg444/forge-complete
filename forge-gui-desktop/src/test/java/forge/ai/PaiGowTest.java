package forge.ai;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameEndReason;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.PaiGowMagic;
import forge.game.card.Card;
import forge.game.mana.ManaPool;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Pai Gow Magic and Booster Blitz (Frank Karsten's rules, mtg.wiki/page/Pai_Gow_Magic): the piles, the five
 * separate games, who plays first, how the match ends, and the no-library, 5-life, unlimited-mana game itself.
 */
public class PaiGowTest extends AITest {
    private static final String[] PACK = {"Grizzly Bears", "Lightning Bolt", "Llanowar Elves", "Giant Growth", "Shock",
            "Divination", "Counterspell", "Doom Blade", "Serra Angel", "Shivan Dragon", "Wrath of God",
            "Swords to Plowshares", "Dark Ritual", "Hill Giant", "Island"};

    private enum Result { A_WINS, B_WINS, DRAW }

    private static final class Seen {
        final List<String> starters = Lists.newArrayList();
        final List<List<String>> handsOfA = Lists.newArrayList();
    }

    private Deck pack(final int cards) {
        initAndCreateGame(); // loads the card database
        final Deck deck = new Deck("pack");
        for (int i = 0; i < cards; i++) {
            deck.getMain().add(FModel.getMagicDb().getCommonCards().getCard(PACK[i]));
        }
        return deck;
    }

    private Match match(final GameType variant, final Deck deck) {
        final Set<GameType> variants = EnumSet.of(variant);
        final List<RegisteredPlayer> players = Lists.newArrayList();
        for (final String name : new String[] {"a", "b"}) {
            players.add(RegisteredPlayer.forVariants(2, variants, deck, null, false, null, null)
                    .setPlayer(new LobbyPlayerAi(name, null)));
        }
        final GameRules rules = new GameRules(GameType.Constructed);
        rules.setAppliedVariants(variants);
        return new Match(rules, players, "Test");
    }

    /**
     * Plays one game of the match, checking the setup at the start of the first turn, then ending it as given.
     * A pile size of 0 takes any.
     */
    private void play(final Match match, final Result result, final Seen seen, final int pileSize) {
        final Game game = match.createGame();
        match.startGame(game, () -> {
            for (final Player p : game.getPlayers()) {
                AssertJUnit.assertEquals(PaiGowMagic.STARTING_LIFE, p.getLife());
                AssertJUnit.assertTrue(p.getCardsIn(ZoneType.Library).isEmpty());
                if (pileSize > 0) {
                    AssertJUnit.assertEquals(pileSize, p.getCardsIn(ZoneType.Hand).size());
                }
                AssertJUnit.assertTrue(p.getManaPool().isUnlimited());
                AssertJUnit.assertEquals(1, p.getCardsIn(ZoneType.Command).size());
            }
            seen.starters.add(game.getPhaseHandler().getPlayerTurn().getName());
            final List<String> hand = Lists.newArrayList();
            for (final Card c : game.getPlayers().get(0).getCardsIn(ZoneType.Hand)) {
                hand.add(c.getName());
            }
            seen.handsOfA.add(hand);

            if (result == Result.DRAW) {
                for (final Player p : game.getPlayers()) {
                    p.intentionalDraw();
                }
                game.setGameOver(GameEndReason.Draw);
            } else {
                game.getPlayers().get(result == Result.A_WINS ? 1 : 0).concede();
                game.getAction().checkGameOverCondition();
            }
        });
        AssertJUnit.assertTrue(game.isGameOver());
    }

    private RegisteredPlayer seat(final Match match, final int i) {
        return match.getPlayers().get(i);
    }

    @Test
    public void testPileSizes() {
        AssertJUnit.assertEquals(List.of(3, 3, 3, 3, 3), PaiGowMagic.pileSizes(15, 5));
        // a 14-card Play Booster: one pile is short (user ruling 2026-10-30)
        AssertJUnit.assertEquals(List.of(3, 3, 3, 3, 2), PaiGowMagic.pileSizes(14, 5));
        AssertJUnit.assertEquals(List.of(3, 3, 3, 3), PaiGowMagic.pileSizes(15, 4));
    }

    @Test
    public void testFiveGamesFromThePilesLoserPlaysFirst() {
        final Match match = match(GameType.PaiGow, pack(15));
        final Seen seen = new Seen();
        final Result[] results = {Result.A_WINS, Result.B_WINS, Result.DRAW, Result.A_WINS, Result.A_WINS};
        for (int i = 0; i < results.length; i++) {
            AssertJUnit.assertFalse("over before game " + (i + 1), match.isMatchOver());
            play(match, results[i], seen, 3);
        }
        AssertJUnit.assertTrue(match.isMatchOver());
        AssertJUnit.assertSame(seat(match, 0), match.getWinner());

        // the loser plays first; after the draw, the same player as in the drawn game
        AssertJUnit.assertEquals("b", seen.starters.get(1));
        AssertJUnit.assertEquals("a", seen.starters.get(2));
        AssertJUnit.assertEquals("a", seen.starters.get(3));
        AssertJUnit.assertEquals("b", seen.starters.get(4));

        // each pile is one game's whole hand: every card of the pack once over the five games
        final List<String> all = Lists.newArrayList();
        for (final List<String> hand : seen.handsOfA) {
            all.addAll(hand);
        }
        all.sort(null);
        final List<String> expected = Lists.newArrayList(PACK);
        expected.sort(null);
        AssertJUnit.assertEquals(expected, all);
    }

    @Test
    public void testThreeWinsEndTheMatchEarly() {
        final Match match = match(GameType.PaiGow, pack(15));
        final Seen seen = new Seen();
        for (int i = 0; i < 3; i++) {
            play(match, Result.B_WINS, seen, 3);
        }
        AssertJUnit.assertTrue(match.isMatchOver());
        AssertJUnit.assertSame(seat(match, 1), match.getWinner());
        AssertJUnit.assertTrue(match.isWonBy(seat(match, 1).getPlayer()));
        AssertJUnit.assertFalse(match.isWonBy(seat(match, 0).getPlayer()));
    }

    @Test
    public void testBoosterBlitzRunsOutOfPilesInADraw() {
        final Match match = match(GameType.BoosterBlitz, pack(15));
        final Seen seen = new Seen();
        for (final Result r : new Result[] {Result.A_WINS, Result.B_WINS, Result.DRAW, Result.DRAW}) {
            AssertJUnit.assertFalse(match.isMatchOver());
            play(match, r, seen, 3);
        }
        // four piles, so four games; 1-1 is no one's match
        AssertJUnit.assertTrue(match.isMatchOver());
        AssertJUnit.assertNull(match.getWinner());
        AssertJUnit.assertFalse(match.isWonBy(seat(match, 0).getPlayer()));
        AssertJUnit.assertFalse(match.isWonBy(seat(match, 1).getPlayer()));
        // 12 of the 15 cards fit in the piles
        int cards = 0;
        for (final List<String> hand : seen.handsOfA) {
            cards += hand.size();
        }
        AssertJUnit.assertEquals(12, cards);
    }

    @Test
    public void testMoreWinsTakeTheMatchAfterDraws() {
        final Match match = match(GameType.PaiGow, pack(14));
        final Seen seen = new Seen();
        final Result[] results = {Result.DRAW, Result.A_WINS, Result.DRAW, Result.B_WINS, Result.A_WINS};
        for (final Result r : results) {
            play(match, r, seen, 0);
        }
        // 2-1 with two draws
        AssertJUnit.assertTrue(match.isMatchOver());
        AssertJUnit.assertSame(seat(match, 0), match.getWinner());
        AssertJUnit.assertTrue(match.isWonBy(seat(match, 0).getPlayer()));

        // a 14-card pack: four piles of three and one of two, in some order
        final List<Integer> sizes = Lists.newArrayList();
        for (final List<String> hand : seen.handsOfA) {
            sizes.add(hand.size());
        }
        sizes.sort(null);
        AssertJUnit.assertEquals(List.of(2, 3, 3, 3, 3), sizes);
    }

    @Test
    public void testAiSpreadsItsCreatures() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final List<PaperCard> pack = pack(15).getMain().toFlatList();
        final List<List<PaperCard>> piles = PaiGowAi.makePiles(ai, pack, PaiGowMagic.pileSizes(15, 5));
        AssertJUnit.assertEquals(5, piles.size());
        for (final List<PaperCard> pile : piles) {
            AssertJUnit.assertEquals(3, pile.size());
            // five creatures in the pack, so one each
            AssertJUnit.assertEquals(1, pile.stream().filter(pc -> pc.getRules().getType().isCreature()).count());
        }
    }

    @Test
    public void testUnlimitedManaPaysAnyCostAndStays() {
        final Game game = initAndCreateGame();
        game.getRules().addAppliedVariant(GameType.PaiGow);
        final Player p = game.getPlayers().get(1);
        PaiGowMagic.createRulesEffect(p);
        final ManaPool pool = p.getManaPool();

        final Card dragon = addCardToZone("Shivan Dragon", p, ZoneType.Hand);
        final SpellAbility cast = dragon.getFirstSpellAbility();
        cast.setActivatingPlayer(p);
        AssertJUnit.assertTrue(ComputerUtilMana.canPayManaCost(cast, p, 0, false));
        AssertJUnit.assertTrue(ComputerUtilMana.canPayManaCost(cast, p, 40, false));

        final Card fireball = addCardToZone("Fireball", p, ZoneType.Hand);
        final SpellAbility x = fireball.getFirstSpellAbility();
        x.setActivatingPlayer(p);
        // unlimited is any number, not infinity
        AssertJUnit.assertFalse(pool.canPayInfiniteX(x));

        // it doesn't empty, and there's nothing to warn about losing
        AssertJUnit.assertFalse(pool.willManaBeLostAtEndOfPhase());
        AssertJUnit.assertTrue(pool.clearPool(true).isEmpty());
        AssertJUnit.assertTrue(pool.isUnlimited());
        AssertJUnit.assertTrue(ComputerUtilMana.canPayManaCost(cast, p, 0, false));
    }

    @Test
    public void testAiTakesADrawOnlyOnceTheGameHasStalled() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        // outside Pai Gow it never does
        AssertJUnit.assertFalse(PaiGowAi.acceptsDraw(ai));

        game.getRules().addAppliedVariant(GameType.PaiGow);
        AssertJUnit.assertTrue("nothing in hand, nothing to hurt anyone with", PaiGowAi.acceptsDraw(ai));

        final Card spell = addCardToZone("Shock", ai, ZoneType.Hand);
        AssertJUnit.assertFalse("a card still to play", PaiGowAi.acceptsDraw(ai));
        ai.getGame().getAction().moveToGraveyard(spell, null);

        final Card bears = addCard("Grizzly Bears", ai);
        bears.setSickness(true);
        AssertJUnit.assertFalse("a creature that hasn't had its chance to attack", PaiGowAi.acceptsDraw(ai));
        bears.setSickness(false);
        AssertJUnit.assertTrue("it could have attacked, didn't, and no one lost life", PaiGowAi.acceptsDraw(ai));
        game.getPlayers().get(0).setLifeLostLastTurn(2);
        AssertJUnit.assertFalse("the game is still moving", PaiGowAi.acceptsDraw(ai));
    }

    @Test
    public void testAiCastsItsHandWithoutLands() {
        final Game game = initAndCreateGame();
        game.getRules().addAppliedVariant(GameType.PaiGow);
        final Player ai = game.getPlayers().get(1);
        PaiGowMagic.createRulesEffect(ai);
        addCardToZone("Shivan Dragon", ai, ZoneType.Hand);
        addCardToZone("Serra Angel", ai, ZoneType.Hand);
        playUntilPhase(game, forge.game.phase.PhaseType.END_OF_TURN);
        AssertJUnit.assertEquals(2, ai.getCreaturesInPlay().size());
        AssertJUnit.assertTrue(ai.getManaPool().isUnlimited());
    }

    @Test
    public void testNoLossForDrawingFromAnEmptyLibrary() {
        final Game game = initAndCreateGame();
        game.getRules().addAppliedVariant(GameType.PaiGow);
        final Player p = game.getPlayers().get(1);
        PaiGowMagic.createRulesEffect(p);
        AssertJUnit.assertTrue(p.getCardsIn(ZoneType.Library).isEmpty());
        p.drawCard();
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(p.hasLost());
        AssertJUnit.assertFalse(game.isGameOver());
    }
}
