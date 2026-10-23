package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Homelands script review and R&D's Secret Lair printed text (2026-10-23). */
public class HomelandsAuditTest extends AITest {

    private static Card printing(final String name, final String set, final Player owner, final ZoneType zone) {
        final Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(name, set), owner);
        c.setGameTimestamp(owner.getGame().getNextTimestamp());
        owner.getZone(zone).add(c);
        return c;
    }

    private static void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    /** The one ability of this API whose activation restrictions currently pass. */
    private static SpellAbility usable(final Card c, final ApiType api, final Player p) {
        SpellAbility found = null;
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() != api) {
                continue;
            }
            sa.setActivatingPlayer(p);
            if (sa.getRestrictions().canPlay(c, sa)) {
                AssertJUnit.assertNull("only one version should be usable", found);
                found = sa;
            }
        }
        return found;
    }

    @Test
    public void testFaerieNobleIncludesItselfAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card noble = printing("Faerie Noble", "HML", p, ZoneType.Battlefield);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(2, noble.getNetToughness());
        addCard("R&D's Secret Lair", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(3, noble.getNetToughness());
        AbilityUtils.resolve(usable(noble, ApiType.PumpAll, p));
        AssertJUnit.assertEquals(2, noble.getNetPower());
    }

    @Test
    public void testAnabaAncestorAndBaronSengirTargetThemselvesAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card ancestor = printing("Anaba Ancestor", "HML", p, ZoneType.Battlefield);
        Card baron = printing("Baron Sengir", "HML", p, ZoneType.Battlefield);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(usable(ancestor, ApiType.Pump, p).canTarget(ancestor));
        AssertJUnit.assertFalse(usable(baron, ApiType.Regenerate, p).canTarget(baron));
        addCard("R&D's Secret Lair", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(usable(ancestor, ApiType.Pump, p).canTarget(ancestor));
        AssertJUnit.assertTrue(usable(baron, ApiType.Regenerate, p).canTarget(baron));
    }

    @Test
    public void testSengirAutocratBuriesSerfsAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("R&D's Secret Lair", p);
        Card autocrat = printing("Sengir Autocrat", "HML", p, ZoneType.Battlefield);
        SpellAbility serfs = autocrat.getTriggers().get(0).ensureAbility();
        serfs.setActivatingPlayer(p);
        AbilityUtils.resolve(serfs);
        resolveAll(game);
        AssertJUnit.assertEquals(4, p.getCreaturesInPlay().size());
        SpellAbility leaves = autocrat.getTriggers().get(1).ensureAbility();
        leaves.setActivatingPlayer(p);
        AbilityUtils.resolve(leaves);
        resolveAll(game);
        AssertJUnit.assertEquals(1, p.getCreaturesInPlay().size());
        AssertJUnit.assertEquals("the Serfs died rather than being exiled", 3, AbilityUtils.calculateAmount(autocrat,
                "Count$ThisTurnEntered_Graveyard_from_Battlefield_Creature.token", null));
    }

    @Test
    public void testApocalypseChimeTakesHomelandsPrintingsAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("R&D's Secret Lair", p);
        Card chime = printing("Apocalypse Chime", "HML", p, ZoneType.Battlefield);
        Card homelands = printing("Abbey Gargoyles", "HML", p, ZoneType.Battlefield);
        Card fifth = printing("Abbey Gargoyles", "5ED", p, ZoneType.Battlefield);
        game.getAction().checkStateEffects(true);
        SpellAbility sa = chime.getSpellAbilities().stream().filter(s -> s.getApi() == ApiType.DestroyAll).findFirst().get();
        sa.setActivatingPlayer(p);
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(homelands.isInPlay());
        AssertJUnit.assertTrue(fifth.isInPlay());
    }
}
