package forge.game;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import forge.game.ability.AbilityKey;
import forge.game.player.Player;
import forge.game.replacement.ReplacementType;
import forge.game.trigger.TriggerType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Represents the planar dice for Planechase games.
 *
 */
public enum PlanarDice {
    Planeswalk,
    Chaos,
    Blank;

    /** One planar die's face - with Krark's Other Thumb, the kept one of two (any die you roll, rulings). */
    private static PlanarDice rollFace(final Player roller, final int thumbs) {
        if (thumbs <= 0) {
            final int i = forge.util.MyRandom.getRandom().nextInt(6);
            return i == 0 ? Planeswalk : i == 1 ? Chaos : Blank;
        }
        final PlanarDice first = rollFace(roller, thumbs - 1);
        final PlanarDice second = rollFace(roller, thumbs - 1);
        final PlanarDice ignored = roller.getController().choosePDRollToIgnore(Lists.newArrayList(first, second));
        return ignored == first ? second : first;
    }

    public static PlanarDice roll(Player roller, PlanarDice riggedResult) {
        final Game game = roller.getGame();
        int rolls = 1;
        int ignore = 0;

        final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(roller);
        repParams.put(AbilityKey.Number, rolls);
        repParams.put(AbilityKey.Ignore, ignore);

        switch (game.getReplacementHandler().run(ReplacementType.RollPlanarDice, repParams)) {
            case NotReplaced:
                break;
            case Updated: {
                rolls = (int) repParams.get(AbilityKey.Number);
                ignore = (int) repParams.get(AbilityKey.Ignore);
                break;
            }
        }

        List<PlanarDice> results = Lists.newArrayList();
        final int thumbs = forge.game.ability.effects.RollDiceEffect.rollTwoKeepOneCount(roller);
        for (int r = 0; r < rolls; r++) {
            PlanarDice thisRoll = rollFace(roller, thumbs);
            roller.roll();
            // Wall of Fortune can have the planar die rerolled too (Unstable ruling)
            while (riggedResult == null && forge.game.ability.effects.RollDiceEffect.tapWallToReroll(roller, -1, 6,
                    thisRoll + " on the planar die")) {
                thisRoll = rollFace(roller, thumbs);
                roller.roll();
            }
            if (riggedResult != null)
                thisRoll = riggedResult;
            results.add(thisRoll);
        }

        for (int ig = 0; ig < ignore; ig++) {
            results.remove(roller.getController().choosePDRollToIgnore(results));
        }
        PlanarDice res = results.get(0);

        final Map<AbilityKey, Object> resRepParams = AbilityKey.mapFromAffected(roller);
        resRepParams.put(AbilityKey.Result, res);

        switch (game.getReplacementHandler().run(ReplacementType.PlanarDiceResult, resRepParams)) {
            case NotReplaced:
                break;
            case Updated: {
                res = (PlanarDice) resRepParams.get(AbilityKey.Result);
                break;
            }
        }

        Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(roller);
        runParams.put(AbilityKey.Result, res);
        game.getTriggerHandler().runTrigger(TriggerType.PlanarDice, runParams, false);

        // Also run normal RolledDie and RolledDieOnce triggers
        for (int r = 0; r < rolls; r++) {
            runParams = AbilityKey.mapFromPlayer(roller);
            runParams.put(AbilityKey.Sides, 6);
            runParams.put(AbilityKey.Result, 0);
            roller.getGame().getTriggerHandler().runTrigger(TriggerType.RolledDie, runParams, false);
        }

        runParams = AbilityKey.mapFromPlayer(roller);
        runParams.put(AbilityKey.Sides, 6);
        runParams.put(AbilityKey.Result, Arrays.asList(0));
        roller.getGame().getTriggerHandler().runTrigger(TriggerType.RolledDieOnce, runParams, false);

        if (res == Chaos) {
            runParams = AbilityKey.mapFromPlayer(roller);
            roller.getGame().getTriggerHandler().runTrigger(TriggerType.ChaosEnsues, runParams, false);
        }

        return res;
    }

    /**
     * Parses a string into an enum member.
     * @param string to parse
     * @return enum equivalent
     */
    public static PlanarDice smartValueOf(String value) {
        final String valToCompate = value.trim();
        for (final PlanarDice v : PlanarDice.values()) {
            if (v.name().compareToIgnoreCase(valToCompate) == 0) {
                return v;
            }
        }

        throw new RuntimeException("Element " + value + " not found in PlanarDice enum");
    }

    public static final ImmutableList<PlanarDice> values = ImmutableList.copyOf(values());
}
