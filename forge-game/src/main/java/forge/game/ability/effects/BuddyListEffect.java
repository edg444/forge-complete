package forge.game.ability.effects;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import forge.game.GameLogEntryType;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Champion of the Hareish: write one of the Defined creature's creature types that isn't on your buddy list yet onto
 * it, if it has one. The list starts empty each game and nothing is ever erased from it (rulings).
 */
public class BuddyListEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getParamOrDefault("SpellDescription", "");
    }

    @SuppressWarnings("unchecked")
    public static Set<String> buddyListOf(final Player p) {
        Object list = p.getGameNote("BuddyList");
        if (!(list instanceof Set<?>)) {
            list = new LinkedHashSet<String>();
            p.setGameNote("BuddyList", list);
        }
        return (Set<String>) list;
    }

    public static boolean onBuddyList(final Player p, final Card c) {
        final Set<String> list = buddyListOf(p);
        return c.getType().getCreatureTypes().stream().anyMatch(list::contains);
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Player p = sa.getActivatingPlayer();
        final Set<String> list = buddyListOf(p);
        for (final Card c : AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParamOrDefault("Defined", "Self"), sa)) {
            final List<String> unwritten = c.getType().getCreatureTypes().stream().filter(t -> !list.contains(t)).toList();
            if (unwritten.isEmpty()) {
                continue;
            }
            final String type = unwritten.size() == 1 ? unwritten.get(0)
                    : p.getController().chooseSomeType("Creature", sa, unwritten, false);
            if (type == null) {
                continue;
            }
            list.add(type);
            p.getGame().getGameLog().add(GameLogEntryType.INFORMATION, p + " writes " + type + " on their buddy list.");
        }
    }
}
