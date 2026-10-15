package forge.game.ability.effects;

import java.util.Map;

import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.collect.FCollection;

/**
 * Interplanar Brushwagg: the defined creatures also attack one of the given players or planeswalkers - whoever's
 * attacking chooses which when there's more than one (its ruling). They become attacking without being declared, so
 * nothing that triggers on attacking sees it, and nothing that stops them attacking applies (its rulings).
 */
public class JoinAttackEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return sa.getDescription();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final Combat combat = game.getCombat();
        if (combat == null) {
            return;
        }
        final FCollection<GameEntity> defenders = new FCollection<>();
        for (final GameEntity e : AbilityUtils.getDefinedEntities(host, sa.getParamOrDefault("Defenders", "Defending"), sa)) {
            if (combat.getDefenders().contains(e)) {
                defenders.add(e);
            }
        }
        if (defenders.isEmpty()) {
            return;
        }
        Player chooser = combat.getAttackingPlayer();
        if (sa.hasParam("Chooser")) {
            chooser = AbilityUtils.getDefinedPlayers(host, sa.getParam("Chooser"), sa).stream().findFirst().orElse(chooser);
        }
        for (final Card c : getDefinedCardsOrTargeted(sa)) {
            if (!c.isCreature() || !c.isInPlay() || combat.isAttacking(c)) {
                continue;
            }
            GameEntity defender = defenders.getFirst();
            if (defenders.size() > 1) {
                final Map<String, Object> params = Maps.newHashMap();
                params.put("Attacker", c);
                defender = chooser.getController().chooseSingleEntityForEffect(defenders, sa,
                        "Choose what " + c.getTranslatedName() + " attacks", false, params);
            }
            if (defender == null) {
                continue;
            }
            combat.addAttacker(c, defender);
            combat.getBandOfAttacker(c).setBlocked(false);
            game.updateCombatForView();
            game.fireEvent(new forge.game.event.GameEventCombatChanged());
        }
    }
}
