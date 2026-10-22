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
import forge.game.card.CardFactory;
import forge.game.card.CardState;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The Meld Hound: the Melder$ card melds with the Defined$ creature spell. The spell becomes the combination of both
 * (abilities and types of both, combined power, toughness and mana cost, both names unless its controller gives it a
 * new one), and the melder joins it as a merged card, so wherever the spell or permanent goes, both cards go (user
 * ruling 2026-10-22, like a real meld; GameAction moves a spell's merged cards with it too).
 */
public class MeldIntoSpellEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "The exiled creature card melds with that spell.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();
        final List<Card> spells = getDefinedCardsOrTargeted(sa);
        final List<Card> melders = AbilityUtils.getDefinedCards(host, sa.getParam("Melder"), sa);
        if (spells.isEmpty() || melders.isEmpty()) {
            return;
        }
        final Card spell = game.getCardState(spells.get(0));
        final Card melder = game.getCardState(melders.get(0));
        if (spell == null || melder == null || !spell.isInZone(ZoneType.Stack) || !melder.isInZone(ZoneType.Exile)) {
            return;
        }

        final CardCloneStates states = CardFactory.getCloneStates(spell, spell, sa);
        final CardState merged = states.get(CardStateName.Original);
        final CardState other = CardFactory.getCloneStates(melder, spell, sa).get(CardStateName.Original);
        final List<String> names = Lists.newArrayList(merged.getName(), other.getName());
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

        String name = String.join(" and ", names);
        final Player namer = spell.getController();
        if (!namer.getController().isAI() && namer.getController().confirmAction(sa, null,
                "Give the combined creature spell a new name? (Otherwise it's " + name + ".)", null)) {
            final String typed = namer.getController().guessString(sa, "Name the combined creature spell").trim();
            if (!typed.isEmpty()) {
                name = typed;
                names.clear();
                names.add(typed);
            }
        }
        merged.setName(name);

        game.getTriggerHandler().clearActiveTriggers(spell, null);
        spell.addCloneState(states, game.getNextTimestamp());
        spell.setCopiedNames(names);

        if (!spell.hasMergedCard()) {
            spell.addMergedCard(spell);
        }
        spell.addMergedCard(melder);
        melder.setMergedToCard(spell);
        game.getAction().moveTo(melder.getOwner().getZone(ZoneType.Merged), melder, sa);
        spell.updateStateForView();
    }
}
