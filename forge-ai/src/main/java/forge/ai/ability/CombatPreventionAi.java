package forge.ai.ability;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import forge.ai.AiTeams;
import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.ComputerUtilCost;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.FileSection;

/**
 * AILogic$ PreventCombat: an effect that prevents some damage this turn (Benalish Missionary, Ebony Horse, Divine
 * Light, Redeem, Hunter's Ambush...) is only worth it once blocks are known, by what it saves in that combat - the AI's
 * life and creatures, less the opposing life and creatures it saves too. Its prevention is read off the card itself:
 * DamageDone prevention replacements and "Prevent all ... damage" keywords from statics or pumps, where a targeted
 * one ("Card.IsRemembered") covers whatever the AI would target.
 */
public final class CombatPreventionAi {

    private static final String BY = "Prevent all combat damage that would be dealt by CARDNAME.";
    private static final String ALL_BY = "Prevent all damage that would be dealt by CARDNAME.";
    private static final String TO = "Prevent all damage that would be dealt to CARDNAME.";
    private static final String COMBAT_TO = "Prevent all combat damage that would be dealt to CARDNAME.";

    /** one "damage from a source matching this to a target matching that is prevented"; null matches anything */
    private record Shield(String source, String target) { }

    private CombatPreventionAi() { }

    public static AiAbilityDecision decide(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        if (combat == null || !game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS) || !game.getStack().isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.WaitForCombat);
        }
        final List<Shield> shields = shields(sa);
        if (shields.isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        // a repeatable ability can spend itself on less than a card has to be worth
        final int threshold = sa.isSpell() ? 100 : 40;
        SpellAbility tgtSa = sa;
        while (tgtSa != null && !tgtSa.usesTargeting()) {
            tgtSa = tgtSa.getSubAbility();
        }
        if (tgtSa == null) {
            return value(ai, combat, sa, shields, new CardCollection()) >= threshold
                    ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.DoesntImpactCombat);
        }

        tgtSa.resetTargets();
        final boolean xTargets = "X".equals(tgtSa.getParam("TargetMax")) && "Count$xPaid".equals(tgtSa.getSVar("X"));
        if (xTargets) {
            // Serene Sunset: as many targets as it can pay for, trimmed to the ones worth it below
            ComputerUtilCost.setMaxXValue(sa.getRootAbility(), ai, sa.isTrigger());
        }
        final CardCollection combatants = new CardCollection(combat.getAttackers());
        combatants.addAll(combat.getAllBlockers());
        final CardCollection options = CardLists.filter(CardUtil.getValidCardsToTarget(tgtSa), combatants::contains);
        final CardCollection chosen = new CardCollection();
        int best = value(ai, combat, sa, shields, chosen);
        // greedily, for "up to N" and "any number of" targets alike
        while (tgtSa.canAddMoreTarget() && !options.isEmpty()) {
            Card pick = null;
            int pickValue = best;
            for (final Card c : options) {
                chosen.add(c);
                final int v = value(ai, combat, sa, shields, chosen);
                chosen.remove(c);
                if (v > pickValue) {
                    pickValue = v;
                    pick = c;
                }
            }
            if (pick == null) {
                break;
            }
            chosen.add(pick);
            options.remove(pick);
            best = pickValue;
        }
        if (best < threshold || chosen.size() < tgtSa.getMinTargets()) {
            return new AiAbilityDecision(0, AiPlayDecision.DoesntImpactCombat);
        }
        for (final Card c : chosen) {
            tgtSa.getTargets().add(c);
        }
        if (xTargets) {
            sa.getRootAbility().setXManaCostPaid(chosen.size());
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    private static List<Shield> shields(final SpellAbility sa) {
        final List<Shield> shields = new ArrayList<>();
        for (SpellAbility sub = sa; sub != null; sub = sub.getSubAbility()) {
            if (sub.getApi() == ApiType.Effect) {
                for (final String name : sub.getParamOrDefault("ReplacementEffects", "").split(",")) {
                    if (name.isEmpty() || !sub.hasSVar(name)) {
                        continue;
                    }
                    final Map<String, String> re = FileSection.parseToMap(sub.getSVar(name), FileSection.DOLLAR_SIGN_KV_SEPARATOR);
                    if ("DamageDone".equals(re.get("Event")) && ("True".equals(re.get("Prevent")) || re.containsKey("PreventionEffect"))) {
                        shields.add(new Shield(re.get("ValidSource"), re.get("ValidTarget")));
                    }
                }
                for (final String name : sub.getParamOrDefault("StaticAbilities", "").split(",")) {
                    if (name.isEmpty() || !sub.hasSVar(name)) {
                        continue;
                    }
                    final Map<String, String> st = FileSection.parseToMap(sub.getSVar(name), FileSection.DOLLAR_SIGN_KV_SEPARATOR);
                    final String kw = st.getOrDefault("AddKeyword", "");
                    if (kw.contains(BY) || kw.contains(ALL_BY)) {
                        shields.add(new Shield(st.get("Affected"), null));
                    } else if (kw.contains(TO) || kw.contains(COMBAT_TO)) {
                        shields.add(new Shield(null, st.get("Affected")));
                    }
                }
            } else if (sub.getApi() == ApiType.Pump && sub.hasParam("KW")) {
                final String kw = sub.getParam("KW");
                if (kw.contains(BY) || kw.contains(ALL_BY)) {
                    shields.add(new Shield("Card.IsRemembered", null));
                }
                if (kw.contains(TO) || kw.contains(COMBAT_TO)) {
                    shields.add(new Shield(null, "Card.IsRemembered"));
                }
            }
        }
        return shields;
    }

    private static boolean prevented(final Player ai, final SpellAbility sa, final List<Shield> shields, final Card source,
            final GameEntity target) {
        final Card host = sa.getHostCard();
        for (final Shield s : shields) {
            if ((s.source() == null || source.isValid(s.source().split(","), ai, host, sa))
                    && (s.target() == null || target.isValid(s.target().split(","), ai, host, sa))) {
                return true;
            }
        }
        return false;
    }

    // what the prevention is worth to the AI in this combat, with `chosen` as its targets
    private static int value(final Player ai, final Combat combat, final SpellAbility sa, final List<Shield> shields,
            final CardCollection chosen) {
        final Card host = sa.getHostCard();
        for (final Card c : chosen) {
            host.addRemembered(c);
        }
        try {
            int value = 0;
            int lifeSaved = 0;
            for (final Player opp : ai.getOpponents()) {
                int incoming = 0;
                int stopped = 0;
                for (final Card att : combat.getAttackersOf(opp)) {
                    if (!combat.isUnblocked(att)) {
                        continue;
                    }
                    final int dmg = ComputerUtilCombat.predictDamageTo(opp, ComputerUtilCombat.damageIfUnblocked(att, opp, combat, false), att, true);
                    incoming += dmg;
                    if (prevented(ai, sa, shields, att, opp)) {
                        stopped += dmg;
                    }
                }
                if (stopped > 0 && incoming >= opp.getLife() && incoming - stopped < opp.getLife() && opp.canLoseLife()) {
                    return -1000;
                }
                value -= stopped * 6;
            }
            for (final Card att : combat.getAttackersOf(ai)) {
                if (combat.isUnblocked(att) && prevented(ai, sa, shields, att, ai)) {
                    lifeSaved += ComputerUtilCombat.predictDamageTo(ai, ComputerUtilCombat.damageIfUnblocked(att, ai, combat, false), att, true);
                }
            }
            if (lifeSaved > 0) {
                final int remaining = ComputerUtilCombat.lifeThatWouldRemain(ai, combat);
                if (remaining < 1 && remaining + lifeSaved >= 1 && ai.canLoseLife() && !ai.cantLoseForZeroOrLessLife()) {
                    value += 1000;
                } else {
                    value += lifeSaved * (ComputerUtilCombat.lifeInDanger(ai, combat) ? 30 : 6);
                }
            }
            final CardCollection combatants = new CardCollection(combat.getAttackers());
            combatants.addAll(combat.getAllBlockers());
            for (final Card c : combatants) {
                if (!ComputerUtilCombat.combatantWouldBeDestroyed(ai, c, combat)) {
                    continue;
                }
                final CardCollection foes = combat.isAttacking(c) ? combat.getBlockers(c) : combat.getAttackersBlockedBy(c);
                if (foes.isEmpty() || foes.anyMatch(f -> !prevented(ai, sa, shields, f, c))) {
                    continue;
                }
                value -= AiTeams.sign(ai, c.getController()) * ComputerUtilCard.evaluateCreature(c);
            }
            return value;
        } finally {
            for (final Card c : chosen) {
                host.removeRemembered(c);
            }
        }
    }
}
