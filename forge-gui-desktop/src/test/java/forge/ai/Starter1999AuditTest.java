package forge.ai;

import java.util.ArrayList;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Starter 1999 script review and R&D's Secret Lair printed text (2026-10-23). */
public class Starter1999AuditTest extends AITest {

    private static final String[] LIBRARY = {"Plains", "Island", "Swamp", "Mountain", "Forest", "Wastes",
        "Snow-Covered Plains", "Snow-Covered Island", "Snow-Covered Swamp", "Snow-Covered Mountain"};

    private boolean libraryOrderKept(final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        for (String name : LIBRARY) {
            addCardToZone(name, p, ZoneType.Library);
        }
        Card tutor = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Grim Tutor", "S99"), p);
        p.getZone(ZoneType.Hand).add(tutor);
        SpellAbility sa = null;
        for (SpellAbility s : tutor.getSpellAbilities()) {
            s.setActivatingPlayer(p);
            if (s.getRestrictions().canPlay(tutor, s)) {
                sa = s;
            }
        }
        List<String> before = new ArrayList<>();
        for (Card c : p.getCardsIn(ZoneType.Library)) {
            before.add(c.getName());
        }
        sa.setHostCard(game.getAction().moveToStack(tutor, sa));
        AbilityUtils.resolve(sa);
        List<String> after = new ArrayList<>();
        for (Card c : p.getCardsIn(ZoneType.Library)) {
            after.add(c.getName());
        }
        before.retainAll(after);
        return before.equals(after);
    }

    @Test
    public void testGrimTutorDoesNotShuffleAsPrinted() {
        AssertJUnit.assertTrue(libraryOrderKept(true));
    }

    @Test
    public void testGrimTutorShufflesByOracle() {
        AssertJUnit.assertFalse(libraryOrderKept(false));
    }
}
