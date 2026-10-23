package forge.game.mulligan;

import java.util.List;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.MulliganDefs;
import forge.StaticData;
import forge.game.Game;
import forge.game.GameType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class MulliganService {
    Player firstPlayer;
    Game game;
    List<AbstractMulligan> mulligans = Lists.newArrayList();

    public MulliganService(Player player) {
        firstPlayer = player;
        game = firstPlayer.getGame();
    }

    public void perform() {
        initializeMulligans();
        runPlayerMulligans();
        runPostMulligans();
    }

    private void initializeMulligans() {
        List<Player> whoCanMulligan = Lists.newArrayList(game.getPlayers());
        int offset = whoCanMulligan.indexOf(firstPlayer);

        for (int i = 0; i < offset; i++) {
            whoCanMulligan.add(whoCanMulligan.remove(0));
        }

        boolean firstMullFree = game.getPlayers().size() > 2 || game.getRules().hasAppliedVariant(GameType.Brawl);

        for (Player player : whoCanMulligan) {
            MulliganDefs.MulliganRule rule = StaticData.instance().getMulliganRule();
            AbstractMulligan mulligan;
            switch (rule) {
                case Original:
                    mulligan = new OriginalMulligan(player, firstMullFree);
                    break;
                case Paris:
                    mulligan = new ParisMulligan(player, firstMullFree);
                    break;
                case Vancouver:
                    mulligan = new VancouverMulligan(player, firstMullFree);
                    break;
                case London:
                    mulligan = new LondonMulligan(player, firstMullFree);
                    break;
                case Houston:
                    mulligan = new HoustonMulligan(player, firstMullFree);
                    break;
                default:
                    mulligan = new VancouverMulligan(player, firstMullFree);
                    break;
            }

            mulligans.add(mulligan);
            mulligan.beforeFirstMulligan();
        }
    }

    private void runPlayerMulligans() {
        if (game.hasGiantTeams()) {
            runTeamMulligans();
            return;
        }
        boolean allKept;
        do {
            allKept = true;
            for (AbstractMulligan mulligan : mulligans) {
                if (mulligan.hasKept()) {
                    continue;
                }

                Player p = mulligan.getPlayer();
                if (mulligan.canMulligan()) {
                    offerMulliganPeeks(p);
                }

                boolean keep = !mulligan.canMulligan() ||
                        p.getController().mulliganKeepHand(
                                firstPlayer,
                                mulligan.tuckCardsDuringMulligan()
                        );

                if (game.isGameOver()) {
                    // conceded during mulligan prompt
                    return;
                }

                if (keep) {
                    mulligan.keep();
                    continue;
                }

                allKept = false;
                mulligan.mulligan();
            }
        } while (!allKept);
    }

    /**
     * CR 805.3a, for shared team turns: the starting team's players declare first, then each other team in turn order
     * (seat order from the starting team's primary player gives exactly that), and only once everyone has declared
     * are the mulligans taken, all at the same time. Each player decides for themselves, so one can mulligan after a
     * teammate keeps.
     */
    private void runTeamMulligans() {
        while (true) {
            final List<AbstractMulligan> taking = Lists.newArrayList();
            for (AbstractMulligan mulligan : mulligans) {
                if (mulligan.hasKept()) {
                    continue;
                }
                if (mulligan.canMulligan()) {
                    offerMulliganPeeks(mulligan.getPlayer());
                }
                final boolean keep = !mulligan.canMulligan()
                        || mulligan.getPlayer().getController().mulliganKeepHand(firstPlayer, mulligan.tuckCardsDuringMulligan());
                if (game.isGameOver()) {
                    // conceded during mulligan prompt
                    return;
                }
                if (keep) {
                    mulligan.keep();
                } else {
                    taking.add(mulligan);
                }
            }
            if (taking.isEmpty()) {
                return;
            }
            for (AbstractMulligan mulligan : taking) {
                mulligan.mulligan();
            }
        }
    }

    /**
     * No-Regrets Egret: any time its owner could mulligan with it in hand, they may reveal it to look at the top
     * SVar:MulliganPeek cards of their library, which stay in order. The AI, which wouldn't use what it saw, doesn't.
     */
    private void offerMulliganPeeks(final Player p) {
        if (p.getController().isAI()) {
            return;
        }
        for (final Card c : p.getCardsIn(ZoneType.Hand)) {
            if (!c.hasSVar("MulliganPeek") || p.getCardsIn(ZoneType.Library).isEmpty()) {
                continue;
            }
            final int n = Integer.parseInt(c.getSVar("MulliganPeek"));
            final SpellAbility sa = new SpellAbility.EmptySa(c, p);
            if (!p.getController().confirmAction(sa, null, "Reveal " + c.getTranslatedName() + " to look at the top "
                    + n + " cards of your library?", null)) {
                continue;
            }
            game.getAction().reveal(new CardCollection(c), p, true, c.getTranslatedName() + " - revealed by ");
            final CardCollection top = new CardCollection(Iterables.limit(p.getCardsIn(ZoneType.Library), n));
            p.getController().reveal(top, ZoneType.Library, p, "Top of your library: ", false);
        }
    }

    private void runPostMulligans() {
        for (AbstractMulligan mulligan : mulligans) {
            mulligan.afterMulligan();
        }
    }
}