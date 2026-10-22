package forge.game.ability.effects;

import java.util.List;

import forge.StaticData;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.item.PaperCard;
import forge.util.Aggregates;

/**
 * Unstickerify: which real card is under a stickered playtest card's sticker. A person names it from card-name search
 * the first time it matters, or says it's unclear without peeling (a Wastes); the AI's copies each have a random real
 * card underneath (user ruling 2026-10-22). Kept for that copy for the match. The host's chosen name is set to it, for
 * a following Clone CopyFromChosenName$.
 */
public class StickerBeneathEffect extends SpellAbilityEffect {

    public static final String UNCLEAR = "Wastes";

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Treat " + getDefinedCardsOrTargeted(sa) + " as the card beneath its sticker.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        for (final Card c : getDefinedCardsOrTargeted(sa)) {
            host.addNamedCard(beneath(c, sa));
        }
    }

    public static String beneath(final Card c, final SpellAbility sa) {
        final Player owner = c.getOwner();
        final String printed = c.getPaperCard() != null ? c.getPaperCard().getName() : c.getName();
        // the physical copy: its place among the owner's cards of that name, which keep their ids all game
        final List<Card> same = CardLists.filter(owner.getAllCards(), x -> x.getPaperCard() != null && printed.equals(x.getPaperCard().getName()));
        same.sort(java.util.Comparator.comparingInt(Card::getId));
        final String key = "StickerBeneath:" + printed + "#" + (same.indexOf(c) + 1);
        if (owner.getMatchNote(key) instanceof String noted) {
            return noted;
        }
        String name;
        if (owner.getController().isAI()) {
            final PaperCard random = Aggregates.random(StaticData.instance().getCommonCards().getUniqueCards());
            name = random == null ? UNCLEAR : random.getName();
        } else if (owner.getController().confirmAction(sa, null, "Can you tell which card is under " + c.getTranslatedName()
                + "'s sticker without peeling it?", null)) {
            name = owner.getController().chooseCardName(sa, f -> true, "Card", "Name the card under " + c.getTranslatedName() + "'s sticker");
            if (name == null || name.isEmpty()) {
                name = UNCLEAR;
            }
        } else {
            name = UNCLEAR;
        }
        owner.setMatchNote(key, name);
        return name;
    }
}
