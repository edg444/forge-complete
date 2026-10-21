package forge.game.ability.effects;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.GameOutcome;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.event.EventValueChangeType;
import forge.game.event.GameEventDayTimeChanged;
import forge.game.event.GameEventSubgameEnd;
import forge.game.event.GameEventSubgameStart;
import forge.game.event.GameEventZone;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Localizer;
import forge.util.collect.FCollectionView;

public class SubgameEffect extends SpellAbilityEffect {

    private Game createSubGame(Game maingame, int startingLife) {
        List<RegisteredPlayer> players = Lists.newArrayList();

        // Add remaining players to subgame
        for (Player p : maingame.getPlayers()) {
            players.add(p.getRegisteredPlayer());
        }

        return new Game(players, maingame.getRules(), maingame.getMatch(), maingame, startingLife);
    }

    private void setCardsInZone(Player player, final ZoneType zoneType, final CardCollectionView oldCards, boolean addMapping) {
        PlayerZone zone = player.getZone(zoneType);
        List<Card> newCards = Lists.newArrayList();
        for (final Card card : oldCards) {
            if (card.isToken() || card.isCopiedSpell()) continue;
            Card newCard = Card.fromPaperCard(card.getPaperCard(), player);
            newCards.add(newCard);
            if (addMapping) {
                // Build mapping between maingame cards and subgame cards,
                // so when subgame pick a card from maingame (like Wish effects),
                // The maingame card will also be moved.
                // (Will be move to Subgame zone, which will be added back to library after subgame ends.)
                player.addMaingameCardMapping(newCard, card);
            }
        }
        zone.setCards(newCards);
    }

    private void initVariantsZonesSubgame(final Game subgame, final Player maingamePlayer, final Player player) {
        PlayerZone com = player.getZone(ZoneType.Command);
        RegisteredPlayer registeredPlayer = player.getRegisteredPlayer();

        // Vanguard
        if (registeredPlayer.getVanguardAvatars() != null) {
            for(PaperCard avatar:registeredPlayer.getVanguardAvatars()) {
                com.add(Card.fromPaperCard(avatar, player));
            }
        }

        // Commander
        final CardCollectionView commandCards = maingamePlayer.getCardsIn(ZoneType.Command);
        for (final Card card : commandCards) {
            if (card.isCommander()) {
                Card cmd = Card.fromPaperCard(card.getPaperCard(), player);
                player.initCommanderColor(cmd);
                com.add(cmd);
                player.addCommander(cmd);
            }
        }

        // Conspiracies
        // 720.2 doesn't mention Conspiracy cards so I guess they don't move
    }

    /**
     * Tug of War: up to max permanent cards with different names from a player's main-game library, set aside to start
     * the subgame on the battlefield.
     */
    private static forge.game.card.CardCollection chooseStartingPermanents(final Player p, final int max, final SpellAbility sa) {
        final forge.game.card.CardCollection chosen = new forge.game.card.CardCollection();
        for (int i = 0; i < max; i++) {
            final forge.game.card.CardCollection options = forge.game.card.CardLists.filter(p.getCardsIn(ZoneType.Library),
                    c -> c.isPermanent() && chosen.stream().noneMatch(x -> x.sharesNameWith(c)));
            if (options.isEmpty()) {
                break;
            }
            final Card c = p.getController().chooseSingleEntityForEffect(options, sa, "Choose a permanent card to start the "
                    + "subgame on the battlefield (" + (max - i) + " more at most)", true, null);
            if (c == null) {
                break;
            }
            chosen.add(c);
        }
        return chosen;
    }

    private void prepareAllZonesSubgame(final Game maingame, final Game subgame) {
        prepareAllZonesSubgame(maingame, subgame, Maps.newHashMap());
    }

    private void prepareAllZonesSubgame(final Game maingame, final Game subgame, final Map<Player, forge.game.card.CardCollection> setAside) {
        final FCollectionView<Player> players = subgame.getPlayers();
        final FCollectionView<Player> maingamePlayers = maingame.getPlayers();
        final List<ZoneType> outsideZones = Arrays.asList(ZoneType.Hand, ZoneType.Battlefield,
                ZoneType.Graveyard, ZoneType.Exile, ZoneType.Stack, ZoneType.Sideboard, ZoneType.Ante, ZoneType.Merged);

        for (int i = 0; i < players.size(); i++) {
            final Player player = players.get(i);
            final Player maingamePlayer = maingamePlayers.get(i);

            // Library, less any cards set aside to start on the battlefield
            final forge.game.card.CardCollection aside = setAside.getOrDefault(maingamePlayer, new forge.game.card.CardCollection());
            final forge.game.card.CardCollection library = new forge.game.card.CardCollection(maingamePlayer.getCardsIn(ZoneType.Library));
            library.removeAll(aside);
            setCardsInZone(player, ZoneType.Library, library, false);

            // Sideboard
            // 720.4
            final CardCollectionView outsideCards = maingame.getCardsInOwnedBy(outsideZones, maingamePlayer);
            if (!outsideCards.isEmpty()) {
                setCardsInZone(player, ZoneType.Sideboard, outsideCards, true);
                // Update card view so it shows the origin zone in text.
                for (Card c : player.getCardsIn(ZoneType.Sideboard)) {
                    c.updateStateForView();
                }

                player.assignCompanion(subgame, player.getController());
            }

            // Schemes
            setCardsInZone(player, ZoneType.SchemeDeck, maingamePlayer.getCardsIn(ZoneType.SchemeDeck), false);

            // Planes
            setCardsInZone(player, ZoneType.PlanarDeck, maingamePlayer.getCardsIn(ZoneType.PlanarDeck), false);

            // Attractions
            setCardsInZone(player, ZoneType.AttractionDeck, maingamePlayer.getCardsIn(ZoneType.AttractionDeck), false);

            // Contraptions
            setCardsInZone(player, ZoneType.ContraptionDeck, maingamePlayer.getCardsIn(ZoneType.ContraptionDeck), false);

            // Vanguard and Commanders
            initVariantsZonesSubgame(subgame, maingamePlayer, player);

            player.shuffle(null);
            player.getZone(ZoneType.SchemeDeck).shuffle();
            player.getZone(ZoneType.PlanarDeck).shuffle();
            player.getZone(ZoneType.AttractionDeck).shuffle();
            player.getZone(ZoneType.ContraptionDeck).shuffle();
        }
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card hostCard = sa.getHostCard();
        final Game maingame = hostCard.getGame();

        int startingLife = -1;
        if (sa.hasParam("StartingLife")) {
            startingLife = Integer.parseInt(sa.getParam("StartingLife"));
        }
        Game subgame = createSubGame(maingame, startingLife);

        String startMessage = Localizer.getInstance().getMessage("lblSubgameStart", hostCard.getTranslatedName());
        maingame.getMatch().fireEvent(new GameEventSubgameStart(subgame, startMessage));

        // Tug of War: each player's set-aside cards, and their subgame copies, which go onto the battlefield once
        // mulligans are done, starting player first (rulings)
        final Map<Player, forge.game.card.CardCollection> setAside = Maps.newHashMap();
        if (sa.hasParam("StartingPermanents")) {
            final int max = Integer.parseInt(sa.getParam("StartingPermanents"));
            for (final Player p : maingame.getPlayersInTurnOrder(sa.getActivatingPlayer())) {
                setAside.put(p, chooseStartingPermanents(p, max, sa));
            }
            subgame.setAfterMulligans(first -> {
                for (final Player p : subgame.getPlayersInTurnOrder(first)) {
                    final Player main = maingame.getPlayers().get(subgame.getPlayers().indexOf(p));
                    for (final Card c : setAside.getOrDefault(main, new forge.game.card.CardCollection())) {
                        final Card copy = Card.fromPaperCard(c.getPaperCard(), p);
                        p.getZone(ZoneType.Sideboard).add(copy);
                        subgame.getAction().moveToPlay(copy, p, null, AbilityKey.newMap());
                    }
                }
            });
        }
        prepareAllZonesSubgame(maingame, subgame, setAside);
        subgame.getAction().startGame(null, null);
        subgame.clearCaches();

        // Find out winners and losers
        final GameOutcome outcome = subgame.getOutcome();
        List<Player> winPlayers = Lists.newArrayList();
        List<Player> notWinPlayers = Lists.newArrayList();
        StringBuilder sbWinners = new StringBuilder();
        StringBuilder sbLosers = new StringBuilder();
        for (Player p : maingame.getPlayers()) {
            if (outcome.isWinner(p.getRegisteredPlayer())) {
                if (!winPlayers.isEmpty()) {
                    sbWinners.append(", ");
                }
                sbWinners.append(p.getName());
                winPlayers.add(p);
            } else {
                if (!notWinPlayers.isEmpty()) {
                    sbLosers.append(", ");
                }
                sbLosers.append(p.getName());
                notWinPlayers.add(p);
            }
        }

        if (sa.hasParam("RememberPlayers")) {
            final String param = sa.getParam("RememberPlayers");
            if (param.equals("Win")) {
                hostCard.addRemembered(winPlayers);
            } else if (param.equals("NotWin")) {
                hostCard.addRemembered(notWinPlayers);
            }
        }

        String endMessage = outcome.isDraw() ? Localizer.getInstance().getMessage("lblSubgameEndDraw") :
                Localizer.getInstance().getMessage("lblSubgameEnd", sbWinners.toString(), sbLosers.toString());
        maingame.getMatch().fireEvent(new GameEventSubgameEnd(maingame, endMessage));
        if (maingame.getDayTime() != null) {
            maingame.fireEvent(new GameEventDayTimeChanged(maingame.getDayTime()));
        }
        // only mobile seems to need this
        for (Player p : maingame.getPlayers()) {
            maingame.fireEvent(new GameEventZone(ZoneType.Battlefield, p, EventValueChangeType.ComplexUpdate, null));
            maingame.fireEvent(new GameEventZone(ZoneType.Hand, p, EventValueChangeType.ComplexUpdate, null));
            maingame.fireEvent(new GameEventZone(ZoneType.Graveyard, p, EventValueChangeType.ComplexUpdate, null));
            maingame.fireEvent(new GameEventZone(ZoneType.Exile, p, EventValueChangeType.ComplexUpdate, null));
            maingame.fireEvent(new GameEventZone(ZoneType.Command, p, EventValueChangeType.ComplexUpdate, null));
        }

        // Tug of War: the winner puts one of the cards they set aside onto the main-game battlefield instead
        if (sa.hasParam("StartingPermanents") && winPlayers.size() == 1 && !outcome.isDraw()) {
            final Player winner = winPlayers.get(0);
            final forge.game.card.CardCollection options = forge.game.card.CardLists.filter(
                    setAside.getOrDefault(winner, new forge.game.card.CardCollection()), c -> c.isInZone(ZoneType.Library));
            if (!options.isEmpty()) {
                final Card kept = winner.getController().chooseSingleEntityForEffect(options, sa,
                        "Choose a card to put onto the battlefield", false, null);
                if (kept != null) {
                    maingame.getAction().moveToPlay(kept, winner, sa, AbilityKey.newMap());
                }
            }
        }

        // Setup maingame library
        final FCollectionView<Player> subgamePlayers = subgame.getRegisteredPlayers();
        final FCollectionView<Player> players = maingame.getPlayers();
        for (int i = 0; i < players.size(); i++) {
            final Player subgamePlayer = subgamePlayers.get(i);
            final Player player = players.get(i);

            // All cards moved to Subgame Zone will be put into library when subgame ends.
            // 720.5
            final CardCollectionView movedCards = player.getCardsIn(ZoneType.Subgame);
            PlayerZone library = player.getZone(ZoneType.Library);
            for (final Card card : movedCards) {
                library.add(card);
            }
            player.getZone(ZoneType.Subgame).removeAllCards(true);

            // Move commander if it is no longer in subgame's commander zone
            // 720.5c
            List<Card> subgameCommanders = Lists.newArrayList();
            List<Card> movedCommanders = Lists.newArrayList();
            for (final Card card : subgamePlayer.getCardsIn(ZoneType.Command)) {
                if (card.isCommander()) {
                    subgameCommanders.add(card);
                }
            }
            for (final Card card : player.getCardsIn(ZoneType.Command)) {
                if (card.isCommander()) {
                    boolean isInSubgameCommand = false;
                    for (final Card subCard : subgameCommanders) {
                        if (card.getName().equals(subCard.getName())) {
                            isInSubgameCommand = true;
                        }
                    }
                    if (!isInSubgameCommand) {
                        movedCommanders.add(card);
                    }
                }
            }
            for (final Card card : movedCommanders) {
                maingame.getAction().moveTo(ZoneType.Library, card, null, AbilityKey.newMap());
            }

            player.shuffle(sa);
            player.getZone(ZoneType.SchemeDeck).shuffle();
            player.getZone(ZoneType.PlanarDeck).shuffle();
            player.getZone(ZoneType.AttractionDeck).shuffle();
            player.getZone(ZoneType.ContraptionDeck).shuffle();
        }
    }

}
