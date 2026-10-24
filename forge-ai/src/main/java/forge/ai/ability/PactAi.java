package forge.ai.ability;

import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;

/**
 * Pacts (Pact of Negation, Slaughter Pact, Summoner's Pact...): "At the beginning of your next upkeep, pay {cost}. If
 * you don't, you lose the game." The AI pays that when it can, so a pact is only cast when its lands - all untapped by
 * then - can pay it in full and in color, on top of any pact already waiting.
 */
public final class PactAi {

    private PactAi() { }

    private static String pactCost(final SpellAbility sa) {
        for (SpellAbility sub = sa; sub != null; sub = sub.getSubAbility()) {
            if (sub.getApi() == ApiType.DelayedTrigger && sub.hasParam("Execute")) {
                final java.util.Map<String, String> exec = forge.util.FileSection.parseToMap(sub.getSVar(sub.getParam("Execute")),
                        forge.util.FileSection.DOLLAR_SIGN_KV_SEPARATOR);
                if ("LosesGame".equals(exec.get("DB")) && exec.containsKey("UnlessCost")) {
                    return exec.get("UnlessCost");
                }
            }
        }
        return null;
    }

    /** False when casting sa would leave the AI unable to pay for its pacts next upkeep. */
    public static boolean canPayLater(final Player ai, final SpellAbility sa) {
        final String cost = pactCost(sa);
        if (cost == null) {
            return true;
        }
        int generic = 0;
        final int[] colored = new int[MagicColor.WUBRG.length];
        final java.util.List<String> owed = new java.util.ArrayList<>();
        owed.add(cost);
        for (final Trigger t : ai.getGame().getTriggerHandler().getDelayedTriggers()) {
            final SpellAbility exec = t.getOverridingAbility();
            if (exec != null && exec.getApi() == ApiType.LosesGame && exec.hasParam("UnlessCost")
                    && t.getHostCard() != null && ai.equals(t.getHostCard().getController())) {
                owed.add(exec.getParam("UnlessCost"));
            }
        }
        for (final String s : owed) {
            final ManaCost mc = new ManaCost(s);
            generic += mc.getGenericCost();
            for (final ManaCostShard shard : mc) {
                for (int i = 0; i < MagicColor.WUBRG.length; i++) {
                    if (shard.isColor(MagicColor.WUBRG[i])) {
                        colored[i]++;
                    }
                }
            }
        }
        final CardCollectionView lands = ai.getLandsInPlay();
        int total = generic;
        for (int i = 0; i < colored.length; i++) {
            total += colored[i];
            final String c = MagicColor.toShortString(MagicColor.WUBRG[i]);
            final int need = colored[i];
            if (need > 0 && lands.stream().filter(l -> produces(l, c)).count() < need) {
                return false;
            }
        }
        return lands.size() >= total;
    }

    private static boolean produces(final Card land, final String color) {
        return land.getManaAbilities().anyMatch(m -> m.canProduce(color));
    }
}
