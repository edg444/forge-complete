/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.game.mana;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCostShard;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.cost.CostPayment;
import forge.game.event.EventValueChangeType;
import forge.game.event.GameEventManaPool;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.replacement.ReplacementLayer;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityUnspentMana;

import java.util.*;

/**
 * <p>
 * ManaPool class.
 * </p>
 *
 * @author Forge
 * @version $Id$
 */
public class ManaPool extends ManaConversionMatrix implements Iterable<Mana> {
    private final Player owner;
    private final ArrayListMultimap<Byte, Mana> floatingMana = ArrayListMultimap.create();

    public ManaPool(final Player player) {
        owner = player;
        restoreColorReplacements();
    }

    public final int getAmountOfColor(final byte color) {
        Collection<Mana> ofColor = floatingMana.get(color);
        return ofColor == null ? 0 : ofColor.size();
    }

    // Unhinged half mana. A half reaches the pool either as change from paying a whole mana into a
    // half cost (Cheap Ass reducing {2} to {1 1/2}) or produced outright by Mons's Goblin Waiters,
    // and it stays there so it can pay a half cost later, e.g. Little Girl's {HW}. Two halves of a
    // colour are folded into a whole mana as they meet, so they aren't stranded - see
    // AbilityManaPart.produceMana.
    private final Map<Byte, Integer> floatingHalves = Maps.newHashMap();

    // Mox Lotus adds {inf}. Rather than a very large number that can still be exhausted, the pool
    // remembers that its colourless is unbounded: spending some puts it straight back. It empties
    // with everything else at end of step or phase.
    private boolean infiniteColorless;
    private Card infiniteSource;
    private AbilityManaPart infiniteManaAbility;
    private Mana cachedInfiniteMana;

    public final int getHalfMana(final byte color) {
        return floatingHalves.getOrDefault(color, 0);
    }
    public final boolean hasHalfMana() {
        return floatingHalves.values().stream().anyMatch(i -> i > 0);
    }
    /** Total floating halves across all colours, for mana burn. */
    public final int totalHalfMana() {
        return floatingHalves.values().stream().mapToInt(Integer::intValue).sum();
    }
    public final void addHalfMana(final byte color) {
        floatingHalves.merge(color, 1, Integer::sum);
        halfManaChanged(color, EventValueChangeType.Added);
    }

    // updateManaForView alone only refreshes the tracked value - the pool display repaints on the
    // GameEventManaPool event, so without this a lone floating half stayed invisible until a second
    // one merged into a whole mana and addMana fired the event for it
    private void halfManaChanged(final byte color, final EventValueChangeType change) {
        owner.updateManaForView();
        owner.getGame().fireEvent(new GameEventManaPool(owner, change,
                EnumSet.of(MagicColor.Color.fromByte(color))));
    }

    /**
     * Spend a floating half of a colour this cost accepts.
     * @param colorMask colours that may pay, 0xFF for any
     * @return true if a half was found and spent
     */
    public final boolean payHalfMana(final byte colorMask) {
        for (Map.Entry<Byte, Integer> e : floatingHalves.entrySet()) {
            if (e.getValue() > 0 && (e.getKey() == 0 || (e.getKey() & colorMask) != 0)) {
                e.setValue(e.getValue() - 1);
                halfManaChanged(e.getKey(), EventValueChangeType.Removed);
                return true;
            }
        }
        return false;
    }

    /**
     * Spend a floating half of exactly this colour, unlike {@link #payHalfMana} which also accepts a
     * colourless half against any mask.
     */
    public final boolean payHalfManaExact(final byte color) {
        final int n = floatingHalves.getOrDefault(color, 0);
        if (n <= 0) {
            return false;
        }
        floatingHalves.put(color, n - 1);
        halfManaChanged(color, EventValueChangeType.Removed);
        return true;
    }

    public final void clearHalfMana() {
        if (!floatingHalves.isEmpty()) {
            floatingHalves.clear();
            owner.updateManaForView();
        }
    }

    public void addManaNoEvent(final Mana mana) {
        floatingMana.put(mana.getColor(), mana);
    }

    public final void addMana(final Mana... manaList) {
        addMana(Arrays.asList(manaList));
    }
    public final void addMana(final Iterable<Mana> manaList) {
        Set<MagicColor.Color> colors = EnumSet.noneOf(MagicColor.Color.class);
        for (final Mana m : manaList) {
            floatingMana.put(m.getColor(), m);
            colors.add(MagicColor.Color.fromByte(m.getColor()));
        }
        if (!colors.isEmpty()) {
            owner.updateManaForView();
            owner.getGame().fireEvent(new GameEventManaPool(owner, EventValueChangeType.Added, colors));
        }
    }

    /**
     * <p>
     * willManaBeLostAtEndOfPhase.
     *
     * @return - whether floating mana will be lost if the current phase ended right now
     * </p>
     */
    public final boolean willManaBeLostAtEndOfPhase() {
        if (floatingMana.isEmpty() && !hasHalfMana()) {
            return false;
        }

        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromAffected(owner);
        if (!owner.getGame().getReplacementHandler().getReplacementList(ReplacementType.LoseMana, runParams, ReplacementLayer.Other).isEmpty()) {
            return false;
        }

        // no static ability keeps a half, so one floating is always about to be lost
        if (hasHalfMana()) {
            return true;
        }

        int safeMana = 0;
        for (final byte c : StaticAbilityUnspentMana.getManaToKeep(owner)) {
            safeMana += getAmountOfColor(c);
        }

        // TODO isPersistentMana

        return totalMana() != safeMana; //won't lose floating mana if all mana is of colors that aren't going to be emptied
    }

    public final boolean hasBurn() {
        final Game game = owner.getGame();
        return game.getRules().hasManaBurn() || StaticAbilityUnspentMana.hasManaBurn(owner);
    }

    public final void resetPool() {
        // This should only be used to reset the pool to empty by things like restores.
        floatingMana.clear();
        floatingHalves.clear();
    }

    public final List<Mana> clearPool(boolean isEndOfPhase) {
        // isEndOfPhase parameter: true = end of phase, false = mana drain effect
        List<Mana> cleared = Lists.newArrayList();
        // a floating half empties with the rest of the pool, and does so before the early return
        // below since it can outlive the last whole mana
        final boolean clearedAHalf = hasHalfMana();
        clearHalfMana();
        infiniteColorless = false;
        infiniteSource = null;
        infiniteManaAbility = null;
        cachedInfiniteMana = null;
        if (floatingMana.isEmpty()) {
            // the pool event below is what actually repaints the UI, so a half that emptied on its
            // own still has to fire it or the stale value lingers until the pool next changes
            if (clearedAHalf) {
                owner.getGame().fireEvent(new GameEventManaPool(owner, EventValueChangeType.Cleared, null));
            }
            return cleared;
        }

        Byte convertTo = null;

        // TODO move this lower in case all mana would be persistent
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromAffected(owner);
        runParams.put(AbilityKey.Mana, "C");
        switch (owner.getGame().getReplacementHandler().run(ReplacementType.LoseMana, runParams)) {
        case NotReplaced:
            break;
        case Skipped:
            return cleared;
        default:
            convertTo = ManaAtom.fromName((String) runParams.get(AbilityKey.Mana));
            break;

        }

        final List<Byte> keys = Lists.newArrayList(floatingMana.keySet());
        if (isEndOfPhase) {
            keys.removeAll(StaticAbilityUnspentMana.getManaToKeep(owner));
        }
        if (convertTo != null) {
            keys.remove(convertTo);
        }

        for (Byte b : keys) {
            Collection<Mana> cm = floatingMana.get(b);
            final List<Mana> pMana = Lists.newArrayList();
            if (isEndOfPhase && !owner.getGame().getPhaseHandler().is(PhaseType.CLEANUP)) {
                for (final Mana mana : cm) {
                    if (mana.isPersistentMana()) {
                        pMana.add(mana);
                    }
                    if (mana.isCombatMana() && !owner.getGame().getPhaseHandler().is(PhaseType.COMBAT_END)) {
                        pMana.add(mana);
                    }
                }
            }
            cm.removeAll(pMana);
            if (convertTo != null) {
                convertManaColor(b, convertTo);
                cm.addAll(pMana);
            } else {
                cleared.addAll(cm);
                cm.clear();
                floatingMana.putAll(b, pMana);
            }
        }

        owner.updateManaForView();
        owner.getGame().fireEvent(new GameEventManaPool(owner, EventValueChangeType.Cleared, null));
        return cleared;
    }

    private void convertManaColor(final byte originalColor, final byte toColor) {
        List<Mana> convert = Lists.newArrayList();
        Collection<Mana> cm = floatingMana.get(originalColor);
        for (Mana m : cm) {
            convert.add(m.convertColor(toColor));
        }
        cm.clear();
        floatingMana.putAll(toColor, convert);
        owner.updateManaForView();
    }

    public final boolean hasInfiniteColorless() {
        return infiniteColorless;
    }
    public final void addInfiniteColorless(final Card source, final AbilityManaPart manaAbility) {
        // Mana insists on a real producer - it takes an LKI copy of the source card in its
        // constructor, and isSnow() reads it later - so the refills have to remember one too.
        infiniteColorless = true;
        infiniteSource = source;
        infiniteManaAbility = manaAbility;
        // one real mana so the pool is non-empty and the usual "can you pay" checks find something
        addMana(newInfiniteMana());
    }
    private Mana newInfiniteMana() {
        // Mana's constructor takes an LKI copy of the source card, so building these one at a time in
        // a payment loop is ruinously expensive - one shared instance is enough, since every mana
        // this pool hands back is identical by construction.
        if (cachedInfiniteMana == null) {
            cachedInfiniteMana = new Mana((byte) ManaAtom.COLORLESS, infiniteSource, infiniteManaAbility, owner);
        }
        return cachedInfiniteMana;
    }
    /** Puts back colorless spent while the pool is unbounded, so it never actually runs down. */
    private void refillInfinite(final Iterable<Mana> spent) {
        if (!infiniteColorless || infiniteSource == null) {
            return;
        }
        for (final Mana m : spent) {
            if (m.getColor() == (byte) ManaAtom.COLORLESS) {
                floatingMana.put(m.getColor(), newInfiniteMana());
            }
        }
    }

    public boolean removeManaNoEvent(final Mana mana) {
        final boolean removed = floatingMana.remove(mana.getColor(), mana);
        refillInfinite(Lists.newArrayList(mana));
        return removed;
    }

    public boolean removeMana(Mana... manaList) {
        return removeMana(Arrays.asList(manaList));
    }
    public boolean removeMana(final Iterable<Mana> manaList) {
        Set<MagicColor.Color> colors = EnumSet.noneOf(MagicColor.Color.class);
        for (Mana m : manaList) {
            if (floatingMana.remove(m.getColor(), m)) {
                colors.add(MagicColor.Color.fromByte(m.getColor()));
            }
        }
        refillInfinite(manaList);
        if (!colors.isEmpty()) {
            owner.updateManaForView();
            owner.getGame().fireEvent(new GameEventManaPool(owner, EventValueChangeType.Removed, colors));
        }
        return !colors.isEmpty();
    }

    public final void payManaFromAbility(final SpellAbility saPaidFor, ManaCostBeingPaid manaCost, final SpellAbility saPayment) {
        // Mana restriction must be checked before this method is called
        final List<SpellAbility> paidAbs = saPaidFor.getPayingManaAbilities();

        paidAbs.add(saPayment); // assumes some part on the mana produced by the ability will get used

        // need to get all mana from all ManaAbilities of the SpellAbility
        for (AbilityManaPart mp : saPayment.getAllManaParts()) {
            for (final Mana mana : mp.getLastManaProduced()) {
                if (!saPaidFor.allowsPayingWithShard(mp.getSourceCard(), mana.getColor())) {
                    continue;
                }
                if (tryPayCostWithMana(saPaidFor, manaCost, mana, false)) {
                    saPaidFor.getPayingMana().add(mana);
                }
            }
        }
    }

    public boolean tryPayCostWithColor(byte colorCode, SpellAbility saPaidFor, ManaCostBeingPaid manaCost, List<Mana> manaSpentToPay) {
        Mana manaFound = null;
        Collection<Mana> cm = floatingMana.get(colorCode);

        for (final Mana mana : cm) {
            if (!mana.meetsManaRestrictions(saPaidFor)) {
                continue;
            }

            if (!saPaidFor.allowsPayingWithShard(mana.getSourceCard(), colorCode)) {
                continue;
            }

            manaFound = mana;
            break;
        }

        if (manaFound != null && tryPayCostWithMana(saPaidFor, manaCost, manaFound, false)) {
            manaSpentToPay.add(manaFound);
            return true;
        }
        return false;
    }

    public boolean tryPayCostWithMana(final SpellAbility sa, ManaCostBeingPaid manaCost, final Mana mana, boolean test) {
        if (!manaCost.isNeeded(mana, this)) {
            return false;
        }
        // only pay mana into manaCost when the Mana could be removed from the Mana pool
        // if the mana wasn't in the mana pool then something is wrong
        if (!removeMana(mana)) {
            return false;
        }
        manaCost.payMana(mana, this, test);

        return true;
    }

    public final boolean isEmpty() {
        return floatingMana.isEmpty();
    }

    public final int totalMana() {
        return floatingMana.values().size();
    }

    //Account for mana part of ability when undoing it
    public boolean accountFor(final AbilityManaPart ma) {
        if (ma == null) {
            return false;
        }
        if (floatingMana.isEmpty()) {
            return false;
        }

        final List<Mana> removeFloating = Lists.newArrayList();

        boolean manaNotAccountedFor = false;
        // loop over mana produced by mana ability
        for (Mana mana : ma.getLastManaProduced()) {
            Collection<Mana> poolLane = floatingMana.get(mana.getColor());

            if (poolLane != null && poolLane.contains(mana)) {
                removeFloating.add(mana);
            } else {
                manaNotAccountedFor = true;
                break;
            }
        }

        // When is it legitimate for all the mana not to be accountable?
        // TODO: Does this condition really indicate an bug in Forge?
        if (manaNotAccountedFor) {
            return false;
        }

        removeMana(removeFloating);
        return true;
    }

    public void refundMana(List<Mana> manaSpent) {
        addMana(manaSpent);
        manaSpent.clear();
    }

    public boolean canPayForShardWithColor(ManaCostShard shard, byte color) {
        if (shard.isOfKind(ManaAtom.COLORLESS) && color == ManaAtom.GENERIC) {
            return false; // FIXME: testing Colorless against Generic is a recipe for disaster, but probably there should be a better fix.
        }

        byte line = getPossibleColorUses(color);

        for (byte outColor : ManaAtom.MANATYPES) {
            if ((line & outColor) != 0 && shard.canBePaidWithManaOfColor(outColor)) {
                return true;
            }
        }

        return shard.canBePaidWithManaOfColor((byte)0);
    }

    /**
     * Checks if the given mana cost can be paid from floating mana.
     * @param cost mana cost to pay for
     * @param sa ability to pay for
     * @param test actual payment is made if this is false
     * @param manaSpentToPay list of mana spent
     * @return whether the floating mana is sufficient to pay the cost fully
     */
    public boolean payManaCostFromPool(final ManaCostBeingPaid cost, final SpellAbility sa, final boolean test, List<Mana> manaSpentToPay) {
        final boolean hasConverge = sa.getHostCard().hasConverge();

        // An unbounded pool settles the whole generic portion at once. getUnpaidShards() below
        // expands generic into one list entry per point, so Gleemax's {1000000} would otherwise build
        // and sort a million-element list and take a million trips through the payment loop.
        // must run on the test pass too - that is the affordability check, and it walks the same list
        if (infiniteColorless && cost.getGenericManaAmount() > 0) {
            cost.decreaseGenericMana(cost.getGenericManaAmount());
            if (!test) {
                manaSpentToPay.add(newInfiniteMana());
            }
        }

        List<ManaCostShard> unpaidShards = cost.getUnpaidShards();
        Collections.sort(unpaidShards); // most difficult shards must come first
        for (ManaCostShard part : unpaidShards) {
            if (part != ManaCostShard.X) {
                if (cost.isPaid()) {
                    continue;
                }

                // get a mana of this type from floating, bail if none available
                final Mana mana = CostPayment.getMana(owner, part, sa, hasConverge ? cost.getColorsPaid() : -1, cost.getXManaCostPaidByColor());
                if (mana != null && tryPayCostWithMana(sa, cost, mana, test)) {
                    manaSpentToPay.add(mana);
                }
            }
        }

        if (cost.isPaid()) {
            // refund any mana taken from mana pool when test
            if (test) {
                refundMana(manaSpentToPay);
            }
            return true;
        }
        return false;
    }

    @Override
    public Iterator<Mana> iterator() {
        return floatingMana.values().iterator();
    }

}
