package forge.game.ability.effects;

import java.util.List;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.card.CardFlavorText;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.item.IPaperCard;

/**
 * My First Tome. The flavor text is shown to the guesser rather than described, because the point of
 * the card is that they hear it and nothing else - the name stays hidden until the reveal.
 * <p>
 * Shaped like GuessArtistEffect (DefinedCard$/Defined$/GuessCorrect$/GuessWrong$) so the two read
 * the same way.
 */
public class GuessNameEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        if (sa.hasParam("SpellDescription")) {
            return sa.getParam("SpellDescription");
        }
        return sa.getHostCard().getName() + " - guess the card's name.";
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();

        final List<Card> cards = AbilityUtils.getDefinedCards(host, sa.getParam("DefinedCard"), sa);
        if (cards.isEmpty()) {
            return;
        }
        final Card hidden = cards.get(0);

        final List<Player> guessers = getDefinedPlayersOrTargeted(sa, "Defined");
        if (guessers.isEmpty()) {
            return;
        }
        final Player guesser = guessers.get(0);

        final IPaperCard pc = hidden.getPaperCard();
        String flavor = pc == null ? "" : CardFlavorText.get(pc.getEdition(), pc.getCollectorNumber());
        if (flavor == null || flavor.isEmpty()) {
            flavor = "(this card has no flavor text)";
        }

        // Tell the chooser what they're supposed to be saying out loud. Forge picks the card for them
        // without asking whenever only one card in hand has flavor text, so this is often the first
        // they hear of it - and the whole physical action of the card is reading this aloud.
        final Player sayer = sa.getActivatingPlayer();
        if (sayer != null) {
            sayer.getController().notifyOfValue(sa, hidden,
                    "Say the flavor text on " + hidden.getName() + ":\n\n\"" + flavor + "\"");
        }

        final String guess = guesser.getController().guessString(sa,
                "\"" + flavor + "\"\n\nName the card that flavor text is on");
        final boolean correct = guess != null && guess.trim().equalsIgnoreCase(hidden.getName().trim());

        // name the guess, not just the verdict - "Not it." alone gives away nothing about what was
        // actually said, which is half the fun and all of the information
        final String said = guess == null || guess.trim().isEmpty() ? "(no guess)" : guess.trim();
        host.getGame().getAction().notifyOfValue(sa, guesser,
                said + (correct ? " - guessed it." : " - not it."), null);

        if (correct && sa.hasParam("GuessCorrect")) {
            AbilityUtils.resolve(sa.getAdditionalAbility("GuessCorrect"));
        } else if (!correct && sa.hasParam("GuessWrong")) {
            AbilityUtils.resolve(sa.getAdditionalAbility("GuessWrong"));
        }
    }
}
