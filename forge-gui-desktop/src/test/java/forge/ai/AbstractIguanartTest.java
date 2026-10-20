package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.effects.NoteArtistInitialsEffect;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/**
 * Abstract Iguanart (UND #47), checked against Scryfall and its 4 rulings 2026-10-20.
 */
public class AbstractIguanartTest extends AITest {

    private static void cast(final Game game, final Player p, final String name, final String set, final forge.game.GameObject target) {
        Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(name, set), p);
        p.getZone(ZoneType.Hand).add(c);
        game.getAction().checkStateEffects(true);
        SpellAbility spell = c.getFirstSpellAbility();
        spell.setActivatingPlayer(p);
        spell.getTargets().add(target);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(p, spell, null));
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    @Test
    public void initialsOfEachArtist() {
        AssertJUnit.assertEquals(List.of("Z", "G"), NoteArtistInitialsEffect.initials("Zoltan Boros & Gabor Szikszai"));
        AssertJUnit.assertEquals(List.of("R"), NoteArtistInitialsEffect.initials("Rebecca \"Don't Mess with Me\" Guay"));
        AssertJUnit.assertTrue(NoteArtistInitialsEffect.initials("").isEmpty());
    }

    @Test
    public void newLettersGrowIt() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        addCards("Mountain", 2, ai);
        addCards("Forest", 1, ai);
        Card iguana = addCard("Abstract Iguanart", ai);
        game.getAction().checkStateEffects(true);

        cast(game, ai, "Lightning Bolt", "M10", game.getPlayers().get(0)); // Christopher Moeller
        AssertJUnit.assertEquals(1, iguana.getCounters(CounterEnumType.P1P1));
        cast(game, ai, "Lightning Bolt", "M10", game.getPlayers().get(0));
        AssertJUnit.assertEquals("C was already noted", 1, iguana.getCounters(CounterEnumType.P1P1));
        cast(game, ai, "Giant Growth", "M10", iguana); // Matt Cavotta
        AssertJUnit.assertEquals(2, iguana.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(List.of("C", "M"), iguana.getNotedTypes());
    }
}
