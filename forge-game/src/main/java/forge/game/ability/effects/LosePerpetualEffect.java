package forge.game.ability.effects;

import com.google.common.collect.Lists;
import com.google.common.collect.Table.Cell;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.ICardTraitChanges;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;

public class LosePerpetualEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        // Defaults to the ability's own host card, same as before. But when the ability-bearing
        // permanent also changes zones as part of the same resolution (e.g. a dies-trigger that
        // returns itself to the battlefield then loses the ability that just triggered), sa.getHostCard()
        // is a stale reference to the object that's dying, not the new object taking its place -
        // stripping the grant there doesn't stop it from being freshly re-applied onto the new object
        // via Card.setPerpetual(oldCard). Defined$ lets the script target the correct live object instead.
        final Card host = sa.hasParam("Defined")
                ? Lists.newArrayList(AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa)).stream().findFirst().orElse(sa.getHostCard())
                : sa.getHostCard();
        long toRemove = (long) 0;
        // currently only part of perpetual triggers... expand in future as needed
        if (sa.getTrigger() != null) {
            Trigger trig = sa.getTrigger();
            for (Cell<Long, Long, ICardTraitChanges> cell : host.getChangedCardTraits().cellSet()) {
                if (cell.getValue().applyTrigger(Lists.newArrayList()).contains(trig)) {
                    toRemove = cell.getRowKey();
                    break;
                }
            }
            if (toRemove == (long) 0 && sa.hasParam("Defined")) {
                // Reference match failed: when the ability-bearing permanent itself changed zones as part
                // of this same resolution (Defined$ points at a new object), Card.setPerpetual(oldCard)
                // already rebuilt a fresh Trigger instance for that object - never == the instance that
                // originally fired. Fall back to matching by the trigger's own script identity (its
                // Execute SVar name is stable across that rebuild, unlike object identity).
                String execName = trig.getParam("Execute");
                for (Cell<Long, Long, ICardTraitChanges> cell : host.getChangedCardTraits().cellSet()) {
                    for (Trigger t : cell.getValue().applyTrigger(Lists.newArrayList())) {
                        if (execName != null && execName.equals(t.getParam("Execute"))) {
                            toRemove = cell.getRowKey();
                            break;
                        }
                    }
                    if (toRemove != (long) 0) {
                        break;
                    }
                }
            }
            if (toRemove != (long) 0) {
                host.getChangedCardTraits().remove(toRemove, (long) 0);
                host.removePerpetual(toRemove);
                // Mutating changedCardTraits directly (rather than through Card.addChangedCardTraits'
                // updateView-flagged path, which is what keeps the grant side in sync) skips the view
                // refresh entirely - the tooltip keeps showing the just-removed ability until this runs.
                host.updateAbilityTextForView();
                host.getGame().fireEvent(new GameEventCardStatsChanged(host));
            }
        }
    }
}
