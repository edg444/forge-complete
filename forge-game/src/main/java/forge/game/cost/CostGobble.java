package forge.game.cost;

import java.util.Map;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;

/**
 * Gobble N (Pie-Eating Contest): each time, take a bite of food or sacrifice a Food token. The payer chooses which
 * Food tokens to sacrifice, all at once (rulings), and each gobble left over is a bite they take - honor system, so a
 * gobble can always be paid. Paid as a sacrifice of the chosen tokens, which may be none.
 */
public class CostGobble extends CostPartWithList {
    private static final long serialVersionUID = 1L;

    public CostGobble(final String amount) {
        super(amount, "Food.token", "Food token");
    }

    @Override
    public int paymentOrder() {
        return 15;
    }

    @Override
    public Integer getMaxAmountX(final SpellAbility ability, final Player payer, final boolean effect) {
        // a bite can always be taken; X is only ever as large as what it's spent on
        return payer.getCardsIn(ZoneType.Battlefield).size() + 20;
    }

    @Override
    public final String toString() {
        return "Gobble " + getAmount();
    }

    @Override
    public final boolean canPay(final SpellAbility ability, final Player payer, final boolean effect) {
        return true;
    }

    /** The Food tokens the payer could sacrifice for gobbles. */
    public CardCollectionView foods(final SpellAbility ability, final Player payer, final boolean effect) {
        final CardCollectionView foods = CardLists.getValidCards(payer.getCardsIn(ZoneType.Battlefield), getType(),
                payer, ability.getHostCard(), ability);
        return CardLists.filter(foods, CardPredicates.canBeSacrificedBy(ability, effect));
    }

    /** The prompt for the gobbles left over after the Food tokens. */
    public static String bitesPrompt(final int bites) {
        return "Take " + Lang.nounWithNumeralExceptOne(bites, "bite") + " of food, one at a time (gum isn't food)";
    }

    @Override
    protected Card doPayment(final Player payer, final SpellAbility ability, final Card targetCard, final boolean effect) {
        return null;
    }

    @Override
    protected boolean canPayListAtOnce() {
        return true;
    }

    @Override
    protected CardCollectionView doListPayment(final Player payer, final SpellAbility ability, final CardCollectionView targetCards,
            final boolean effect) {
        final Game game = ability.getHostCard().getGame();
        final Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
        AbilityKey.addCardZoneTableParams(moveParams, table);
        return game.getAction().sacrifice(targetCards, ability, effect, moveParams);
    }

    @Override
    public String getHashForLKIList() {
        return "Sacrificed";
    }

    @Override
    public String getHashForCardList() {
        return "SacrificedCards";
    }

    @Override
    public <T> T accept(final ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
