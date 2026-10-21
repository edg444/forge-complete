package forge.game.ability.effects;

import java.util.List;

import com.google.common.collect.Lists;

import forge.card.CardStateName;
import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCloneStates;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactory;
import forge.game.card.CardLists;
import forge.game.card.CardState;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * It Came from Planet Glurg: the host becomes a copy of Amount$ different creatures at once (user, 2026-10-21: the full
 * merge). Each creature's copiable values are copied as Clone would, then combined: total power and toughness, combined
 * mana costs, every type, color and ability, and every name - name checks see each one (rulings; Card.getNames).
 */
public class CloneCombinedEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getHostCard() + " becomes a copy of several creatures at once.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();
        final int amount = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Amount", "1"), sa);
        if (amount <= 0) {
            return;
        }
        final CardCollection choices = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield),
                sa.getParamOrDefault("Choices", "Creature.Other"), activator, host, sa);
        if (choices.size() < amount) {
            return;
        }
        final CardCollectionView chosen = activator.getController().chooseCardsForEffect(choices, sa,
                "Choose " + amount + " different creatures to copy", amount, amount, false, null);
        if (chosen.size() < amount) {
            return;
        }

        final CardCloneStates states = CardFactory.getCloneStates(chosen.getFirst(), host, sa);
        final CardState merged = states.get(CardStateName.Original);
        final List<String> names = Lists.newArrayList(merged.getName());
        for (int i = 1; i < chosen.size(); i++) {
            final CardState other = CardFactory.getCloneStates(chosen.get(i), host, sa).get(CardStateName.Original);
            names.add(other.getName());
            merged.setBasePower(merged.getBasePower() + other.getBasePower());
            merged.setBaseToughness(merged.getBaseToughness() + other.getBaseToughness());
            merged.setManaCost(ManaCost.combine(merged.getManaCost(), other.getManaCost()));
            final forge.card.CardType types = new forge.card.CardType(merged.getType());
            types.addAll(other.getType());
            merged.setType(types);
            merged.addColor(other.getColor());
            for (final java.util.Map.Entry<String, String> svar : other.getSVars().entrySet()) {
                if (!merged.getSVars().containsKey(svar.getKey())) {
                    merged.setSVar(svar.getKey(), svar.getValue());
                }
            }
            merged.addAbilitiesFrom(other, false);
        }
        merged.setName(String.join(" and ", names));

        game.getTriggerHandler().clearActiveTriggers(host, null);
        host.addCloneState(states, game.getNextTimestamp());
        host.setCopiedNames(names);
    }
}
