package forge.ai.simulation;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/**
 * CR 613.7 (timestamps) and 613.8 (dependency) cases, each checked against the rules text rather than a ruling.
 */
public class LayerDependencyTest extends SimulationTest {

    private Game newGame() {
        Game game = initAndCreateGame();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, game.getPlayers().get(0));
        return game;
    }

    // 613.8a: Blood Moon depends on Life and Limb (which makes the Saproling a nonbasic land), so it applies
    // second even though it is older, and its 305.7 subtype-setting removes Forest again.
    @Test
    public void testOlderBloodMoonStillAppliesAfterLifeAndLimb() {
        Game game = newGame();
        Player p = game.getPlayers().get(0);
        addCard("Blood Moon", p);
        addCard("Life and Limb", p);
        Card saproling = addCard("Shroofus Sproutsire", p);

        game.getAction().checkStaticAbilities();

        AssertJUnit.assertTrue(saproling.isLand());
        AssertJUnit.assertTrue(saproling.isCreature());
        AssertJUnit.assertTrue(saproling.getType().hasSubtype("Mountain"));
        AssertJUnit.assertFalse(saproling.getType().hasSubtype("Forest"));
    }

    // 613.8a: Urborg's effect depends on Blood Moon (Blood Moon removes the ability), in either timestamp order.
    @Test
    public void testUrborgDependsOnBloodMoon() {
        for (boolean moonFirst : new boolean[] {true, false}) {
            Game game = newGame();
            Player p = game.getPlayers().get(0);
            if (moonFirst) {
                addCard("Blood Moon", p);
            }
            Card urborg = addCard("Urborg, Tomb of Yawgmoth", p);
            Card plains = addCard("Plains", p);
            if (!moonFirst) {
                addCard("Blood Moon", p);
            }

            game.getAction().checkStaticAbilities();

            AssertJUnit.assertTrue(urborg.getType().hasSubtype("Mountain"));
            AssertJUnit.assertFalse(urborg.getType().hasSubtype("Swamp"));
            AssertJUnit.assertFalse(plains.getType().hasSubtype("Swamp"));
        }
    }

    // 613.8a "what it does": Experiment Kraj's effect depends on Arcane Teachings, which changes which activated
    // abilities the countered creature has, so Kraj gains the granted ability even though Kraj is older.
    @Test
    public void testKrajGainsAbilityGrantedLater() {
        Game game = newGame();
        Player p = game.getPlayers().get(0);
        Card kraj = addCard("Experiment Kraj", p);
        Card bears = addCard("Grizzly Bears", p);
        bears.addCounterInternal(CounterEnumType.P1P1, 1, p, false, null, null);
        Card teachings = addCard("Arcane Teachings", p);
        teachings.attachToEntity(bears, null);

        game.getAction().checkStaticAbilities();

        boolean hasPing = false;
        for (SpellAbility sa : kraj.getSpellAbilities()) {
            if (sa.isActivatedAbility() && sa.getHostCard() == kraj && sa.toString().contains("1 damage")) {
                hasPing = true;
            }
        }
        AssertJUnit.assertTrue(hasPing);
    }

    // 613.7a, using the rule's own example: the ability Rune of Flight grants Colossus Hammer has Rune of Flight's
    // timestamp, so it applies after an Aura that entered between the Hammer and the Rune.
    @Test
    public void testGrantedStaticUsesGrantorTimestamp() {
        Game game = newGame();
        Player p = game.getPlayers().get(0);
        Card bears = addCard("Grizzly Bears", p);
        Card hammer = addCard("Colossus Hammer", p);
        hammer.attachToEntity(bears, null);
        Card grounded = addCard("Grounded", p);
        grounded.attachToEntity(bears, null);
        Card rune = addCard("Rune of Flight", p);
        rune.attachToEntity(hammer, null);

        game.getAction().checkStaticAbilities();

        AssertJUnit.assertTrue(bears.hasKeyword(Keyword.FLYING));
    }

    // 613.7a: "If Colossus Hammer becomes attached to a creature, both of its abilities receive new timestamps,
    // but the relative order of those timestamps remains the same." Grounded is now older than both.
    @Test
    public void testGrantedStaticKeepsRelativeOrderOnNewTimestamp() {
        Game game = newGame();
        Player p = game.getPlayers().get(0);
        Card bears = addCard("Grizzly Bears", p);
        Card grounded = addCard("Grounded", p);
        grounded.attachToEntity(bears, null);
        Card hammer = addCard("Colossus Hammer", p);
        Card rune = addCard("Rune of Flight", p);
        rune.attachToEntity(hammer, null);
        hammer.attachToEntity(bears, null);

        game.getAction().checkStaticAbilities();

        AssertJUnit.assertTrue(bears.hasKeyword(Keyword.FLYING));
    }

    // No dependency between Opalescence and Humility in 7b, so timestamp order decides Humility's own size.
    @Test
    public void testOpalescenceHumilityTimestampOrder() {
        for (boolean humilityFirst : new boolean[] {true, false}) {
            Game game = newGame();
            Player p = game.getPlayers().get(0);
            Card humility = humilityFirst ? addCard("Humility", p) : null;
            addCard("Opalescence", p);
            if (!humilityFirst) {
                humility = addCard("Humility", p);
            }
            Card bears = addCard("Grizzly Bears", p);

            game.getAction().checkStaticAbilities();

            AssertJUnit.assertTrue(humility.isCreature());
            AssertJUnit.assertEquals(humilityFirst ? 4 : 1, humility.getNetPower());
            AssertJUnit.assertEquals(1, bears.getNetPower());
        }
    }
}
