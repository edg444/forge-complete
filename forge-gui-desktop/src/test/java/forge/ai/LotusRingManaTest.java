package forge.ai;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import forge.ai.simulation.FlatMcTestBase;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.phase.PhaseType;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Lotus Ring gives the equipped creature "{T}, Sacrifice this creature: Add three mana of any one color." The AI
 * sacrificed a Ring-equipped Bloodsworn Squire, a 6/6 with vigilance, to cast a second Squire.
 */
public class LotusRingManaTest extends FlatMcTestBase {

    private Card equipped(final Card creature) {
        put("Lotus Ring", ai, ZoneType.Battlefield).attachToEntity(creature, null);
        creature.setSickness(false);
        game.getAction().checkStateEffects(true);
        return creature;
    }

    private SpellAbility ringMana(final Card creature) {
        for (final SpellAbility ma : creature.getManaAbilities()) {
            if (ma.getGrantorStatic() != null) {
                ma.setActivatingPlayer(ai);
                return ma;
            }
        }
        throw new AssertionError("the Ring did not give " + creature + " its mana ability");
    }

    private SpellAbility inHand(final String name) {
        final SpellAbility sa = put(name, ai, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        return sa;
    }

    @Test
    public void doesNotSacrificeTheEquippedCreatureForAWorseOne() {
        setUpGame();
        at(PhaseType.MAIN1, ai);
        put("Swamp", ai, ZoneType.Battlefield);
        final Card squire = equipped(put("Bloodsworn Squire", ai, ZoneType.Battlefield));
        final SpellAbility second = inHand("Bloodsworn Squire");
        assertEquals(squire.getNetPower(), 6, "the Ring is attached");
        assertFalse(ManaSacrifice.worthIt(ai, ringMana(squire), second));
        assertFalse(ComputerUtilMana.canPayManaCost(second, ai, 0, false),
                "the only way to pay is to sacrifice a 6/6 vigilance creature for a 3/3");
        assertEquals(ComputerUtilMana.getAvailableManaEstimate(ai, true), 1, "the Ring's mana is not planned on");
    }

    @Test
    public void stillCastsWhatTheLandsPayFor() {
        setUpGame();
        at(PhaseType.MAIN1, ai);
        put("Swamp", 4, ai, ZoneType.Battlefield);
        final Card squire = equipped(put("Bloodsworn Squire", ai, ZoneType.Battlefield));
        final SpellAbility second = inHand("Bloodsworn Squire");
        assertTrue(ComputerUtilMana.canPayManaCost(second, ai, 0, false));
        final ManaCostBeingPaid cost = ComputerUtilMana.calculateManaCost(second.getPayCosts(), second, ai, true, 0, false);
        final CardCollection sources = ComputerUtilMana.getManaSourcesToPayCost(cost, second, ai, false);
        assertFalse(sources.contains(squire), "four Swamps pay for it: " + sources);
    }

    @Test
    public void aRingedTokenIsStillWorthTrading() {
        setUpGame();
        at(PhaseType.MAIN1, ai);
        put("Swamp", ai, ZoneType.Battlefield);
        final Card soldier = equipped(addToken("w_1_1_soldier", ai));
        final SpellAbility squire = inHand("Bloodsworn Squire");
        assertTrue(ManaSacrifice.worth(ai, squire) >= ManaSacrifice.sacrificeValue(ai, soldier) + ManaSacrifice.MARGIN,
                "precondition: a 4/4 token is worth less than a real 3/3 plus the margin");
        assertTrue(ManaSacrifice.worthIt(ai, ringMana(soldier), squire));
        assertTrue(ComputerUtilMana.canPayManaCost(squire, ai, 0, false), "the rule is a trade check, not a ban");
    }

    @Test
    public void ownSacrificeManaIsUnchanged() {
        setUpGame();
        final Card pet = put("Blood Pet", ai, ZoneType.Battlefield);
        final SpellAbility ma = pet.getManaAbilities().get(0);
        ma.setActivatingPlayer(ai);
        assertFalse(ManaSacrifice.sacrificesGrantedCreature(ma));
        assertTrue(ManaSacrifice.worthIt(ai, ma, inHand("Lightning Bolt")));
    }
}
