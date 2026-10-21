package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Jermane, Pride of the Circus (UNF #144), checked against Scryfall and its rulings 2026-10-21. Legs are the art's, on the
 * figure that is the creature: M12 Armored Warhorse is tagged with four, M19 Llanowar Elves (the rulings' example) isn't,
 * and M10 Black Knight's four legs are its horse's.
 */
public class JermanePrideOfTheCircusTest extends AITest {

    private Card printing(final String name, final String set, final String cn, final Player p) {
        final PaperCard pc = FModel.getMagicDb().getCommonCards().getCard(name, set, cn);
        final Card c = Card.fromPaperCard(pc, p);
        p.getZone(ZoneType.Battlefield).add(c);
        return c;
    }

    private SpellAbility ability(final Card jermane, final int index, final Player p, final Card target) {
        final SpellAbility sa = jermane.getSpellAbilities().stream().filter(SpellAbility::isActivatedAbility)
                .toList().get(index);
        sa.setActivatingPlayer(p);
        sa.getTargets().add(target);
        return sa;
    }

    @Test
    public void testKnownLegs() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        AssertJUnit.assertTrue(printing("Armored Warhorse", "M12", "7", p).hasKnownFourLegs());
        AssertJUnit.assertFalse(printing("Llanowar Elves", "M19", "314", p).hasKnownFourLegs());
        AssertJUnit.assertFalse("the horse's legs, not the knight's", printing("Black Knight", "M10", "85", p).hasKnownFourLegs());
    }

    @Test
    public void testAbilitiesOnAKnownFourLeggedCreature() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card jermane = addCard("Jermane, Pride of the Circus", p);
        final Card horse = printing("Armored Warhorse", "M12", "7", p);
        AbilityUtils.resolve(ability(jermane, 0, p, horse));
        AssertJUnit.assertEquals(2, horse.getCounters(CounterEnumType.P1P1));
        AbilityUtils.resolve(ability(jermane, 1, p, horse));
        AssertJUnit.assertTrue(horse.hasKeyword("All creatures able to block CARDNAME do so."));
    }
}
