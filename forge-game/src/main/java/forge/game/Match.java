package forge.game;

import com.google.common.collect.*;
import com.google.common.eventbus.EventBus;
import forge.LobbyPlayer;
import forge.StaticData;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckSection;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.event.Event;
import forge.game.event.GameEventAddLog;
import forge.game.event.GameEventAnteCardsSelected;
import forge.game.event.GameEventGameFinished;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.player.RegisteredPlayer;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Lang;
import forge.util.Localizer;
import forge.util.MyRandom;
import forge.util.collect.FCollectionView;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.Map.Entry;

public class Match {
    private List<PaperCard> removedCards = Lists.newArrayList();
    private final List<RegisteredPlayer> players;
    private final GameRules rules;
    private final String title;

    private final EventBus events = new EventBus("match events");
    private final Map<Integer, GameOutcome> gameOutcomes = Maps.newHashMap();

    private GameOutcome lastOutcome = null;

    // Cards like Double Dip need to carry a delayed effect from one game in the match into the
    // next - there's no Card/Player object that survives across Game instances to host a normal
    // trigger, so this is tracked as plain Match-level state instead and applied by PhaseHandler
    // at the new game's first upkeep.
    private final Map<RegisteredPlayer, Integer> pendingFirstUpkeepLifeGain = Maps.newHashMap();
    // Same mechanism, for Double Take's "you draw two cards" instead of Double Dip's life gain.
    private final Map<RegisteredPlayer, Integer> pendingFirstUpkeepDraw = Maps.newHashMap();
    // Same mechanism, for Double Cross's "your opponent discards a card of your choice". Keyed by
    // the caster (the one who gets to choose), not the discarder - see DiscardEffect's
    // NextGameFirstUpkeep handling.
    private final Map<RegisteredPlayer, Integer> pendingFirstUpkeepDiscard = Maps.newHashMap();
    // Same mechanism, for Double Deal's "deals 3 damage to the player". Keyed by the caster, not
    // the chosen player being damaged - see DamageDealEffect's NextGameFirstUpkeep handling.
    private final Map<RegisteredPlayer, Integer> pendingFirstUpkeepDamage = Maps.newHashMap();
    // Same mechanism, for Double Play's "search your library for a basic land card, put it onto the
    // battlefield" - see ChangeZoneEffect's NextGameFirstUpkeep handling.
    private final Map<RegisteredPlayer, Integer> pendingFirstUpkeepBasicLand = Maps.newHashMap();

    public Match(final GameRules rules0, final List<RegisteredPlayer> players0, final String title) {
        players = Collections.unmodifiableList(Lists.newArrayList(players0));
        rules = rules0;
        this.title = title;
    }

    public GameRules getRules() {
        return rules;
    }
    String getTitle() {
        final Multiset<RegisteredPlayer> wins = getGamesWon();
        final StringBuilder titleAppend = new StringBuilder(title);
        titleAppend.append(" (");
        for (final RegisteredPlayer rp : players) {
            titleAppend.append(wins.count(rp)).append('-');
        }
        titleAppend.deleteCharAt(titleAppend.length() - 1);
        titleAppend.append(')');
        return titleAppend.toString();
    }

    public void addGamePlayed(Game finished) {
        if (!finished.isGameOver()) {
            throw new IllegalStateException("Game is not over yet.");
        }
        lastOutcome = finished.getOutcome();
        gameOutcomes.put(finished.getId(), finished.getOutcome());
    }

    public Game createGame() {
        return new Game(players, rules, this);
    }

    public void startGame(final Game game) {
        startGame(game, null);
    }

    public void startGame(final Game game, Runnable startGameHook) {
        prepareAllZones(game);
        createPendingFirstUpkeepEffects(game);
        if (rules.useAnte()) {  // Deciding which cards go to ante
            Multimap<Player, Card> list = game.chooseCardsForAnte(rules.getMatchAnteRarity(), rules.getAnteIncludeBasicLands());
            for (Entry<Player, Card> kv : list.entries()) {
                Player p = kv.getKey();
                game.getAction().moveTo(ZoneType.Ante, kv.getValue(), null, AbilityKey.newMap());
                game.fireEvent(new GameEventAddLog(GameLogEntryType.ANTE, p + " anted " + kv.getValue()));
            }
            game.fireEvent(GameEventAnteCardsSelected.fromCards(list));
        }

        game.getAction().startGame(this.lastOutcome, startGameHook);

        // Typically ante, but also tearing up a blacker lotus
        executeOwnershipChanges(game);

        game.clearCaches();

        // will pull UI dialog, when the UI is listening
        game.fireEvent(new GameEventGameFinished());

        //run GC after game is finished
        System.gc();
    }

    public GameOutcome getOutcomeById(int id) {
        return gameOutcomes.get(id);
    }

    public void clearGamesPlayed() {
        gameOutcomes.clear();
        for (RegisteredPlayer p : players) {
            p.restoreDeck();
        }
    }

    public Collection<GameOutcome> getOutcomes() {
        return gameOutcomes.values();
    }

    // gameOutcomes is a plain HashMap keyed by game ID, so .values() has no reliable order. Game
    // IDs (Game.nextId()) are assigned in creation order, so sorting by key gives chronological
    // order within this match - needed by Gus's "since you last won a game against them" streak.
    public List<GameOutcome> getOutcomesInOrder() {
        final List<Integer> ids = Lists.newArrayList(gameOutcomes.keySet());
        Collections.sort(ids);
        final List<GameOutcome> ordered = Lists.newArrayList();
        for (final Integer id : ids) {
            ordered.add(gameOutcomes.get(id));
        }
        return ordered;
    }

    public GameOutcome getLastOutcome() {
        return lastOutcome;
    }

    public boolean isMatchOver() {
        int[] victories = new int[players.size()];
        for (GameOutcome go : getOutcomes()) {
            LobbyPlayer winner = go.getWinningLobbyPlayer();
            int i = 0;
            for (RegisteredPlayer p : players) {
                if (p.getPlayer().equals(winner)) {
                    victories[i]++;
                    if (victories[i] >= rules.getGamesToWinMatch()) {
                        return true;
                    }
                }
                i++;
            }
        }

        // Games are first to X wins, not first to X wins or Y total games played
        return false;
    }

    public int getGamesWonBy(LobbyPlayer questPlayer) {
        int sum = 0;
        for (GameOutcome go : getOutcomes()) {
            if (questPlayer.equals(go.getWinningLobbyPlayer())) {
                sum++;
            }
        }
        return sum;
    }
    public Multiset<RegisteredPlayer> getGamesWon() {
        final Multiset<RegisteredPlayer> won = HashMultiset.create(players.size());
        for (final GameOutcome go : getOutcomes()) {
            if (go.getWinningPlayer() == null) {
                // Game hasn't finished yet. Exit early.
                return won;
            }
            won.add(go.getWinningPlayer());
        }
        return won;
    }

    public boolean isWonBy(LobbyPlayer questPlayer) {
        return getGamesWonBy(questPlayer) >= rules.getGamesToWinMatch();
    }

    public RegisteredPlayer getWinner() {
        if (this.isMatchOver()) {
            return lastOutcome.getWinningPlayer();
        }
        return null;
    }

    public List<RegisteredPlayer> getPlayers() {
        return players;
    }

    private static Set<PaperCard> getRemovedAnteCards(Deck toUse) {
        final String keywordToRemove = "Remove CARDNAME from your deck before playing if you're not playing for ante.";
        Set<PaperCard> myRemovedAnteCards = new HashSet<>();
        for (Entry<DeckSection, CardPool> ds : toUse) {
            for (Entry<PaperCard, Integer> cp : ds.getValue()) {
                if (Iterables.contains(cp.getKey().getRules().getMainPart().getKeywords(), keywordToRemove)) {
                    myRemovedAnteCards.add(cp.getKey());
                }
            }
        }
        return myRemovedAnteCards;
    }

    public List<PaperCard> getRemovedCards() { return removedCards; }

    public void removeCard(PaperCard c) {
        removedCards.add(c);
    }

    public void queueFirstUpkeepLifeGain(RegisteredPlayer player, int life) {
        pendingFirstUpkeepLifeGain.merge(player, life, Integer::sum);
    }

    public void queueFirstUpkeepDraw(RegisteredPlayer player, int cards) {
        pendingFirstUpkeepDraw.merge(player, cards, Integer::sum);
    }

    public void queueFirstUpkeepDiscard(RegisteredPlayer player, int cards) {
        pendingFirstUpkeepDiscard.merge(player, cards, Integer::sum);
    }

    public void queueFirstUpkeepDamage(RegisteredPlayer player, int damage) {
        pendingFirstUpkeepDamage.merge(player, damage, Integer::sum);
    }

    public void queueFirstUpkeepBasicLandSearch(RegisteredPlayer player, int count) {
        pendingFirstUpkeepBasicLand.merge(player, count, Integer::sum);
    }

    // Called once per new game in this match, right after zones are prepared. Rather than
    // applying the gain directly (which would silently happen with no trigger/stack presence),
    // build a real Command-zone Effect carrying a genuine "first upkeep of the game" trigger -
    // same mechanism DB$ Effect uses (see EffectEffect.java) - so it announces and resolves on
    // the stack exactly like any other "at the beginning of the first upkeep" ability.
    private void createPendingFirstUpkeepEffects(Game game) {
        if (!pendingFirstUpkeepLifeGain.isEmpty()) {
            for (Player p : game.getPlayers()) {
                Integer life = pendingFirstUpkeepLifeGain.remove(p.getRegisteredPlayer());
                if (life == null || life <= 0) {
                    continue;
                }
                Card hostCard = Card.fromPaperCard(StaticData.instance().getCommonCards().getCard("Double Dip"), p);
                Card eff = SpellAbilityEffect.createEffect(null, hostCard, p, "Double Dip", hostCard.getImageKey(), game.getNextTimestamp());
                eff.setSVar("TrigGainLife", "DB$ GainLife | Defined$ You | LifeAmount$ " + life + " | SubAbility$ DBExile");
                eff.setSVar("DBExile", "DB$ ChangeZone | Defined$ Self | Origin$ Command | Destination$ Exile");
                Trigger trigger = TriggerHandler.parseTrigger("Mode$ Phase | Phase$ Upkeep | FirstUpkeepThisGame$ True | Execute$ TrigGainLife | TriggerDescription$ At the beginning of the first upkeep in your next game with that player, you gain " + life + " life.", eff, true);
                trigger.setActiveZone(EnumSet.of(ZoneType.Command));
                eff.addTrigger(trigger);
                game.getAction().moveTo(ZoneType.Command, eff, null, AbilityKey.newMap());
            }
        }
        if (!pendingFirstUpkeepDraw.isEmpty()) {
            for (Player p : game.getPlayers()) {
                Integer cards = pendingFirstUpkeepDraw.remove(p.getRegisteredPlayer());
                if (cards == null || cards <= 0) {
                    continue;
                }
                Card hostCard = Card.fromPaperCard(StaticData.instance().getCommonCards().getCard("Double Take"), p);
                Card eff = SpellAbilityEffect.createEffect(null, hostCard, p, "Double Take", hostCard.getImageKey(), game.getNextTimestamp());
                eff.setSVar("TrigDraw", "DB$ Draw | Defined$ You | NumCards$ " + cards + " | SubAbility$ DBExile");
                eff.setSVar("DBExile", "DB$ ChangeZone | Defined$ Self | Origin$ Command | Destination$ Exile");
                Trigger trigger = TriggerHandler.parseTrigger("Mode$ Phase | Phase$ Upkeep | FirstUpkeepThisGame$ True | Execute$ TrigDraw | TriggerDescription$ At the beginning of the first upkeep in your next game with that player, you draw " + Lang.nounWithNumeral(cards, "card") + ".", eff, true);
                trigger.setActiveZone(EnumSet.of(ZoneType.Command));
                eff.addTrigger(trigger);
                game.getAction().moveTo(ZoneType.Command, eff, null, AbilityKey.newMap());
            }
        }
        if (!pendingFirstUpkeepDiscard.isEmpty()) {
            for (Player p : game.getPlayers()) {
                Integer cards = pendingFirstUpkeepDiscard.remove(p.getRegisteredPlayer());
                if (cards == null || cards <= 0) {
                    continue;
                }
                Card hostCard = Card.fromPaperCard(StaticData.instance().getCommonCards().getCard("Double Cross"), p);
                Card eff = SpellAbilityEffect.createEffect(null, hostCard, p, "Double Cross", hostCard.getImageKey(), game.getNextTimestamp());
                eff.setSVar("TrigDiscard", "DB$ Discard | Defined$ Opponent | Mode$ RevealYouChoose | NumCards$ " + cards + " | DiscardValid$ Card.nonBasic | SubAbility$ DBExile");
                eff.setSVar("DBExile", "DB$ ChangeZone | Defined$ Self | Origin$ Command | Destination$ Exile");
                Trigger trigger = TriggerHandler.parseTrigger("Mode$ Phase | Phase$ Upkeep | FirstUpkeepThisGame$ True | Execute$ TrigDiscard | TriggerDescription$ At the beginning of the first upkeep in your next game with that player, look at that player's hand and choose a card other than a basic land card from it. They discard that card.", eff, true);
                trigger.setActiveZone(EnumSet.of(ZoneType.Command));
                eff.addTrigger(trigger);
                game.getAction().moveTo(ZoneType.Command, eff, null, AbilityKey.newMap());
            }
        }
        if (!pendingFirstUpkeepDamage.isEmpty()) {
            for (Player p : game.getPlayers()) {
                Integer damage = pendingFirstUpkeepDamage.remove(p.getRegisteredPlayer());
                if (damage == null || damage <= 0) {
                    continue;
                }
                Card hostCard = Card.fromPaperCard(StaticData.instance().getCommonCards().getCard("Double Deal"), p);
                Card eff = SpellAbilityEffect.createEffect(null, hostCard, p, "Double Deal", hostCard.getImageKey(), game.getNextTimestamp());
                eff.setSVar("TrigDamage", "DB$ DealDamage | Defined$ Opponent | NumDmg$ " + damage + " | SubAbility$ DBExile");
                eff.setSVar("DBExile", "DB$ ChangeZone | Defined$ Self | Origin$ Command | Destination$ Exile");
                Trigger trigger = TriggerHandler.parseTrigger("Mode$ Phase | Phase$ Upkeep | FirstUpkeepThisGame$ True | Execute$ TrigDamage | TriggerDescription$ At the beginning of the first upkeep in your next game with that player, Double Deal deals " + damage + " damage to the player.", eff, true);
                trigger.setActiveZone(EnumSet.of(ZoneType.Command));
                eff.addTrigger(trigger);
                game.getAction().moveTo(ZoneType.Command, eff, null, AbilityKey.newMap());
            }
        }
        if (!pendingFirstUpkeepBasicLand.isEmpty()) {
            for (Player p : game.getPlayers()) {
                Integer count = pendingFirstUpkeepBasicLand.remove(p.getRegisteredPlayer());
                if (count == null || count <= 0) {
                    continue;
                }
                Card hostCard = Card.fromPaperCard(StaticData.instance().getCommonCards().getCard("Double Play"), p);
                Card eff = SpellAbilityEffect.createEffect(null, hostCard, p, "Double Play", hostCard.getImageKey(), game.getNextTimestamp());
                eff.setSVar("TrigSearch", "DB$ ChangeZone | Origin$ Library | Destination$ Battlefield | ChangeType$ Land.Basic | ChangeTypeDesc$ basic land | ChangeNum$ " + count + " | SubAbility$ DBExile");
                eff.setSVar("DBExile", "DB$ ChangeZone | Defined$ Self | Origin$ Command | Destination$ Exile");
                Trigger trigger = TriggerHandler.parseTrigger("Mode$ Phase | Phase$ Upkeep | FirstUpkeepThisGame$ True | Execute$ TrigSearch | TriggerDescription$ At the beginning of the first upkeep in your next game with that player, search your library for a basic land card, put it onto the battlefield, then shuffle.", eff, true);
                trigger.setActiveZone(EnumSet.of(ZoneType.Command));
                eff.addTrigger(trigger);
                game.getAction().moveTo(ZoneType.Command, eff, null, AbilityKey.newMap());
            }
        }
    }

    private static void preparePlayerZone(Player player, final ZoneType zoneType, CardPool section, boolean canRandomFoil) {
        PlayerZone library = player.getZone(zoneType);
        List<Card> newLibrary = new ArrayList<>();
        for (final Entry<PaperCard, Integer> stackOfCards : section) {
            final PaperCard cp = stackOfCards.getKey();
            for (int i = 0; i < stackOfCards.getValue(); i++) {
                final Card card = Card.fromPaperCard(cp, player);

                // Assign card-specific foiling or random foiling on approximately 1:20 cards if enabled
                if (cp.isFoil() || (canRandomFoil && MyRandom.percentTrue(5))) {
                    card.setRandomFoil();
                }
                card.setCollectible(true);
                if (zoneType == ZoneType.Library) {
                    // Sideboard is explicitly excluded from "starting deck" per rule 702.139f (Companion)
                    card.setStartingDeckCard(true);
                }

                newLibrary.add(card);
            }
        }
        library.setCards(newLibrary);
    }

    private void prepareAllZones(final Game game) {
        // need this code here, otherwise observables fail
        Trigger.resetIDs();
        game.getTriggerHandler().clearDelayedTrigger();

        // friendliness
        Map<Player, Map<DeckSection, List<? extends PaperCard>>> rAICards = new HashMap<>();
        Multimap<Player, PaperCard> removedAnteCards = ArrayListMultimap.create();
        Map<Player, List<PaperCard>> unsupported = new HashMap<>();

        final FCollectionView<Player> players = game.getPlayers();
        final List<RegisteredPlayer> playersConditions = game.getMatch().getPlayers();

        boolean isFirstGame = gameOutcomes.isEmpty();
        boolean canSideBoard = !isFirstGame && rules.getGameType().isSideboardingAllowed();
        // Only allow this if feature flag is on AND for certain match types
        boolean sideboardForAIs = rules.getSideboardForAI() &&
            rules.getGameType().getDeckFormat().equals(DeckFormat.Constructed);
        PlayerController sideboardProxy = null;
        if (canSideBoard && sideboardForAIs) {
            for (int i = 0; i < players.size(); i++) {
                final Player player = players.get(i);
                //final RegisteredPlayer psc = playersConditions.get(i);
                if (!player.getController().isAI()) {
                    sideboardProxy = player.getController();
                    break;
                }
            }
        }

        for (int i = 0; i < playersConditions.size(); i++) {
            final Player player = players.get(i);
            final RegisteredPlayer psc = playersConditions.get(i);
            PlayerController person = player.getController();

            if (canSideBoard) {
                if (sideboardProxy != null && person.isAI()) {
                    person = sideboardProxy;
                }

                Deck toChange = psc.getDeck();
                if (!getRemovedCards().isEmpty()) {
                    CardPool main = new CardPool();
                    main.addAll(toChange.get(DeckSection.Main));
                    CardPool sideboard = new CardPool();
                    sideboard.addAll(toChange.getOrCreate(DeckSection.Sideboard));
                    for (PaperCard c : removedCards) {
                        if (main.contains(c)) {
                            main.remove(c, 1);
                        } else if (sideboard.contains(c)) {
                            sideboard.remove(c, 1);
                        }
                    }
                    toChange.getMain().clear();
                    toChange.getMain().addAll(main);
                    toChange.get(DeckSection.Sideboard).clear();
                    toChange.get(DeckSection.Sideboard).addAll(sideboard);
                }
                List<PaperCard> newMain = person.sideboard(toChange, rules.getGameType(), player.getName());
                if (null != newMain) {
                    CardPool allCards = new CardPool();
                    allCards.addAll(toChange.get(DeckSection.Main));
                    allCards.addAll(toChange.getOrCreate(DeckSection.Sideboard));
                    for (PaperCard c : newMain) {
                        allCards.remove(c);
                    }
                    toChange.getMain().clear();
                    toChange.getMain().add(newMain);
                    toChange.get(DeckSection.Sideboard).clear();
                    toChange.get(DeckSection.Sideboard).addAll(allCards);
                }
            }

            Deck toCheck = psc.getDeck();
            if (toCheck == null) {
                try {
                    System.err.println(psc.getPlayer().getName() + " Deck is NULL...");
                    int val = rules.getGameType().getDeckFormat().getMainRange().getMinimum();
                    toCheck = new Deck("NULL");
                    if (val > 0)
                        toCheck.getMain().add("Wastes", val);
                } catch (Exception ignored) {}
            }
            Pair<Deck, List<PaperCard>> myDeck = toCheck.getValid();
            player.setDraftNotes(myDeck.getLeft().getDraftNotes());

            Set<PaperCard> myRemovedAnteCards = null;
            if (!rules.useAnte()) {
                myRemovedAnteCards = getRemovedAnteCards(myDeck.getLeft());
                for (PaperCard cp: myRemovedAnteCards) {
                    for (Entry<DeckSection, CardPool> ds : myDeck.getLeft()) {
                        ds.getValue().removeAll(cp);
                    }
                }
            }

            preparePlayerZone(player, ZoneType.Library, myDeck.getLeft().getMain(), psc.useRandomFoil());
            if (myDeck.getLeft().has(DeckSection.Sideboard)) {
                preparePlayerZone(player, ZoneType.Sideboard, myDeck.getLeft().get(DeckSection.Sideboard), psc.useRandomFoil());

                player.assignCompanion(game, person);
            }

            player.initVariantsZones(psc);

            player.shuffle(null);

            if (isFirstGame) {
                Map<DeckSection, List<? extends PaperCard>> cardsComplained = player.getController().complainCardsCantPlayWell(myDeck.getLeft());
                if (cardsComplained != null && !cardsComplained.isEmpty()) {
                    rAICards.put(player, cardsComplained);
                }
            } else {
                //reset cards to fix weird issues on netplay nextgame client
                for (Card c : player.getCardsIn(ZoneType.Library)) {
                    c.setTapped(false);
                    c.resetActivationsPerTurn();
                }
            }

            if (myRemovedAnteCards != null && !myRemovedAnteCards.isEmpty()) {
                removedAnteCards.putAll(player, myRemovedAnteCards);
            }
            unsupported.put(player, myDeck.getRight());
        }

        final Localizer localizer = Localizer.getInstance();
        if (!rAICards.isEmpty() && !rules.getGameType().isCardPoolLimited() && rules.warnAboutAICards()) {
            game.getAction().revealUnplayableByAI(localizer.getMessage("lblAICantPlayCards"), rAICards);
        }

        if (!removedAnteCards.isEmpty()) {
            game.getAction().revealAnte(localizer.getMessage("lblAnteCardsRemoved"), removedAnteCards);
        }

        if (!unsupported.isEmpty()) {
            game.getAction().revealUnsupported(unsupported);
        }
    }

    private void executeOwnershipChanges(Game lastGame) {
        GameOutcome outcome = lastGame.getOutcome();

        // remove all the lost cards from owners' decks
        List<PaperCard> losses = new ArrayList<>();
        int cntPlayers = players.size();
        int iWinner = -1;
        for (int i = 0; i < cntPlayers; i++) {
            Player gamePlayer = lastGame.getRegisteredPlayers().get(i);
            RegisteredPlayer registered = gamePlayer.getRegisteredPlayer();

            // Add/Remove Cards lost via ChangeOwnership cards like Darkpact
            CardCollectionView lostOwnership = gamePlayer.getLostOwnership();
            CardCollectionView gainedOwnership = gamePlayer.getGainedOwnership();

            if (!lostOwnership.isEmpty()) {
                List<PaperCard> lostPaperOwnership = new ArrayList<>();
                for (Card c : lostOwnership) {
                    lostPaperOwnership.add((PaperCard)c.getPaperCard());
                }
                outcome.addAnteLost(registered, lostPaperOwnership);
            }

            if (!gainedOwnership.isEmpty()) {
                List<PaperCard> gainedPaperOwnership = new ArrayList<>();
                for (Card c : gainedOwnership) {
                    gainedPaperOwnership.add((PaperCard)c.getPaperCard());
                }
                outcome.addAnteWon(registered, gainedPaperOwnership);
            }

            if (!getRules().useAnte()) {
                continue;
            }

            if (outcome.isDraw()) {
                continue;
            }

            if (!gamePlayer.hasLost()) {
                iWinner = i;
                continue; // not a loser
            }

            Deck losersDeck = players.get(i).getDeck();
            List<PaperCard> personalLosses = new ArrayList<>();
            for (Card c : gamePlayer.getCardsIn(ZoneType.Ante)) {
                if (!c.isCollectible())
                    continue;
                PaperCard toRemove = (PaperCard) c.getPaperCard();
                // this could miss the cards by returning instances that are not equal to cards found in deck
                // (but only if the card has multiple prints in a set)
                losersDeck.getMain().remove(toRemove);
                personalLosses.add(toRemove);
                losses.add(toRemove);
            }

            outcome.addAnteLost(registered, personalLosses);
        }

        if (rules.useAnte() && iWinner >= 0) {
            // Winner gains these cards always
            Player fromGame = lastGame.getRegisteredPlayers().get(iWinner);
            RegisteredPlayer registered = fromGame.getRegisteredPlayer();
            outcome.addAnteWon(registered, losses);

            if (rules.getGameType().canAddWonCardsMidGame()) {
                // But only certain game types lets you swap midgame
                List<PaperCard> chosen = fromGame.getController().chooseCardsYouWonToAddToDeck(losses);
                if (null != chosen) {
                    Deck deck = players.get(iWinner).getDeck();
                    for (PaperCard c : chosen) {
                        deck.getMain().add(c);
                    }
                }
            }
            // Other game types (like Quest) need to do something in their own calls to actually update data
        }
    }

    public GameOutcome.AnteResult getAnteResult(RegisteredPlayer player) {
        GameOutcome.AnteResult out = new GameOutcome.AnteResult();
        for (GameOutcome outcome : gameOutcomes.values()) {
            GameOutcome.AnteResult gameAnte = outcome.getAnteResult(player);
            if (gameAnte == null) {
                continue;
            }
            out.addWon(gameAnte.wonCards);
            out.addLost(gameAnte.lostCards);
        }
        return out;
    }

    /**
     * Fire only the events after they became real for gamestate and won't get replaced.<br>
     * The events are sent to UI, log and sound system. Network listeners are under development.
     */
    public void fireEvent(final Event event) {
        events.post(event);
    }
    public void subscribeToEvents(final Object subscriber) {
        events.register(subscriber);
    }

}
