package forge.game.ability.effects;

import java.util.List;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.item.IPaperCard;
import forge.util.Localizer;

// Squirrel Farm: "...Target opponent guesses the artist. If they guess wrong, create a 1/1 green
// Squirrel creature token." Artist is real, verifiable per-printing data Forge already tracks
// (IPaperCard.getArtist(), reachable off a live Card via getPaperCard()) - unlike most Un-set
// mechanics this session, this needed a genuine free-text guess-and-compare primitive
// (PlayerController.guessString) rather than an honor-system workaround, since the answer is
// actually checkable. Built generically (DefinedCard$/Defined$/GuessCorrect$/GuessWrong$) so future
// "guess a real fact about a card" Un-set cards can reuse this instead of each needing new Java.
public class GuessArtistEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        if (sa.hasParam("SpellDescription")) {
            return sa.getParam("SpellDescription");
        }
        return sa.getHostCard().getName() + " - guess the artist.";
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();

        final List<Card> cards = AbilityUtils.getDefinedCards(host, sa.getParam("DefinedCard"), sa);
        if (cards.isEmpty()) {
            return;
        }
        final Card revealed = cards.get(0);
        final IPaperCard pc = revealed.getPaperCard();
        final String actualArtist = pc == null ? "" : pc.getArtist();

        final List<Player> guessers = getDefinedPlayersOrTargeted(sa, "Defined");
        if (guessers.isEmpty()) {
            return;
        }
        final Player guesser = guessers.get(0);

        final String guess = guesser.getController().guessString(sa,
                Localizer.getInstance().getMessage("lblNameACard") + " - " + revealed.getName() + ": guess the artist");
        final boolean correct = !actualArtist.isEmpty() && guess != null && guess.trim().equalsIgnoreCase(actualArtist.trim());

        host.getGame().getAction().notifyOfValue(sa, guesser,
                (correct ? "Correct! " : "Wrong. ") + "The artist was " + actualArtist + ".", null);

        if (correct && sa.hasParam("GuessCorrect")) {
            AbilityUtils.resolve(sa.getAdditionalAbility("GuessCorrect"));
        } else if (!correct && sa.hasParam("GuessWrong")) {
            AbilityUtils.resolve(sa.getAdditionalAbility("GuessWrong"));
        }
    }
}
