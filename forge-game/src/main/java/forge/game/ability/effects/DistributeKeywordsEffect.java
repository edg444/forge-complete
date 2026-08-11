package forge.game.ability.effects;

import java.util.List;

import com.google.common.collect.Lists;

import forge.GameCommand;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.keyword.KeywordInterface;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Some Disassembly Required - "Distribute the sacrificed creature's keyword abilities among any
 * number of other target creatures."
 * <p>
 * Distribute, not copy: each keyword goes to exactly one recipient, the way damage is divided,
 * rather than every recipient gaining the whole set. Repeated instances of a keyword are handed out
 * separately, so a creature with two of something can split them between two recipients.
 */
public class DistributeKeywordsEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return sa.getHostCard() + " - distribute keyword abilities.";
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final Player chooser = sa.getActivatingPlayer();
        if (chooser == null) {
            return;
        }

        final List<Card> sources = AbilityUtils.getDefinedCards(host, sa.getParam("Source"), sa);
        final CardCollection recipients = new CardCollection(getTargetCards(sa));
        int distributed = 0;

        if (!sources.isEmpty() && !recipients.isEmpty()) {
            final Card source = sources.get(0);
            final List<KeywordInterface> keywords = Lists.newArrayList(source.getKeywords());
            final long timestamp = game.getNextTimestamp();
            final CardCollection given = new CardCollection();

            // Collect every assignment first, then grant each creature its whole set in one call.
            // Changed keywords are keyed by (timestamp, staticId), so granting them one at a time
            // under a shared timestamp overwrites rather than accumulates - a creature given three
            // keywords would end up with only the last one.
            final com.google.common.collect.Multimap<Card, String> assigned =
                    com.google.common.collect.ArrayListMultimap.create();
            for (final KeywordInterface kw : keywords) {
                final Card target = chooser.getController().chooseSingleEntityForEffect(recipients, sa,
                        "Give " + kw.getOriginal() + " to which creature?", false, null);
                if (target == null) {
                    continue;
                }
                assigned.put(target, kw.getOriginal());
                distributed++;
            }
            for (final Card target : assigned.keySet()) {
                target.addChangedCardKeywords(Lists.newArrayList(assigned.get(target)),
                        Lists.newArrayList(), false, timestamp, null);
                given.add(target);
            }

            if (!given.isEmpty()) {
                final GameCommand untilEOT = new GameCommand() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void run() {
                        for (final Card c : given) {
                            c.removeChangedCardKeywords(timestamp, 0);
                            c.updateAbilityTextForView();
                        }
                    }
                };
                addUntilCommand(sa, untilEOT);
            }
        }

        if (sa.hasParam("RememberDistributed")) {
            host.addRemembered(Integer.valueOf(distributed));
        }
    }
}
