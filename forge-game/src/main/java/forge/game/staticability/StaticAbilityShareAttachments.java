package forge.game.staticability;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.zone.ZoneType;

/**
 * Solaflora, Intergalactic Icon: Auras and Equipment (ValidAttachment$) attached to the host affect other creatures
 * (Affected$) as though attached to them. They aren't enchanted or equipped (rulings), so only the "enchanted/equipped
 * by this" checks read this, not "is enchanted" or "is equipped".
 */
public class StaticAbilityShareAttachments {

    public static boolean affectsAsAttached(final Card card, final Card attachment) {
        if (attachment == null || !(attachment.getEntityAttachedTo() instanceof Card)) {
            return false;
        }
        final Card host = (Card) attachment.getEntityAttachedTo();
        if (host.equals(card)) {
            return false;
        }
        for (final StaticAbility stAb : host.getStaticAbilities()) {
            if (stAb.checkConditions(StaticAbilityMode.ShareAttachments)
                    && stAb.matchesValidParam("ValidAttachment", attachment)
                    && stAb.matchesValidParam("Affected", card)) {
                return true;
            }
        }
        return false;
    }

    /** The other creatures an attachment affects as though attached to them (for Defined$ Enchanted/Equipped). */
    public static CardCollection sharers(final Card attachment) {
        final CardCollection result = new CardCollection();
        if (attachment == null || !(attachment.getEntityAttachedTo() instanceof Card) || attachment.getGame() == null) {
            return result;
        }
        final Card host = (Card) attachment.getEntityAttachedTo();
        boolean shares = false;
        for (final StaticAbility stAb : host.getStaticAbilities()) {
            shares |= stAb.checkConditions(StaticAbilityMode.ShareAttachments);
        }
        if (!shares) {
            return result;
        }
        for (final Card c : attachment.getGame().getCardsIn(ZoneType.Battlefield)) {
            if (affectsAsAttached(c, attachment)) {
                result.add(c);
            }
        }
        return result;
    }
}
