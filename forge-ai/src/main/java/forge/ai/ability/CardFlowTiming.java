package forge.ai.ability;

import java.util.Collection;

import forge.ai.AiCardMemory;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilMana;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.phase.PhaseHandler;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The fork's timing rules for card selection and cantrips. Kept out of upstream's files so their daily edits merge
 * cleanly.
 *
 * <ul>
 *   <li>A pure scry or surveil (Sigiled Starfish) is pointless when every card it would look at is one an earlier scry
 *   already chose to keep on top and would keep again: with two Starfish, the second one only sees the card the first
 *   one kept.</li>
 *   <li>Flat Monte Carlo discovery must not hand the rollouts a pure selection play, or a cantrip whose other effect
 *   only shrinks attackers (Hydrolash), when the heuristics turned it down. The heuristics hold these for a moment the
 *   rollouts cannot value: right before the AI's own draw, with the creature untapped again for blocking, or the
 *   opponent's attack.</li>
 *   <li>A cantrip held for the opponent's combat is still worth casting on the AI's own turn when it is starved for
 *   cards: nothing else to play, and a land drop or spare mana to use the card it draws.</li>
 * </ul>
 */
public final class CardFlowTiming {
    private CardFlowTiming() {
    }

    /** Every effect is a scry or a surveil: nothing but a look at the top of the library. */
    public static boolean isPureSelection(final SpellAbility sa) {
        if (sa == null) {
            return false;
        }
        for (SpellAbility a = sa.getRootAbility(); a != null; a = a.getSubAbility()) {
            if (a.getApi() != ApiType.Scry && a.getApi() != ApiType.Surveil) {
                return false;
            }
        }
        return true;
    }

    /** The cards a scry or surveil of the AI left on top of its library. */
    public static void rememberKeptOnTop(final Player ai, final Collection<Card> kept) {
        for (final Card c : kept) {
            AiCardMemory.rememberCard(ai, c, AiCardMemory.MemorySet.KEPT_ON_TOP);
        }
    }

    /**
     * A pure selection play that would only see cards an earlier scry kept on top, and that the AI would keep again
     * now. Anything that changed the top of the library (a card bottomed, drawn or shuffled in) shows a new card.
     */
    public static boolean onlySeesKeptCards(final Player ai, final SpellAbility sa) {
        if (!isPureSelection(sa) || sa.usesTargeting()) {
            return false;
        }
        final String amount = sa.getApi() == ApiType.Surveil ? sa.getParamOrDefault("Amount", "1")
                : sa.getParamOrDefault("ScryNum", "1");
        final int n;
        try {
            n = AbilityUtils.calculateAmount(sa.getHostCard(), amount, sa);
        } catch (final RuntimeException e) {
            return false;
        }
        final CardCollectionView library = ai.getCardsIn(ZoneType.Library);
        if (n <= 0 || library.isEmpty()) {
            return false;
        }
        for (int i = 0; i < Math.min(n, library.size()); i++) {
            final Card c = library.get(i);
            if (!AiCardMemory.isRememberedCard(ai, c, AiCardMemory.MemorySet.KEPT_ON_TOP)
                    || ComputerUtil.scryWillMoveCardToBottomOfLibrary(ai, c)) {
                return false;
            }
        }
        return true;
    }

    /** A Pump or PumpAll curse that only lowers power: it does something only to creatures in combat. */
    static boolean isPowerOnlyCurse(final SpellAbility a) {
        if (a.getApi() != ApiType.PumpAll && a.getApi() != ApiType.Pump || !a.isCurse()) {
            return false;
        }
        if (a.hasParam("NumDef") || a.hasParam("KW")) {
            return false;
        }
        final String att = a.getParamOrDefault("NumAtt", "");
        return att.startsWith("-");
    }

    /** Draws cards, and its only other effect shrinks attackers (Hydrolash, Bewildering Blizzard). */
    static boolean isCombatCurseCantrip(final SpellAbility sa) {
        boolean draws = false;
        boolean curse = false;
        for (SpellAbility a = sa.getRootAbility(); a != null; a = a.getSubAbility()) {
            if (a.getApi() == ApiType.Draw) {
                draws = true;
            } else if (isPowerOnlyCurse(a)) {
                curse = true;
            } else {
                return false;
            }
        }
        return draws && curse;
    }

    /**
     * Whether a play the heuristics turned down stays turned down for Flat Monte Carlo discovery: their timing for card
     * selection and for a combat curse with a cantrip is final, since the rollouts score the play by the card it shows
     * or draws and cannot see what waiting for the right moment is worth.
     */
    public static boolean heuristicsHoldIt(final SpellAbility sa) {
        return sa != null && sa.getHostCard() != null && (isPureSelection(sa) || isCombatCurseCantrip(sa));
    }

    /**
     * On the AI's own main phase: no other card it could play this turn, at most one other card in hand, and a way to
     * use the card a cantrip draws now (a land drop left, or mana to spare after paying for it). Drawing the card now
     * is then worth more than holding the spell for the opponent's turn.
     */
    public static boolean starvedForCards(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ph.getPhase().isMain() || !ai.getGame().getStack().isEmpty()) {
            return false;
        }
        final Card host = sa.getHostCard();
        final boolean landDrop = ai.getMaxLandPlaysInfinite() || ai.getLandsPlayedThisTurn() < ai.getMaxLandPlays();
        int others = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c.equals(host)) {
                continue;
            }
            if (++others > 1) {
                return false;
            }
            if (c.isLand()) {
                if (landDrop) {
                    return false;
                }
                continue;
            }
            for (final SpellAbility other : c.getSpellAbilities()) {
                if (other.isSpell() && ComputerUtilMana.hasEnoughManaSourcesToCast(other, ai)) {
                    return false;
                }
            }
        }
        final int cost = sa.getPayCosts() == null ? 0 : sa.getPayCosts().getTotalMana().getCMC();
        return landDrop || ComputerUtilMana.getAvailableManaEstimate(ai, false) > cost;
    }
}
