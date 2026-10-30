package forge.ai;

import java.io.File;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.deck.Deck;
import forge.deck.io.DeckSerializer;
import forge.game.Game;
import forge.game.GameLogEntry;
import forge.game.GameRules;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.Match;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.mulligan.MulliganService;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.DandanDecks;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;

/**
 * Dandan (Forgetful Fish), against the rules published with the Secret Lair Dandan Deck (magic.wizards.com,
 * 2026-03-15): a shared library and graveyard that "your" means, the caster owns a spell or permanent, simultaneous
 * draws are dealt from the active player, and a hand with fewer than two lands or spells gets a free mulligan.
 */
public class DandanTest extends AITest {

    /** p1 and p2, p1's first main phase, nothing in the shared library yet. */
    private Game createDandanGame() {
        initAndCreateGame();
        final List<RegisteredPlayer> players = Lists.newArrayList();
        final Deck deck = new Deck();
        players.add(new RegisteredPlayer(deck).setPlayer(new LobbyPlayerAi("p1", null)));
        players.add(new RegisteredPlayer(deck).setPlayer(new LobbyPlayerAi("p2", null)));
        final GameRules rules = new GameRules(GameType.Dandan);
        rules.setAppliedVariants(EnumSet.of(GameType.Dandan));
        final Game game = new Game(players, rules, new Match(rules, players, "Test"));
        game.setAge(GameStage.Play);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, game.getPlayers().get(0));
        game.getPhaseHandler().onStackResolved();
        return game;
    }

    /** Puts the named cards on top of the shared library, the first one on top. */
    private List<Card> stackLibrary(final Player p, final String... names) {
        final List<Card> cards = Lists.newArrayList();
        for (int i = names.length - 1; i >= 0; i--) {
            final Card c = createCard(names[i], p);
            c.setGameTimestamp(p.getGame().getNextTimestamp());
            p.getZone(ZoneType.Library).add(c, 0);
            cards.add(0, c);
        }
        return cards;
    }

    private void resolve(final Card host, final Player activator) {
        final SpellAbility sa = host.getFirstSpellAbility();
        sa.setActivatingPlayer(activator);
        AbilityUtils.resolve(sa);
    }

    @Test
    public void playersShareOneLibraryAndGraveyard() {
        final Game game = createDandanGame();
        final Player p1 = game.getPlayers().get(0), p2 = game.getPlayers().get(1);
        fillLibrary(p1, 5);
        fillLibrary(p2, 5);
        addCardToZone("Memory Lapse", p1, ZoneType.Graveyard);
        addCardToZone("Brainstorm", p2, ZoneType.Graveyard);

        AssertJUnit.assertSame(p1.getZone(ZoneType.Library), p2.getZone(ZoneType.Library));
        AssertJUnit.assertSame(p1.getZone(ZoneType.Graveyard), p2.getZone(ZoneType.Graveyard));
        AssertJUnit.assertNotSame(p1.getZone(ZoneType.Hand), p2.getZone(ZoneType.Hand));
        AssertJUnit.assertEquals(10, p2.getCardsIn(ZoneType.Library).size());
        // each card counted once, however the zones are gone over
        AssertJUnit.assertEquals(10, game.getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertEquals(2, game.getCardsIn(ZoneType.Graveyard).size());
        AssertJUnit.assertEquals(2, game.getPlayers().getCardsIn(ZoneType.Graveyard).size());
        AssertJUnit.assertEquals(2, game.getCardsIncludePhasingIn(ZoneType.Graveyard).size());
        AssertJUnit.assertEquals(2, game.getCardsIn(EnumSet.of(ZoneType.Graveyard, ZoneType.Hand)).size());
        AssertJUnit.assertTrue(p1.getZone(ZoneType.Graveyard).is(ZoneType.Graveyard, p2));
    }

    @Test
    public void yourGraveyardIsTheSharedOne() {
        final Game game = createDandanGame();
        final Player p1 = game.getPlayers().get(0), p2 = game.getPlayers().get(1);
        final Card lapse = addCardToZone("Memory Lapse", p1, ZoneType.Graveyard);
        final Card sanctuary = addCard("Mystic Sanctuary", p2);

        // Mystic Sanctuary's "target instant or sorcery card from your graveyard" is Instant.YouOwn
        AssertJUnit.assertTrue(lapse.isValid("Instant.YouOwn", p2, sanctuary, null));
        AssertJUnit.assertTrue(lapse.isValid("Instant.YouCtrl", p2, sanctuary, null));
        AssertJUnit.assertTrue(lapse.isValid("Instant.OppOwn", p2, sanctuary, null));
        AssertJUnit.assertFalse(lapse.isValid("Instant.YouDontOwn", p2, sanctuary, null));
        // a permanent is still only its controller's
        AssertJUnit.assertFalse(sanctuary.isValid("Land.YouCtrl", p1, sanctuary, null));
    }

    @Test
    public void accumulatedKnowledgeCountsTheSharedGraveyardOnce() {
        final Game game = createDandanGame();
        final Player p1 = game.getPlayers().get(0), p2 = game.getPlayers().get(1);
        fillLibrary(p1, 10);
        addCardToZone("Accumulated Knowledge", p1, ZoneType.Graveyard);
        addCardToZone("Accumulated Knowledge", p2, ZoneType.Graveyard);
        final Card ak = addCardToZone("Accumulated Knowledge", p2, ZoneType.Hand);

        resolve(ak, p2);

        // a card, then one for each of the two in the graveyard
        AssertJUnit.assertEquals(3, p2.getNumDrawnThisTurn());
    }

    @Test
    public void aCardInHandIsItsHoldersAndASpellItsCasters() {
        final Game game = createDandanGame();
        final Player p1 = game.getPlayers().get(0), p2 = game.getPlayers().get(1);
        final Card top = stackLibrary(p1, "Dandân").get(0);
        AssertJUnit.assertEquals(p1, top.getOwner());

        p2.drawCard();
        final Card drawn = p2.getCardsIn(ZoneType.Hand).getFirst();
        AssertJUnit.assertEquals("Dandân", drawn.getName());
        AssertJUnit.assertEquals(p2, drawn.getOwner());

        // "return target creature to its owner's hand": the player who cast it, not whoever controls it now
        final Card fish = game.getAction().moveToPlay(drawn, p2, null, null);
        fish.setController(p1, game.getNextTimestamp());
        AssertJUnit.assertEquals(p2, fish.getOwner());
        final Card bounced = game.getAction().moveToHand(fish, null);
        AssertJUnit.assertTrue(p2.getCardsIn(ZoneType.Hand).contains(bounced));
    }

    @Test
    public void takingACardFromTheSharedGraveyardMakesItYours() {
        final Game game = createDandanGame();
        final Player p1 = game.getPlayers().get(0), p2 = game.getPlayers().get(1);
        final Card fish = addCardToZone("Dandân", p1, ZoneType.Graveyard);
        final Card fengraf = addCard("Haunted Fengraf", p2);
        SpellAbility ret = null;
        for (final SpellAbility sa : fengraf.getSpellAbilities()) {
            if (sa.getApi() == ApiType.ChangeZone) {
                ret = sa;
            }
        }
        AssertJUnit.assertNotNull(ret);
        ret.setActivatingPlayer(p2);
        AbilityUtils.resolve(ret);

        // "return a creature card at random from your graveyard to your hand" - p2's hand, though p1 last owned it
        AssertJUnit.assertEquals(1, p2.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(0, p1.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(p2, p2.getCardsIn(ZoneType.Hand).getFirst().getOwner());
        AssertJUnit.assertFalse(game.getCardsIn(ZoneType.Graveyard).contains(fish));
    }

    @Test
    public void flashbackWorksFromTheSharedGraveyardForEitherPlayer() {
        final Game game = createDandanGame();
        final Player p1 = game.getPlayers().get(0), p2 = game.getPlayers().get(1);
        final Card retrieval = addCardToZone("Mystic Retrieval", p1, ZoneType.Graveyard);

        AssertJUnit.assertTrue(p1.getCardsIn(ZoneType.Flashback).contains(retrieval));
        AssertJUnit.assertTrue(p2.getCardsIn(ZoneType.Flashback).contains(retrieval));
    }

    @Test
    public void simultaneousDrawsAreDealtFromTheActivePlayer() {
        final Game game = createDandanGame();
        final Player p1 = game.getPlayers().get(0), p2 = game.getPlayers().get(1);
        // p2's turn: p2 is dealt first, though the spell is p1's
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p2);
        final List<Card> top = stackLibrary(p1, "Island", "Forest", "Mountain", "Plains", "Swamp");
        final Card skeins = addCardToZone("Vision Skeins", p1, ZoneType.Exile);

        resolve(skeins, p1);

        AssertJUnit.assertEquals(List.of("Island", "Mountain"), names(p2.getCardsIn(ZoneType.Hand)));
        AssertJUnit.assertEquals(List.of("Forest", "Plains"), names(p1.getCardsIn(ZoneType.Hand)));
        AssertJUnit.assertEquals(top.get(4), p1.getCardsIn(ZoneType.Library).getFirst());
    }

    @Test
    public void openingHandsAreDealt() {
        final Game game = createDandanGame();
        final Player p1 = game.getPlayers().get(0), p2 = game.getPlayers().get(1);
        stackLibrary(p1, "Island", "Forest", "Mountain", "Plains");
        final Map<Player, Integer> counts = Maps.newLinkedHashMap();
        counts.put(p2, 2);
        counts.put(p1, 2);

        game.getAction().dealCards(counts, null, null);

        AssertJUnit.assertEquals(List.of("Island", "Mountain"), names(p2.getCardsIn(ZoneType.Hand)));
        AssertJUnit.assertEquals(List.of("Forest", "Plains"), names(p1.getCardsIn(ZoneType.Hand)));
    }

    @Test
    public void aHandOfAllLandsIsAFreeMulligan() {
        final Game game = createDandanGame();
        final Player p1 = game.getPlayers().get(0), p2 = game.getPlayers().get(1);
        game.setAge(GameStage.Mulligan);
        for (int i = 0; i < 40; i++) {
            addCardToZone("Island", p1, ZoneType.Library);
            addCardToZone("Brainstorm", p1, ZoneType.Library);
        }
        // both hands all Islands
        stackLibrary(p1, "Island", "Island", "Island", "Island", "Island", "Island", "Island",
                "Island", "Island", "Island", "Island", "Island", "Island", "Island");
        final Map<Player, Integer> counts = Maps.newLinkedHashMap();
        counts.put(p1, 7);
        counts.put(p2, 7);
        game.getAction().dealCards(counts, null, null);

        new MulliganService(p1).perform();

        int free = 0;
        for (final GameLogEntry e : game.getGameLog().getLogEntries(null)) {
            if (e.message().contains("takes a free mulligan")) {
                free++;
            }
        }
        AssertJUnit.assertTrue(free >= 2);
        // the shared library holds everything the hands don't
        AssertJUnit.assertEquals(94 - p1.getCardsIn(ZoneType.Hand).size() - p2.getCardsIn(ZoneType.Hand).size(),
                p1.getCardsIn(ZoneType.Library).size());
        // a free mulligan costs nothing, so only a later regular one could shrink a hand
        AssertJUnit.assertTrue(p1.getCardsIn(ZoneType.Hand).size() >= 6);
        AssertJUnit.assertTrue(p2.getCardsIn(ZoneType.Hand).size() >= 6);
    }

    @Test
    public void aiHacksAnOpposingDandanToDeath() {
        final Game game = createDandanGame();
        final Player ai = game.getPlayers().get(0), opp = game.getPlayers().get(1);
        fillLibrary(ai, 10);
        addCard("Island", ai);
        addCard("Island", opp);
        final Card fish = addCard("Dandân", opp);
        final Card hack = addCardToZone("Magical Hack", ai, ZoneType.Hand);
        final SpellAbility sa = hack.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa));
        AssertJUnit.assertTrue(sa.getTargets().contains(fish));
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, sa, null));
        playUntilStackClear(game);
        game.getAction().checkStateEffects(true);
        playUntilStackClear(game);

        // "Island" became a type its controller has none of, so "when you control no [them], sacrifice" it
        AssertJUnit.assertFalse(fish.isInPlay());
        AssertJUnit.assertEquals(1, CardLists.filter(game.getCardsIn(ZoneType.Graveyard), c -> c.getName().equals("Dandân")).size());
    }

    @Test
    public void aiMindBendsAnOpposingDandan() {
        final Game game = createDandanGame();
        final Player ai = game.getPlayers().get(0), opp = game.getPlayers().get(1);
        fillLibrary(ai, 10);
        addCard("Island", ai);
        addCard("Island", opp);
        final Card fish = addCard("Dandân", opp);
        final Card bend = addCardToZone("Mind Bend", ai, ZoneType.Hand);
        final SpellAbility sa = bend.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa));
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, sa, null));
        playUntilStackClear(game);
        game.getAction().checkStateEffects(true);
        playUntilStackClear(game);

        AssertJUnit.assertFalse(fish.isInPlay());
    }

    @Test
    public void aiUndoesTheDayInItsSecondMainPhaseWhenBehind() {
        final Game game = createDandanGame();
        final Player ai = game.getPlayers().get(0), opp = game.getPlayers().get(1);
        fillLibrary(ai, 30);
        addCards("Island", 3, ai);
        for (int i = 0; i < 6; i++) {
            addCardToZone("Memory Lapse", opp, ZoneType.Hand);
        }
        final Card undoing = addCardToZone("Day's Undoing", ai, ZoneType.Hand);
        final SpellAbility sa = undoing.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);

        // it ends the turn, so not before combat
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa));
        moveToMain2(game, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa));
    }

    @Test
    public void everyDeckListLoadsInFull() {
        initAndCreateGame();
        final File[] files = new File(ForgeConstants.DANDAN_DIR).listFiles((dir, name) -> name.endsWith(".dck"));
        AssertJUnit.assertNotNull(files);
        AssertJUnit.assertEquals(4, files.length);
        for (final File f : files) {
            final Deck deck = DeckSerializer.fromFile(f);
            final int expected = f.getName().contains("1997") ? 60 : 80;
            AssertJUnit.assertEquals(f.getName(), expected, deck.getMain().countAll());
            if (f.getName().contains("secret_lair")) {
                for (final PaperCard pc : deck.getMain().toFlatList()) {
                    AssertJUnit.assertEquals(pc.getName(), "SLD", pc.getEdition());
                }
            }
        }
        AssertJUnit.assertEquals(4, DandanDecks.names().size());
        AssertJUnit.assertEquals(80, DandanDecks.selected().getMain().countAll());
    }

    private static List<String> names(final Iterable<Card> cards) {
        final List<String> names = Lists.newArrayList();
        for (final Card c : cards) {
            names.add(c.getName());
        }
        return names;
    }
}
