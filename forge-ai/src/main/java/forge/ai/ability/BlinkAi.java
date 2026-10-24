package forge.ai.ability;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.game.GameObject;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

/**
 * AILogic$ Blink (Ghostly Flicker, Illusionist's Stratagem, Siren's Ruse, Ghostway): exiling its own permanents and
 * returning them. Worth it for what's about to be destroyed (in answer to removal), for an enters-the-battlefield
 * trigger to use again, for shedding an opponent's Aura, and for a tapped land coming back untapped; an Aura or Equipment
 * of its own falls off, which counts against it.
 */
public final class BlinkAi {

    private BlinkAi() { }

    private static boolean hasEtb(final Card c) {
        for (final Trigger t : c.getTriggers()) {
            if (t.getMode() == TriggerType.ChangesZone && "Battlefield".equals(t.getParam("Destination"))
                    && t.getParamOrDefault("ValidCard", "").startsWith("Card.Self")) {
                return true;
            }
        }
        return false;
    }

    private static int score(final Player ai, final Card c, final List<GameObject> doomed) {
        int v = 0;
        if (doomed.contains(c)) {
            v += c.isCreature() ? ComputerUtilCard.evaluateCreature(c) : ExchangeTradeAi.value(c);
        }
        if (hasEtb(c)) {
            v += 60;
        }
        for (final Card a : c.getAttachedCards()) {
            if (a.getController().isOpponentOf(ai)) {
                v += c.isCreature() ? ComputerUtilCard.evaluateCreature(c) / 2 : 40;
            } else {
                v -= 30;
            }
        }
        if (c.isLand() && c.isTapped()) {
            v += 10;
        }
        // tokens and copies don't come back
        if (c.isToken() || c.isCopiedSpell()) {
            v -= 1000;
        }
        return v;
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        final boolean answering = !ai.getGame().getStack().isEmpty();
        final List<GameObject> doomed = answering ? ComputerUtil.predictThreatenedObjects(ai, null) : List.of();
        final boolean quiet = !answering && (ph.isPlayerTurn(ai) && ph.is(PhaseType.MAIN2)
                || ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn().equals(ai));
        if (!answering && !quiet) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }
        final boolean draws = drawsCard(sa);
        if (sa.getApi() == ApiType.ChangeZoneAll) {
            // Ghostway: everything of its own comes back at end of turn - for a sweeper
            int v = 0;
            for (final Card c : CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), sa.getParamOrDefault("ChangeType", "Creature.YouCtrl"),
                    ai, sa.getHostCard(), sa)) {
                v += score(ai, c, doomed);
            }
            return v >= 150 ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        if (!sa.usesTargeting()) {
            // Anurid Brushhopper: itself, out of the way of removal
            final boolean self = "Self".equals(sa.getParam("Defined"));
            return self && doomed.contains(sa.getHostCard()) ? new AiAbilityDecision(100, AiPlayDecision.WillPlay)
                    : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        sa.resetTargets();
        final List<Card> options = new ArrayList<>(CardLists.filter(CardUtil.getValidCardsToTarget(sa), c -> c.getController().equals(ai)));
        options.sort(Comparator.comparingInt((Card c) -> score(ai, c, doomed)).reversed());
        final int min = sa.getMinTargets();
        final int max = sa.getMaxTargets();
        final List<Card> pick = new ArrayList<>();
        int total = 0;
        for (final Card c : options) {
            final int s = score(ai, c, doomed);
            // a target only to make up the minimum is fine at no cost (Ghostly Flicker's second)
            if (pick.size() >= max || s < 0 || s == 0 && pick.size() >= min) {
                break;
            }
            pick.add(c);
            total += s;
        }
        // Siren's Ruse draws for a Pirate
        if (sa.getSubAbility() != null && pick.stream().anyMatch(c -> c.getType().hasCreatureType("Pirate"))) {
            total += 50;
        }
        final int threshold = draws ? 30 : sa.isSpell() ? 60 : 40;
        if (pick.size() < min || pick.isEmpty() || total < threshold) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        pick.forEach(c -> sa.getTargets().add(c));
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    private static boolean drawsCard(final SpellAbility sa) {
        for (SpellAbility sub = sa; sub != null; sub = sub.getSubAbility()) {
            if (sub.getApi() == ApiType.Draw && !sub.hasParam("ConditionDefined")) {
                return true;
            }
        }
        return false;
    }
}
