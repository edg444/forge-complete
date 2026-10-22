package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.List;

import forge.card.CardType;
import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.ApiType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * ______: "its name forever becomes a creature type and a noncreature card type of your choice belonging to your first
 * name" - "Gavin's Dragon and Sorceries". The name is written on the card in its owner's deck, like a Legacy word.
 * Its first name is the first word of its player's name.
 */
public class WriteNameEffect extends SpellAbilityEffect {

    private static final List<String> NONCREATURE_TYPES = List.of("Artifact", "Battle", "Enchantment", "Instant",
            "Kindred", "Land", "Planeswalker", "Sorcery");

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Fill in " + sa.getHostCard() + "'s name.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player p = sa.getActivatingPlayer();
        final Game game = p.getGame();
        if (host.getWrittenName() != null) {
            return;
        }
        final SpellAbility choose = new SpellAbility.EmptySa(ApiType.ChooseType, host, p);
        final String creatureType;
        final String cardType;
        if (p.getController().isAI()) {
            final java.util.Iterator<String> most = CardFactoryUtil.getMostProminentCreatureType(p.getAllCards()).iterator();
            creatureType = most.hasNext() ? most.next() : "Human";
            String best = "Instant";
            int count = -1;
            for (final String t : NONCREATURE_TYPES) {
                final int n = CardLists.count(p.getAllCards(), c -> c.getType().hasStringType(t) && !c.isCreature());
                if (n > count) {
                    count = n;
                    best = t;
                }
            }
            cardType = best;
        } else {
            creatureType = p.getController().chooseSomeType("Creature", choose, new ArrayList<>(CardType.getAllCreatureTypes()));
            cardType = p.getController().chooseSomeType("Card", choose, NONCREATURE_TYPES);
        }
        if (creatureType == null || cardType == null) {
            return;
        }
        final String first = p.getName().trim().split("\\s+")[0];
        final String plural = cardType.endsWith("y") ? cardType.substring(0, cardType.length() - 1) + "ies" : cardType + "s";
        final String name = first + "'s " + creatureType + " and " + plural;
        host.setWrittenName(name);
        game.getMatch().writeName(host, name);
        game.getGameLog().add(GameLogEntryType.INFORMATION, p + " fills in the name: " + name + ".");
    }
}
