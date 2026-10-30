package forge.ai;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.math.NumberUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.PaiGowMagic;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;

/**
 * How the AI splits its Pai Gow booster. The piles come up in a random order against random piles of the opponent's,
 * so it keeps them even: the creatures go one to a pile first, then each card to the weakest pile with room. Mana
 * costs don't matter with unlimited mana, so a big spell is just a big spell, and a land that only makes mana is a
 * blank.
 */
public final class PaiGowAi {
    // what wins a 5-life game from a three-card hand: something to answer theirs, or to hit them with
    private static final Set<ApiType> INTERACTION = EnumSet.of(ApiType.Destroy, ApiType.DestroyAll, ApiType.DealDamage,
            ApiType.DamageAll, ApiType.Sacrifice, ApiType.SacrificeAll, ApiType.Counter, ApiType.ChangeZone,
            ApiType.ChangeZoneAll, ApiType.LoseLife, ApiType.Fight, ApiType.Token, ApiType.GainControl);

    private PaiGowAi() { }

    public static List<List<PaperCard>> makePiles(final Player ai, final List<PaperCard> pack, final List<Integer> sizes) {
        final Map<PaperCard, Integer> value = Maps.newIdentityHashMap();
        final Map<PaperCard, Boolean> creature = Maps.newIdentityHashMap();
        for (final PaperCard pc : pack) {
            final Card c = Card.fromPaperCard(pc, ai);
            value.put(pc, evaluate(c));
            creature.put(pc, c.isCreature());
        }
        final List<PaperCard> byValue = Lists.newArrayList(pack);
        byValue.sort((a, b) -> Integer.compare(value.get(b), value.get(a)));

        final List<List<PaperCard>> piles = Lists.newArrayList();
        final int[] totals = new int[sizes.size()];
        for (int i = 0; i < sizes.size(); i++) {
            piles.add(Lists.newArrayList());
        }
        // one creature to each pile, the best of them to the piles in order
        int pile = 0;
        for (final PaperCard pc : Lists.newArrayList(byValue)) {
            while (pile < sizes.size() && sizes.get(pile) == 0) {
                pile++;
            }
            if (pile >= sizes.size()) {
                break;
            }
            if (creature.get(pc)) {
                piles.get(pile).add(pc);
                totals[pile] += value.get(pc);
                byValue.remove(pc);
                pile++;
            }
        }
        for (final PaperCard pc : byValue) {
            int weakest = -1;
            for (int i = 0; i < sizes.size(); i++) {
                if (piles.get(i).size() < sizes.get(i) && (weakest < 0 || totals[i] < totals[weakest])) {
                    weakest = i;
                }
            }
            if (weakest < 0) {
                break; // the piles are full, and what's left is left out
            }
            piles.get(weakest).add(pc);
            totals[weakest] += value.get(pc);
        }
        return piles;
    }

    /**
     * With no library, a Pai Gow game whose hands are spent can stall for good. The AI takes a draw once its hand is
     * empty and either it has nothing left that could deal damage, or the board has stopped moving: its creatures
     * could all attack last turn, it attacked no one, and no one lost life.
     */
    public static boolean acceptsDraw(final Player ai) {
        final Game game = ai.getGame();
        if (!PaiGowMagic.isPlayed(game.getRules()) || !ai.getCardsIn(ZoneType.Hand).isEmpty()) {
            return false;
        }
        boolean canHurt = false;
        boolean freshCreature = false;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.isCreature() && c.getNetCombatDamage() > 0) {
                canHurt = true;
                freshCreature |= c.hasSickness();
            }
            if (!c.getNonManaAbilities().isEmpty()) {
                canHurt = true;
            }
        }
        if (!canHurt) {
            return true;
        }
        if (freshCreature || !ai.getAttackedPlayersMyLastTurn().isEmpty()) {
            return false;
        }
        for (final Player p : game.getPlayers()) {
            if (p.getLifeLostLastTurn() > 0) {
                return false;
            }
        }
        return true;
    }

    static int evaluate(final Card c) {
        if (c.isCreature()) {
            // the mana value only counts for what it buys, which unlimited mana makes free
            return ComputerUtilCard.evaluateCreature(c, true, false);
        }
        if (c.isPlaneswalker()) {
            return 250 + 10 * NumberUtils.toInt(c.getBaseLoyalty(), 3);
        }
        if (c.isLand()) {
            return c.getNonManaAbilities().isEmpty() ? 10 : 60;
        }
        int value = 120 + 10 * c.getCMC();
        for (final SpellAbility sa : c.getSpellAbilities()) {
            for (SpellAbility part = sa; part != null; part = part.getSubAbility()) {
                if (INTERACTION.contains(part.getApi())) {
                    return value + 80;
                }
            }
        }
        return value;
    }
}
