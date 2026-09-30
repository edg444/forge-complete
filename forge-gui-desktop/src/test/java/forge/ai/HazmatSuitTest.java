package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Hazmat Suit (Used) (Unstable): "Enchant creature / Enchanted creature gets +2/+1 and has menace. / Whenever a
 * player's skin or fingernail touches enchanted creature, that player loses 2 life." - Oracle text checked against
 * Scryfall 2026-09-30. The touching is honor system: any player may own up to it.
 */
public class HazmatSuitTest extends AITest {

    private static SpellAbility touched(Card c) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == ApiType.LoseLife) {
                return sa;
            }
        }
        return null;
    }

    @Test
    public void testBonusAndWhoeverTouchesItLosesLife() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card bears = addCard("Grizzly Bears", me);
        Card suit = addCard("Hazmat Suit (Used)", me);
        suit.attachToEntity(bears, null);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertEquals(4, bears.getNetPower());
        AssertJUnit.assertEquals(3, bears.getNetToughness());
        AssertJUnit.assertTrue(bears.hasKeyword(Keyword.MENACE));

        SpellAbility sa = touched(suit);
        AssertJUnit.assertEquals("Whenever a player's skin or fingernail touches enchanted creature, that player "
                + "loses 2 life.", sa.getDescription().trim());
        AssertJUnit.assertEquals("Enchant creature | Enchanted creature gets +2/+1 and has menace. | "
                + sa.getDescription().trim(), String.join(" | ", suit.getAbilityText().trim().split("[\r\n]+")));

        for (Player toucher : new Player[] {opp, me}) {
            sa.setActivatingPlayer(toucher);
            AssertJUnit.assertTrue(sa.getRestrictions().checkActivatorRestrictions(suit, sa));
            AbilityUtils.resolve(sa);
            AssertJUnit.assertEquals(18, toucher.getLife());
        }
    }

    @Test
    public void testOnlyTheAiHandlingTheCreatureTouchesIt() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card bears = addCard("Grizzly Bears", me);
        Card suit = addCard("Hazmat Suit (Used)", me);
        suit.attachToEntity(bears, null);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = touched(suit);
        int mine = 0;
        int theirs = 0;
        // the roll is made once a turn, so look at a lot of turns
        for (int turn = 1; turn <= 300; turn++) {
            game.getPhaseHandler().devModeSet(PhaseType.MAIN1, me, turn);
            sa.setActivatingPlayer(me);
            if (SpellApiToAi.Converter.get(sa).canPlayWithSubs(me, sa).willingToPlay()) {
                mine++;
            }
            // the AI owns up in its own main phase
            game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp, turn);
            sa.setActivatingPlayer(opp);
            if (SpellApiToAi.Converter.get(sa).canPlayWithSubs(opp, sa).willingToPlay()) {
                theirs++;
            }
        }
        AssertJUnit.assertTrue("controller touched it " + mine + " turns in 300", mine > 10 && mine < 60);
        AssertJUnit.assertEquals(0, theirs);

        // the Suit can be on someone else's creature: then it's that player who handles it
        Card giant = addCard("Hill Giant", opp);
        suit.attachToEntity(giant, null);
        game.getAction().checkStateEffects(true);
        mine = 0;
        theirs = 0;
        for (int turn = 1; turn <= 300; turn++) {
            game.getPhaseHandler().devModeSet(PhaseType.MAIN1, me, turn);
            sa.setActivatingPlayer(me);
            if (SpellApiToAi.Converter.get(sa).canPlayWithSubs(me, sa).willingToPlay()) {
                mine++;
            }
            // the AI owns up in its own main phase
            game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp, turn);
            sa.setActivatingPlayer(opp);
            if (SpellApiToAi.Converter.get(sa).canPlayWithSubs(opp, sa).willingToPlay()) {
                theirs++;
            }
        }
        AssertJUnit.assertEquals(0, mine);
        AssertJUnit.assertTrue("creature's controller touched it " + theirs + " turns in 300", theirs > 10 && theirs < 60);
    }
}
