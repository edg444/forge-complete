package forge.ai.ability;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.game.GameObject;

import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.zone.ZoneType;

/**
 * Moving Auras and Equipment between permanents, and licids.
 * <ul>
 * <li>AILogic$ MoveAttachment (Aura Finesse, Brass Squire, Crown of the Ages, Kjeldoran Pride, Magnetic Theft, Simic
 * Guildmage, Aura Graft...): each move is scored by who an attachment helps - a helpful one on the AI's best creature,
 * a harmful one on the opponent's - and an attachment whose permanent is about to be destroyed is worth nothing where
 * it is. The destination a "Choices" move picks at resolution is the one it decided on.</li>
 * <li>AILogic$ Licid: the creature becomes an Aura; the target is the one AttachAi would pick for that Aura, if it's
 * worth more than the creature's own body.</li>
 * </ul>
 */
public final class AttachmentMoveAi {

    private static final Map<Integer, Card> DESTINATIONS = new ConcurrentHashMap<>();

    private AttachmentMoveAi() { }

    public static boolean handles(final String logic) {
        return "MoveAttachment".equals(logic) || "Licid".equals(logic);
    }

    public static Card destination(final Card host) {
        return host == null ? null : DESTINATIONS.get(host.getId());
    }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        return "Licid".equals(sa.getParam("AILogic")) ? licid(ai, sa) : move(ai, sa);
    }

    private static String attachLogic(final Card att) {
        String logic = att.getSVar("AttachAILogic");
        if (logic.isEmpty()) {
            for (final SpellAbility s : att.getSpellAbilities()) {
                if (s.getApi() == ApiType.Attach && s.hasParam("AILogic")) {
                    logic = s.getParam("AILogic");
                }
            }
        }
        return logic;
    }

    private static boolean controls(final Card att) {
        if ("GainControl".equals(attachLogic(att))) {
            return true;
        }
        for (final StaticAbility st : att.getStaticAbilities()) {
            if (st.hasParam("GainControl")) {
                return true;
            }
        }
        return false;
    }

    private static boolean helps(final Card att) {
        if (att.isEquipment()) {
            return true;
        }
        final String logic = attachLogic(att);
        if ("Curse".equals(logic) || "KeepTapped".equals(logic) || "ChangeType".equals(logic)) {
            return false;
        }
        for (final StaticAbility st : att.getStaticAbilities()) {
            final String mode = st.getParamOrDefault("Mode", "");
            if (mode.startsWith("Cant") || st.getParamOrDefault("AddPower", "").startsWith("-")
                    || st.getParamOrDefault("AddToughness", "").startsWith("-")) {
                return false;
            }
        }
        return true;
    }

    /** What it means for the AI that att is attached to c, with ctrl controlling att. */
    private static int goodness(final Player ai, final Card att, final Card c, final Player ctrl, final List<GameObject> doomed) {
        if (c == null || doomed.contains(c) || !c.isCreature()) {
            return 0;
        }
        final int value = ComputerUtilCard.evaluateCreature(c);
        if (controls(att)) {
            // Control Magic and the like: worth the creature to whoever controls the Aura, when it isn't theirs anyway
            if (c.getOwner().equals(ctrl)) {
                return 0;
            }
            return ctrl.equals(ai) ? value : -value;
        }
        final Player side = c.getController();
        final int sign = side.equals(ai) ? 1 : side.isOpponentOf(ai) ? -1 : 0;
        return helps(att) ? sign * value / 3 : -sign * value / 2;
    }

    private static boolean timing(final Player ai, final List<GameObject> doomed, final List<Card> atts) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ai.getGame().getStack().isEmpty()) {
            // in answer to removal on what it's attached to
            return atts.stream().anyMatch(a -> a.getAttachedTo() != null && doomed.contains(a.getAttachedTo()));
        }
        return ph.isPlayerTurn(ai) && ph.is(PhaseType.MAIN1) || ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn().equals(ai);
    }

    private static AiAbilityDecision move(final Player ai, final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final List<GameObject> doomed = ai.getGame().getStack().isEmpty() ? List.of() : ComputerUtil.predictThreatenedObjects(ai, null);
        // the Attach doing the move, and where the attachment comes from: its parent's target, its own target, or itself
        SpellAbility attach = null;
        for (SpellAbility sub = sa; sub != null; sub = sub.getSubAbility()) {
            if (sub.getApi() == ApiType.Attach) {
                attach = sub;
                break;
            }
        }
        if (attach == null) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final boolean takesControl = sa.getApi() == ApiType.GainControl;
        final SpellAbility objectSa = attach != sa && sa.usesTargeting() ? sa
                : attach.hasParam("Object") && "Targeted".equals(attach.getParam("Object")) ? attach : null;
        final List<Card> atts = new ArrayList<>();
        if (objectSa != null) {
            objectSa.resetTargets();
            atts.addAll(CardUtil.getValidCardsToTarget(objectSa));
        } else {
            atts.add(host);
        }
        if (!timing(ai, doomed, atts)) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }
        final boolean destTargeted = attach.usesTargeting() && attach != objectSa;
        int best = Integer.MIN_VALUE;
        Card bestAtt = null;
        Card bestDest = null;
        for (final Card att : atts) {
            if (!att.isAttachment()) {
                continue;
            }
            final Player ctrl = takesControl ? ai : att.getController();
            final int now = goodness(ai, att, att.getAttachedTo(), att.getController(), doomed);
            final List<Card> dests;
            if (destTargeted) {
                attach.resetTargets();
                dests = CardUtil.getValidCardsToTarget(attach);
            } else if (attach.hasParam("Choices")) {
                dests = CardLists.getValidCards(ai.getGame().getCardsIn(ZoneType.Battlefield), attach.getParam("Choices"), ai, host, attach);
            } else {
                dests = List.of();
            }
            for (final Card d : dests) {
                if (d.equals(att.getAttachedTo()) || d.equals(att) || !d.canBeAttached(att, attach)) {
                    continue;
                }
                final int v = goodness(ai, att, d, ctrl, doomed) - now;
                if (v > best) {
                    best = v;
                    bestAtt = att;
                    bestDest = d;
                }
            }
        }
        final int threshold = sa.isSpell() && !hasDraw(sa) ? 60 : 30;
        if (bestAtt == null || best < threshold) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        if (objectSa != null) {
            objectSa.getTargets().add(bestAtt);
        }
        if (destTargeted) {
            attach.getTargets().add(bestDest);
        } else {
            DESTINATIONS.put(host.getId(), bestDest);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    private static boolean hasDraw(final SpellAbility sa) {
        for (SpellAbility sub = sa; sub != null; sub = sub.getSubAbility()) {
            if (sub.getApi() == ApiType.Draw) {
                return true;
            }
        }
        return false;
    }

    private static AiAbilityDecision licid(final Player ai, final SpellAbility sa) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ph.is(PhaseType.MAIN1) || !ai.getGame().getStack().isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }
        final Card host = sa.getHostCard();
        SpellAbility attach = sa.getSubAbility();
        while (attach != null && attach.getApi() != ApiType.Attach) {
            attach = attach.getSubAbility();
        }
        if (attach == null || !attach.usesTargeting()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final String logic = attach.getParamOrDefault("AILogic", "Pump");
        attach.resetTargets();
        final CardCollection options = CardLists.filter(CardUtil.getValidCardsToTarget(attach), c -> !c.equals(host));
        final Card target = AttachAi.attachGeneralAI(ai, attach, options, false, host, logic);
        if (target == null) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final boolean theirs = target.getController().isOpponentOf(ai);
        final boolean wantsTheirs = !"Pump".equals(logic);
        if (theirs != wantsTheirs) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        // the licid gives up its own body for it
        final int body = ComputerUtilCard.evaluateCreature(host);
        final int worth = ComputerUtilCard.evaluateCreature(target);
        final int needed = "GainControl".equals(logic) ? body : "Curse".equals(logic) ? body + 20 : body + 60;
        if (worth < needed) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        attach.getTargets().add(target);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
}
