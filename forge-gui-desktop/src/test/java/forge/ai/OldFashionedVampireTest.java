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
import forge.game.zone.ZoneType;

/**
 * Old-Fashioned Vampire (Unstable): "Flying / This creature gets +2/+2 and has deathtouch as long as it's dark
 * outdoors." - Oracle text checked against Scryfall 2026-10-01 (no rulings, no FAQ entry). Honor system: any
 * player says whether it's dark outdoors, kept once for the whole game.
 */
public class OldFashionedVampireTest extends AITest {

    private static SpellAbility ability(Card c, ApiType api) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == api) {
                return sa;
            }
        }
        return null;
    }

    private static void say(Card vampire, ApiType api, Player who) {
        SpellAbility sa = ability(vampire, api);
        sa.setActivatingPlayer(who);
        AssertJUnit.assertTrue(sa.getRestrictions().checkActivatorRestrictions(vampire, sa));
        AssertJUnit.assertTrue(sa.getRestrictions().checkOtherRestrictions(vampire, sa, who));
        AbilityUtils.resolve(sa);
        vampire.getGame().getAction().checkStateEffects(true);
    }

    @Test
    public void testDarkOutdoorsForEveryVampire() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card mine = addCard("Old-Fashioned Vampire", me);
        Card theirs = addCard("Old-Fashioned Vampire", opp);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue(mine.getType().hasCreatureType("Vampyre"));
        AssertJUnit.assertTrue(mine.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertEquals(3, mine.getNetPower());
        AssertJUnit.assertFalse(mine.hasKeyword(Keyword.DEATHTOUCH));
        // only "it's dark" is on offer while it isn't
        SpellAbility light = ability(mine, ApiType.ChangeZoneAll);
        light.setActivatingPlayer(me);
        AssertJUnit.assertFalse(light.getRestrictions().checkOtherRestrictions(mine, light, me));

        // the opponent looks outside, on my Vampire; both Vampires get it
        say(mine, ApiType.Effect, opp);
        AssertJUnit.assertEquals(1, game.getCardsIn(ZoneType.Command).size());
        for (Card v : new Card[] {mine, theirs}) {
            AssertJUnit.assertEquals(5, v.getNetPower());
            AssertJUnit.assertEquals(5, v.getNetToughness());
            AssertJUnit.assertTrue(v.hasKeyword(Keyword.DEATHTOUCH));
        }
        SpellAbility dark = ability(theirs, ApiType.Effect);
        dark.setActivatingPlayer(me);
        AssertJUnit.assertFalse(dark.getRestrictions().checkOtherRestrictions(theirs, dark, me));

        // and back
        say(theirs, ApiType.ChangeZoneAll, me);
        AssertJUnit.assertEquals(0, game.getCardsIn(ZoneType.Command).size());
        for (Card v : new Card[] {mine, theirs}) {
            AssertJUnit.assertEquals(3, v.getNetPower());
            AssertJUnit.assertFalse(v.hasKeyword(Keyword.DEATHTOUCH));
        }
    }

    @Test
    public void testAiNeverSays() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card vampire = addCard("Old-Fashioned Vampire", ai);
        game.getAction().checkStateEffects(true);
        SpellAbility dark = ability(vampire, ApiType.Effect);
        dark.setActivatingPlayer(ai);
        for (int turn = 1; turn <= 50; turn++) {
            game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai, turn);
            AssertJUnit.assertFalse(SpellApiToAi.Converter.get(dark).canPlayWithSubs(ai, dark).willingToPlay());
        }
    }
}
