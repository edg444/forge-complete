package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilAbility;
import forge.ai.SpellApiToAi;
import forge.game.ability.ApiType;
import forge.game.keyword.Keyword;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** 
 * AbilityFactory for Creature Spells.
 *
 */
public class PermanentNoncreatureAi extends PermanentAi {

    /**
     * The rest of the logic not covered by the canPlayAI template is defined
     * here
     */
    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        AiAbilityDecision decision = super.checkApiLogic(ai, sa);
        if (!decision.willingToPlay()) {
            return decision;
        }

        final Card host = sa.getHostCard();
        final String sourceName = ComputerUtilAbility.getAbilitySourceName(sa);
        final Game game = ai.getGame();

        // Check for valid targets before casting
        if (host.hasSVar("OblivionRing")) {
            // TODO: only the "may" case wouldn't fail checkETBEffects - replace with NeedsToPlay SVar?
            SpellAbility effectExile = AbilityFactory.getAbility(host.getSVar("TrigExile"), host);
            final ZoneType origin = ZoneType.listValueOf(effectExile.getParamOrDefault("Origin", "Battlefield")).get(0);
            effectExile.setActivatingPlayer(ai);
            CardCollection targets = CardLists.getTargetableCards(game.getCardsIn(origin), effectExile);
            if (sourceName.equals("Suspension Field")
                    || sourceName.equals("Detention Sphere")) {
                // existing "exile until leaves" enchantments only target opponent's permanents
                targets = CardLists.filterControlledBy(targets, ai.getOpponents());
            }
            if (targets.isEmpty()) {
                return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
            }
        }
        // Flash Equipment that attaches as it enters (Vibranium Strike Gauntlets) is a buff Aura at instant speed:
        // a combat trick or an end-of-turn play, never a bonus for a creature that's about to die
        if (host.isEquipment() && host.hasKeyword(Keyword.FLASH) && !ai.canCastSorcery()) {
            for (final Trigger t : host.getTriggers()) {
                if (t.getMode() != TriggerType.ChangesZone || !"Battlefield".equals(t.getParam("Destination"))
                        || !"Card.Self".equals(t.getParam("ValidCard"))) {
                    continue;
                }
                final SpellAbility attach = t.ensureAbility();
                if (attach == null || attach.getApi() != ApiType.Attach || !attach.usesTargeting()) {
                    continue;
                }
                final SpellAbility probe = attach.copy(ai);
                probe.resetTargets();
                SpellApiToAi.Converter.get(probe).doTriggerNoCostWithSubs(ai, probe, true);
                final Card target = probe.getTargetCard();
                if (target == null || !AttachAi.doAdvancedFlashAuraLogic(ai, sa, target)) {
                    return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
                }
            }
        }
        return decision;
    }
}
