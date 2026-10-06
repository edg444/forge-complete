package forge.game.ability.effects;

import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

/**
 * Ol' Buzzbark: where dice rolled onto the battlefield come to rest. Forge has no table, so like
 * FlipOntoBattlefieldEffect the dice are aimed at a creature and land around it in its controller's row of
 * creatures (battlefield order). The higher the drop, the more they scatter and the less often one stays on a card.
 * A die can come to rest on the edge between two neighbors and touch both.
 */
public final class DiceLanding {
    private DiceLanding() { }

    /** Chance a die comes to rest touching a card at all, from a height of so many inches. */
    static double touchChance(final int height) {
        return Math.max(0.25, Math.min(0.70, 0.70 - 0.03 * height));
    }

    /** Spread around the aimed creature, in card widths. */
    static double scatter(final int height) {
        return 0.35 + 0.12 * height;
    }

    private static final double EDGE_CHANCE = 0.15;

    public static List<CardCollection> land(final Card aim, final int dice, final int height) {
        return land(aim, dice, height, MyRandom.getRandom());
    }

    public static List<CardCollection> land(final Card aim, final int dice, final int height, final Random rnd) {
        final List<CardCollection> landings = Lists.newArrayList();
        final List<Card> row = aim == null ? Lists.newArrayList()
                : CardLists.filter(aim.getController().getCardsIn(ZoneType.Battlefield), Card::isCreature);
        final int aimIndex = row.indexOf(aim);
        for (int i = 0; i < dice; i++) {
            final CardCollection touched = new CardCollection();
            landings.add(touched);
            if (aimIndex < 0 || rnd.nextDouble() >= touchChance(height)) {
                continue;
            }
            final double drift = rnd.nextGaussian() * scatter(height);
            final int index = aimIndex + (int) Math.round(drift);
            if (index < 0 || index >= row.size()) {
                continue; // rolled past the end of the row
            }
            touched.add(row.get(index));
            if (rnd.nextDouble() < EDGE_CHANCE) {
                final int neighbor = index + (drift >= Math.round(drift) ? 1 : -1);
                if (neighbor >= 0 && neighbor < row.size()) {
                    touched.add(row.get(neighbor));
                }
            }
        }
        return landings;
    }
}
