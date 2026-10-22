package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityFactory;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.util.MyRandom;

/**
 * Sort of _____ and _____: two different swords of the cycle are chosen at random, and the host gets the first piece of
 * the first one's combat damage trigger and the second piece of the second one's. Swords$ lists them as
 * "Name/FirstPieceSVar/SecondPieceSVar"; Trigger$ is the trigger, without its Execute$; each piece's
 * SpellDescription$ is its part of the trigger's text.
 */
public class SortOfSwordEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Choose two swords at random.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final List<String> swords = new ArrayList<>(Arrays.asList(sa.getParam("Swords").split(",")));
        if (swords.size() < 2) {
            return;
        }
        Collections.shuffle(swords, MyRandom.getRandom());
        final String[] first = swords.get(0).split("/");
        final String[] second = swords.get(1).split("/");

        final SpellAbility ability = AbilityFactory.getAbility(host.getSVar(first[1]), host);
        final AbilitySub tail = (AbilitySub) AbilityFactory.getAbility(host.getSVar(second[2]), host);
        SpellAbility last = ability;
        while (last.getSubAbility() != null) {
            last = last.getSubAbility();
        }
        last.setSubAbility(tail);

        final String text = "Whenever equipped creature deals combat damage to a player, "
                + ability.getParamOrDefault("SpellDescription", first[0]) + " and "
                + tail.getParamOrDefault("SpellDescription", second[0]) + " (" + first[0] + ", then " + second[0] + ")";
        final Trigger trigger = TriggerHandler.parseTrigger(host.getSVar(sa.getParam("Trigger"))
                + " | TriggerDescription$ " + text, host, true);
        trigger.setOverridingAbility(ability);
        host.addTrigger(trigger);
        host.updateStateForView();
        game.getGameLog().add(GameLogEntryType.INFORMATION, host + " is sort of " + first[0] + " and " + second[0] + ".");
    }
}
