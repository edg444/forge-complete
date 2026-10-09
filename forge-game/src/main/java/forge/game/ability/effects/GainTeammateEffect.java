package forge.game.ability.effects;

import java.util.Map;

import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Better Than One: "A person outside the game becomes your teammate. (Choose any number of cards in your hand, on top
 * of your library, or on the battlefield under your control. Those cards become your teammate's hand, library, and
 * permanents, respectively.)"
 * <p>
 * From the rulings and the Unstable FAQ: your side of the game becomes a Two-Headed Giant team from then on (three heads
 * if it already had two); the teammate brings no life total of their own, so the team's is yours; for game purposes
 * they own what you give them; only you give them cards. The user ruled the person is played by the AI.
 */
public class GainTeammateEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "A person outside the game becomes " + sa.getActivatingPlayer() + "'s teammate.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Player you = sa.getActivatingPlayer();
        final Game game = you.getGame();

        // the cards are chosen as part of the instructions, before the teammate exists
        final CardCollectionView inHand = you.getCardsIn(ZoneType.Hand);
        final Map<String, Object> params = Maps.newHashMap();
        params.put("Zone", ZoneType.Hand);
        final CardCollection hand = new CardCollection(you.getController().chooseCardsForEffect(inHand, sa,
                "Choose any number of cards in your hand to become your teammate's hand", 0, inHand.size(), true, params));

        final CardCollectionView library = you.getCardsIn(ZoneType.Library);
        final int fromTop = library.isEmpty() ? 0 : you.getController().chooseNumber(sa,
                "How many cards from the top of your library become your teammate's library?", 0, library.size(), Maps.newHashMap());
        final CardCollection libraryCards = new CardCollection();
        for (int i = 0; i < fromTop; i++) {
            libraryCards.add(library.get(i));
        }

        final CardCollectionView yours = you.getCardsIn(ZoneType.Battlefield);
        params.put("Zone", ZoneType.Battlefield);
        final CardCollection permanents = new CardCollection(you.getController().chooseCardsForEffect(yours, sa,
                "Choose any number of permanents you control to become your teammate's permanents", 0, yours.size(), true, params));

        // the new head sits after the rest of your team; your side of the game becomes a Two-Headed Giant team, with
        // your life total as the team's (Unstable FAQ: "They won't need one")
        final PlayerCollection team = you.getGiantTeam();
        final Player teammate = game.addOutsidePlayer(team.get(team.size() - 1));
        if (you.isOnGiantTeam()) {
            teammate.joinLifeTotal(you, you.getLife());
        } else {
            final PlayerCollection newTeam = new PlayerCollection();
            newTeam.add(you);
            newTeam.addAll(you.getTeamMates(false));
            newTeam.add(teammate);
            game.formGiantTeam(newTeam, you.getLife());
        }
        game.getGameLog().add(GameLogEntryType.INFORMATION, teammate + " joins the game as " + you + "'s teammate.");

        // for game purposes they own what they're given (rulings) - set directly rather than as an ownership change,
        // which ante would treat as cards lost from your deck, and the FAQ says they're still yours after the game
        for (final Card c : hand) {
            c.setOwner(teammate);
            game.getAction().moveToHand(c, sa);
        }
        for (final Card c : libraryCards) {
            c.setOwner(teammate);
            // top card first, each to the bottom, keeps the order they had on top of yours
            game.getAction().moveToLibrary(c, -1, sa);
        }
        for (final Card c : permanents) {
            if (!c.isInPlay()) {
                continue;
            }
            c.setOwner(teammate);
            c.setController(teammate, game.getNextTimestamp());
            game.getAction().controllerChangeZoneCorrection(c);
        }

        teammate.updateOpponentsForView();
        for (final Player p : game.getPlayers()) {
            p.updateOpponentsForView();
        }
    }
}
