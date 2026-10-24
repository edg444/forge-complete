package forge.ai.ability;

import forge.ai.*;
import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetChoices;

public class ChangeTargetsAi extends SpellAbilityAi {

    /*
     * (non-Javadoc)
     * 
     * @see forge.ai.SpellAbilityAi#checkApiLogic(forge.game.player.Player,
     * forge.game.spellability.SpellAbility)
     */
    @Override
    protected AiAbilityDecision checkApiLogic(Player ai, SpellAbility sa) {
        final Game game = sa.getHostCard().getGame();
        final SpellAbility topSa = game.getStack().isEmpty() ? null
                : ComputerUtilAbility.getTopSpellAbilityOnStack(game, sa);

        if ("Self".equals(sa.getParam("DefinedMagnet"))) {
            return doSpellMagnet(sa, topSa, ai);
        }
        if (sa.usesTargeting() && !sa.hasParam("DefinedMagnet") && !sa.hasParam("RandomTarget") && topSa != null) {
            return doRetarget(sa, topSa, ai);
        }
        if ("ValidStack SpellAbility".equals(sa.getParam("Defined"))) {
            // Boltbender: any number of them, each changed only if that's better
            for (final forge.game.spellability.SpellAbilityStackInstance si : game.getStack()) {
                final SpellAbility s = si.getSpellAbility();
                if (s.getActivatingPlayer() != null && s.getActivatingPlayer().isOpponentOf(ai) && RetargetAi.gain(ai, s, null) >= 30) {
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
            }
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        // The AI can't otherwise play this ability, but should at least not
        // miss mandatory activations (e.g. triggers).
        if (sa.isMandatory()) {
            return new AiAbilityDecision(50, AiPlayDecision.MandatoryPlay);
        }
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    @Override
    protected boolean willPayCosts(Player payer, SpellAbility sa, forge.game.cost.Cost cost, Card source) {
        if (ComputerUtilCost.isSacrificeSelfCost(cost) && sa.usesTargeting() && !sa.hasParam("DefinedMagnet")) {
            // Goblin Flectomancer: doRetarget already weighs the retarget against the card itself
            return ComputerUtilCost.checkLifeCost(payer, cost, source, 4, sa) && ComputerUtilCost.checkDiscardCost(payer, cost, source, sa);
        }
        return super.willPayCosts(payer, sa, cost, source);
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, forge.game.player.PlayerActionConfirmMode mode, String message,
            java.util.Map<String, Object> params) {
        // "you may choose new targets": the new targets are only taken if they're better
        return true;
    }

    // Deflection, Meddle, Redirect, Willbender: the opponent's spell on top of the stack, if new targets are worth it
    private AiAbilityDecision doRetarget(SpellAbility sa, SpellAbility topSa, Player ai) {
        final Player caster = topSa.getActivatingPlayer();
        if (caster == null || !caster.isOpponentOf(ai) || !sa.canTarget(topSa)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final Card host = sa.getHostCard();
        // Meddle, Quicksilver Dragon: only a spell whose one target is a creature (or this) changes at all
        if (sa.hasParam("ConditionTargetValidTargeting")) {
            final String[] valid = sa.getParam("ConditionTargetValidTargeting").split(",");
            final TargetChoices now = topSa.getTargets();
            if (now.isEmpty() || sa.hasParam("ConditionTargetsSingleTarget") && now.size() != 1
                    || !now.getTargetCards().stream().allMatch(c -> c.isValid(valid, ai, host, sa)) || now.getTargetCards().size() != now.size()) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }
        final Card source = topSa.getTargetCard() != null ? topSa.getTargetCard() : host;
        final java.util.function.Predicate<forge.game.GameObject> filter = sa.hasParam("TargetRestriction")
                ? forge.game.GameObjectPredicates.restriction(sa.getParam("TargetRestriction").split(","), ai, source, sa) : null;
        int gain = RetargetAi.gain(ai, topSa, filter);
        boolean draws = false;
        for (SpellAbility sub = sa.getSubAbility(); sub != null; sub = sub.getSubAbility()) {
            if (sub.getApi() == forge.game.ability.ApiType.CopySpellAbility) {
                // Wild Ricochet: a copy with the best targets on top
                gain += RetargetAi.copyValue(ai, topSa, null);
            } else if (sub.getApi() == forge.game.ability.ApiType.LoseLife) {
                // Imp's Mischief: life equal to the spell's mana value
                final int life = topSa.getHostCard().getCMC();
                if (ai.getLife() <= life + 3) {
                    return new AiAbilityDecision(0, AiPlayDecision.CurseEffects);
                }
                gain -= life * 6;
            } else if (sub.getApi() == forge.game.ability.ApiType.Draw) {
                draws = true;
            }
        }
        int threshold = sa.isSpell() && !draws ? 60 : 30;
        if (host.isCreature() && sa.getPayCosts() != null && sa.getPayCosts().hasSpecificCostType(forge.game.cost.CostSacrifice.class)) {
            // Goblin Flectomancer gives itself up for it
            threshold += ComputerUtilCard.evaluateCreature(host) / 2;
        }
        if (gain < threshold) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        sa.resetTargets();
        sa.getTargets().add(topSa);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    private AiAbilityDecision doSpellMagnet(SpellAbility sa, SpellAbility topSa, Player aiPlayer) {
        // For cards like Spellskite that retarget spells to itself
        if (topSa == null) {
            // nothing on stack, so nothing to target
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final TargetChoices topTargets = topSa.getTargets();
        final Card topHost = topSa.getHostCard();

        if (!sa.getTargets().isEmpty() && sa.isTrigger()) {
            // something was already chosen before (e.g. in response to a trigger - Mizzium Meddler), so just proceed
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        if (!topSa.usesTargeting() || topTargets.getTargetCards().contains(sa.getHostCard())) {
            // if this does not target at all or already targets host, no need to redirect it again
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        for (Card tgt : topTargets.getTargetCards()) {
            if (ComputerUtilAbility.getAbilitySourceName(sa).equals(tgt.getName()) && tgt.getController().equals(aiPlayer)) {
                // We are already targeting at least one card with the same name (e.g. in presence of 2+ Spellskites),
                // no need to retarget again to another one
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }

        if (topHost != null && !topHost.getController().isOpponentOf(aiPlayer)) {
            // make sure not to redirect our own abilities
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        if (!topSa.canTarget(sa.getHostCard())) {
            // don't try targeting it if we can't legally target the host card with it in the first place
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        if (!sa.canTarget(topSa)) {
            // don't try retargeting a spell that the current card can't legally retarget (e.g. Muck Drubb + Lightning Bolt to the face)
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        if (sa.getPayCosts().getCostMana() != null && sa.getPayCosts().getCostMana().getMana().hasPhyrexian()) {
            ManaCost manaCost = sa.getPayCosts().getCostMana().getMana();
            int payDamage = manaCost.getPhyrexianCount() * 2;
            // e.g. Spellskite or a creature receiving its ability that requires Phyrexian mana P/U
            int potentialDmg = ComputerUtil.predictDamageFromSpell(topSa, aiPlayer);
            ManaCost normalizedMana = manaCost.getNormalizedMana();
            boolean canPay = ComputerUtilMana.canPayManaCost(new ManaCostBeingPaid(normalizedMana), sa, aiPlayer, false);
            if (potentialDmg != -1 && potentialDmg <= payDamage && !canPay
                    && topTargets.contains(aiPlayer)) {
                // do not pay Phyrexian mana if the spell is a damaging one but it deals less damage or the same damage as we'll pay life
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }

        Card firstCard = topTargets.getFirstTargetedCard();
        // if we're not the target don't intervene unless we can steal a buff
        if (firstCard != null && !aiPlayer.equals(firstCard.getController()) && !topHost.getController().equals(firstCard.getController()) && !topHost.getController().getAllies().contains(firstCard.getController())) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        Player firstPlayer = topTargets.getFirstTargetedPlayer();
        if (firstPlayer != null && !aiPlayer.equals(firstPlayer)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        sa.resetTargets();
        sa.getTargets().add(topSa);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
}
