package forge.game;

import java.util.List;

import com.google.common.collect.Lists;

import forge.ImageKeys;
import forge.StaticData;
import forge.card.CardEdition;
import forge.deck.Deck;
import forge.game.card.Card;
import forge.card.GamePieceType;
import forge.game.player.Player;
import forge.game.replacement.ReplacementHandler;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.item.BoosterPack;
import forge.item.PaperCard;
import forge.util.Aggregates;

/**
 * Pai Gow Magic (Frank Karsten's rules) and its Booster Blitz variant: each player splits one booster into face-down
 * piles of three, which in a random order become the opening hands of a series of separate games. No libraries,
 * 5 life, unlimited mana of any colors; whoever wins three games wins the match.
 */
public final class PaiGowMagic {
    public static final int PILE_SIZE = 3;
    public static final int STARTING_LIFE = 5;

    private PaiGowMagic() { }

    /** Piles, and so games: 5 for Pai Gow, 4 for Booster Blitz, 0 when neither is played. */
    public static int pileCount(final GameRules rules) {
        if (rules.hasAppliedVariant(GameType.PaiGow)) {
            return 5;
        }
        if (rules.hasAppliedVariant(GameType.BoosterBlitz)) {
            return 4;
        }
        return 0;
    }

    public static boolean isPlayed(final GameRules rules) {
        return pileCount(rules) > 0;
    }

    /**
     * Three cards a pile. A pack short of cards (a 14-card Play Booster) shortens the last piles (user ruling
     * 2026-10-30: 3,3,3,3,2), and cards beyond the piles - Booster Blitz has room for 12 - are left out.
     */
    public static List<Integer> pileSizes(final int cards, final int piles) {
        final List<Integer> sizes = Lists.newArrayList();
        for (int i = 0; i < piles; i++) {
            sizes.add(Math.max(0, Math.min(PILE_SIZE, cards - PILE_SIZE * i)));
        }
        return sizes;
    }

    /** The pack dealt into the piles in order, for a player that makes no choice of its own. */
    public static List<List<PaperCard>> dealInOrder(final List<PaperCard> pack, final List<Integer> sizes) {
        final List<List<PaperCard>> piles = Lists.newArrayList();
        int next = 0;
        for (final int size : sizes) {
            piles.add(Lists.newArrayList(pack.subList(next, next + size)));
            next += size;
        }
        return piles;
    }

    /** Sets that have a booster to open, newest first. */
    public static List<CardEdition> boosterEditions() {
        final List<CardEdition> editions = Lists.newArrayList();
        for (final CardEdition edition : StaticData.instance().getEditions().getOrderedEditions()) {
            if (edition.hasBoosterTemplate()) {
                editions.add(edition);
            }
        }
        return editions;
    }

    /** One booster - the set's regular one, never a collector booster - as the deck; a random set when null. */
    public static Deck openBooster(final CardEdition edition) {
        final CardEdition set = edition != null ? edition : Aggregates.random(boosterEditions());
        final BoosterPack pack = new BoosterPack(set.getName() + " Booster", set.getBoosterTemplate("Draft"));
        final Deck deck = new Deck(pack.getName());
        deck.getMain().add(pack.getCards());
        return deck;
    }

    /** The format's own rules, on a card in the command zone so they can be read during the game. */
    public static Card createRulesEffect(final Player player) {
        final Game game = player.getGame();
        final Card eff = new Card(game.nextCardId(), game);
        eff.setGameTimestamp(game.getNextTimestamp());
        eff.setName(game.getRules().hasAppliedVariant(GameType.BoosterBlitz)
                ? GameType.BoosterBlitz.toString() : GameType.PaiGow.toString());
        eff.setOwner(player);
        eff.setGamePieceType(GamePieceType.EFFECT);
        eff.setImageKey(ImageKeys.getTokenKey("pai_gow_magic"));

        eff.addStaticAbility("Mode$ Continuous | EffectZone$ Command | Affected$ You | Description$ You have an "
                + "unlimited amount of mana in any combination of colors.");
        eff.addReplacementEffect(ReplacementHandler.parseReplacement("Event$ GameLoss | ActiveZones$ Command | "
                + "ValidPlayer$ You | ValidLoseReason$ Milled | Layer$ CantHappen | Description$ There are no "
                + "libraries, and you can't lose the game for drawing from an empty one.", eff, true));

        final PlayerZone com = player.getZone(ZoneType.Command);
        eff.updateStateForView();
        com.add(eff);
        player.updateZoneForView(com);
        player.getManaPool().makeUnlimited(eff);
        return eff;
    }
}
