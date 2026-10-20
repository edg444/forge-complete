package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.List;

import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.ArtistCredit;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.spellability.SpellAbility;

/**
 * Abstract Iguanart: "note the first letter of its artist's name. If that letter wasn't already noted, put a +1/+1
 * counter on this creature." Rulings: each artist a credit names gives a letter (Zoltan Boros & Gabor Szikszai note Z
 * and G, a counter for each new one); the letter is the first character of the name as credited, whatever the
 * language; no artist, no letter. Letters are kept as the card's noted values, so they show on it.
 */
public class NoteArtistInitialsEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Note the first letter of the spell's artist's name.";
    }

    /** The first character of each credited artist's name, upper case. */
    public static List<String> initials(final String credit) {
        final List<String> out = new ArrayList<>();
        for (final String name : ArtistCredit.individuals(credit)) {
            out.add(new String(Character.toChars(name.codePointAt(0))).toUpperCase());
        }
        return out;
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        int fresh = 0;
        for (final Card spell : getDefinedCardsOrTargeted(sa)) {
            for (final String letter : initials(spell.getArtist())) {
                boolean noted = false;
                for (final String n : host.getNotedTypes()) {
                    noted |= n.equals(letter);
                }
                if (!noted) {
                    host.addNotedType(letter);
                    fresh++;
                }
                game.getGameLog().add(GameLogEntryType.INFORMATION, host + " notes " + letter
                        + (noted ? " (already noted)." : "."));
            }
        }
        if (fresh > 0 && host.isInPlay()) {
            final GameEntityCounterTable table = new GameEntityCounterTable();
            host.addCounter(CounterEnumType.P1P1, fresh, sa.getActivatingPlayer(), table);
            table.replaceCounterEffect(game, sa);
        }
    }
}
