package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;

/**
 * Kaya, Ghost Haunter (CMB1 #94), checked against Scryfall and its 4 rulings 2026-10-15: she haunts a creature from
 * exile until it leaves, returning as a new object; her emblems target any haunted creature.
 */
public class KayaGhostHaunterTest extends AITest {

    private static Card castKaya(final Game game, final Player p) {
        Card onStack = game.getAction().moveToStack(addCardToZoneStatic(p, "Kaya, Ghost Haunter"), null);
        SpellAbility sa = onStack.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        game.getStack().add(sa);
        game.getStack().resolveStack();
        return game.getCardState(onStack);
    }

    private static Card addCardToZoneStatic(final Player p, final String name) {
        return p.getZone(ZoneType.Hand).getCards().stream().filter(c -> c.getName().equals(name)).findFirst().get();
    }

    private static void emblemUpkeep(final Game game, final Player you, final String nameContains, final Card target) {
        for (Card e : you.getCardsIn(ZoneType.Command)) {
            if (!e.getName().contains(nameContains)) {
                continue;
            }
            for (Trigger t : e.getTriggers()) {
                SpellAbility sa = t.ensureAbility();
                sa.setActivatingPlayer(you);
                sa.getTargets().add(target);
                AbilityUtils.resolve(sa);
            }
        }
    }

    @Test
    public void hauntingAndTheEmblems() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card bears = addCard("Grizzly Bears", opp);
        addCardToZone("Kaya, Ghost Haunter", ai, ZoneType.Hand);
        Card kaya = castKaya(game, ai);
        AssertJUnit.assertEquals(3, kaya.getCounters(CounterEnumType.LOYALTY));

        for (String cost : new String[] {"1", "2"}) {
            SpellAbility emblem = kaya.getSpellAbilities().stream().filter(sa -> sa.isActivatedAbility()
                    && sa.getApi() == ApiType.Effect && sa.getPayCosts().toSimpleString().contains(cost)).findFirst().get();
            emblem.setActivatingPlayer(ai);
            AbilityUtils.resolve(emblem);
        }
        AssertJUnit.assertEquals(2, ai.getCardsIn(ZoneType.Command).size());

        SpellAbility haunt = kaya.getSpellAbilities().stream().filter(sa -> sa.isActivatedAbility() && sa.getApi() == ApiType.Haunt).findFirst().get();
        haunt.setActivatingPlayer(ai);
        haunt.getTargets().add(bears);
        AbilityUtils.resolve(haunt);
        Card exiled = game.getCardState(kaya);
        AssertJUnit.assertTrue(exiled.isInZone(ZoneType.Exile));
        AssertJUnit.assertTrue(bears.isValid("Creature.Haunted", ai, null, null));

        emblemUpkeep(game, ai, "damage", bears);
        AssertJUnit.assertEquals("3 to the haunted creature's owner", 17, opp.getLife());

        emblemUpkeep(game, ai, "control", bears);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(ai, bears.getController());

        // the creature leaves: Kaya comes back as a new object with her starting loyalty
        game.getAction().destroy(bears, null, true, null);
        game.getAction().checkStateEffects(true);
        game.getAction().checkStateEffects(true);
        Card back = game.getCardState(exiled);
        AssertJUnit.assertTrue(back.isInPlay());
        AssertJUnit.assertEquals(ai, back.getController());
        AssertJUnit.assertEquals(3, back.getCounters(CounterEnumType.LOYALTY));
    }

    @Test
    public void controlLastsOnlyWhileHaunted() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card bears = addCard("Grizzly Bears", opp);
        addCardToZone("Kaya, Ghost Haunter", ai, ZoneType.Hand);
        Card kaya = castKaya(game, ai);
        SpellAbility emblem = kaya.getSpellAbilities().stream().filter(sa -> sa.isActivatedAbility()
                && sa.getApi() == ApiType.Effect && sa.getPayCosts().toSimpleString().contains("2")).findFirst().get();
        emblem.setActivatingPlayer(ai);
        AbilityUtils.resolve(emblem);
        SpellAbility haunt = kaya.getSpellAbilities().stream().filter(sa -> sa.isActivatedAbility() && sa.getApi() == ApiType.Haunt).findFirst().get();
        haunt.setActivatingPlayer(ai);
        haunt.getTargets().add(bears);
        AbilityUtils.resolve(haunt);

        emblemUpkeep(game, ai, "control", bears);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(ai, bears.getController());

        // Kaya leaves exile some other way: the creature is no longer haunted, so the control ends
        game.getAction().moveToHand(game.getCardState(kaya), null);
        game.getAction().checkStateEffects(true);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(bears.isValid("Creature.Haunted", ai, null, null));
        AssertJUnit.assertEquals(opp, bears.getController());
    }

    @Test
    public void returnsWhenTheCreatureLeaves() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card bears = addCard("Grizzly Bears", opp);
        addCardToZone("Kaya, Ghost Haunter", ai, ZoneType.Hand);
        Card kaya = castKaya(game, ai);
        SpellAbility haunt = kaya.getSpellAbilities().stream().filter(sa -> sa.isActivatedAbility() && sa.getApi() == ApiType.Haunt).findFirst().get();
        haunt.setActivatingPlayer(ai);
        haunt.getTargets().add(bears);
        AbilityUtils.resolve(haunt);
        Card exiled = game.getCardState(kaya);
        game.getAction().destroy(bears, null, true, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(game.getCardState(exiled).isInPlay());
    }
}
