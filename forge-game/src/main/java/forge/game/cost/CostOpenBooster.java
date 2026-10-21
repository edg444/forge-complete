package forge.game.cost;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import forge.game.GameLogEntryType;
import forge.game.ability.effects.OpenBoosterEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.MyRandom;

/**
 * Myntasha, Honored One: "open a sealed Magic booster pack and put the cards on the bottom of your booster pile in a
 * random order". As with Stocking Tiger, the pack is a real one generated from the chosen set's own booster template.
 * The booster pile is outside the game, so the cards just go there - nothing enters or leaves the game.
 */
public class CostOpenBooster extends CostPart {
    private static final long serialVersionUID = 1L;

    public CostOpenBooster(final String amount) {
        this.setAmount(amount);
    }

    @Override
    public boolean canPay(final SpellAbility ability, final Player payer, final boolean effect) {
        return !OpenBoosterEffect.boosterSetCodes().isEmpty();
    }

    @Override
    public boolean payAsDecided(final Player payer, final PaymentDecision pd, final SpellAbility sa, final boolean effect) {
        final List<PaperCard> pack = OpenBoosterEffect.openPack(pd.type);
        if (pack.isEmpty()) {
            return false;
        }
        final List<Card> opened = Lists.newArrayList();
        for (final PaperCard pc : pack) {
            opened.add(Card.fromPaperCard(pc, payer));
        }
        Collections.shuffle(opened, MyRandom.getRandom());
        final PlayerZone pile = payer.getZone(ZoneType.BoosterPile);
        for (final Card c : opened) {
            pile.add(c);
        }
        payer.getGame().getGameLog().add(GameLogEntryType.INFORMATION,
                payer + " opens a " + pd.type + " booster pack and puts its " + opened.size()
                        + " cards on the bottom of their booster pile.");
        return true;
    }

    @Override
    public final String toString() {
        return "Open a sealed Magic booster pack and put the cards on the bottom of your booster pile in a random order";
    }

    @Override
    public <T> T accept(final ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
