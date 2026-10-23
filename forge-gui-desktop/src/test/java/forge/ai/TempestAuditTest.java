package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityCantBeCast;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Tempest script review and R&D's Secret Lair printed text (2026-10-23). */
public class TempestAuditTest extends AITest {

    private static Card printing(final String name, final String set, final Player owner, final ZoneType zone) {
        final Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(name, set), owner);
        c.setGameTimestamp(owner.getGame().getNextTimestamp());
        owner.getZone(zone).add(c);
        return c;
    }

    private static SpellAbility spell(final Card c, final boolean printed) {
        for (SpellAbility s : c.getSpellAbilities()) {
            if (s.getDescription().contains("(As printed)") == printed) {
                return s;
            }
        }
        return null;
    }

    @Test
    public void testMagmasaurDealsDamageForItsCountersAfterSacrifice() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card saur = addCard("Magmasaur", p);
        saur.setCounters(CounterEnumType.P1P1, 3);
        Card bear = addCard("Grizzly Bears", p);
        game.getAction().checkStateEffects(true);
        SpellAbility sa = AbilityFactory.getAbility("DB$ Sacrifice | SubAbility$ LetEmHaveIt", saur);
        sa.setActivatingPlayer(p);
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(17, p.getLife());
        AssertJUnit.assertTrue(p.getCardsIn(ZoneType.Graveyard).contains(bear));
    }

    @Test
    public void testHandToHandStopsManaAbilitiesAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("R&D's Secret Lair", p);
        Card forest = addCard("Forest", p);
        printing("Hand to Hand", "TMP", p, ZoneType.Battlefield);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, p);
        game.getAction().checkStateEffects(true);
        SpellAbility mana = forest.getManaAbilities().getFirst();
        mana.setActivatingPlayer(p);
        AssertJUnit.assertTrue(StaticAbilityCantBeCast.cantBeActivatedAbility(mana, forest, p));
    }

    @Test
    public void testHandToHandAllowsManaAbilitiesByOracle() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card forest = addCard("Forest", p);
        printing("Hand to Hand", "TMP", p, ZoneType.Battlefield);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, p);
        game.getAction().checkStateEffects(true);
        SpellAbility mana = forest.getManaAbilities().getFirst();
        mana.setActivatingPlayer(p);
        AssertJUnit.assertFalse(StaticAbilityCantBeCast.cantBeActivatedAbility(mana, forest, p));
    }

    @Test
    public void testFlickeringWardFallsOffOnWhiteAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("R&D's Secret Lair", p);
        Card bear = addCard("Grizzly Bears", p);
        Card ward = printing("Flickering Ward", "TMP", p, ZoneType.Battlefield);
        ward.setChosenColors(java.util.List.of("white"));
        ward.attachToEntity(bear, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(p.getCardsIn(ZoneType.Graveyard).contains(ward));
    }

    @Test
    public void testFlickeringWardStaysOnWhiteByOracle() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card bear = addCard("Grizzly Bears", p);
        Card ward = printing("Flickering Ward", "TMP", p, ZoneType.Battlefield);
        ward.setChosenColors(java.util.List.of("white"));
        ward.attachToEntity(bear, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(ward.isInPlay());
    }

    @Test
    public void testBloodFrenzyAfterCombatDamageAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("R&D's Secret Lair", p);
        Card frenzy = printing("Blood Frenzy", "TMP", p, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_END, p);
        SpellAbility printed = spell(frenzy, true);
        SpellAbility oracle = spell(frenzy, false);
        printed.setActivatingPlayer(p);
        oracle.setActivatingPlayer(p);
        AssertJUnit.assertTrue(printed.getRestrictions().canPlay(frenzy, printed));
        AssertJUnit.assertFalse(oracle.getRestrictions().canPlay(frenzy, oracle));
    }

    @Test
    public void testBloodFrenzyOracleOnlyWithoutLair() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card frenzy = printing("Blood Frenzy", "TMP", p, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, p);
        SpellAbility printed = spell(frenzy, true);
        SpellAbility oracle = spell(frenzy, false);
        printed.setActivatingPlayer(p);
        oracle.setActivatingPlayer(p);
        AssertJUnit.assertFalse(printed.getRestrictions().canPlay(frenzy, printed));
        AssertJUnit.assertTrue(oracle.getRestrictions().canPlay(frenzy, oracle));
    }

    @Test
    public void testMindwhipSliverAbilityIsSorcerySpeed() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card sliver = addCard("Mindwhip Sliver", p);
        game.getAction().checkStateEffects(true);
        boolean found = false;
        for (SpellAbility s : sliver.getSpellAbilities()) {
            if (s.getDescription().contains("discards a card at random")) {
                found = true;
                AssertJUnit.assertTrue(s.getRestrictions().isSorcerySpeed());
            }
        }
        AssertJUnit.assertTrue(found);
    }

    private static boolean shapeshifterFlies(final Game game, final Player p, final Player opp) {
        Card theirs = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Escaped Shapeshifter"), opp);
        opp.getZone(ZoneType.Battlefield).add(theirs);
        Card flight = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Flight"), opp);
        opp.getZone(ZoneType.Battlefield).add(flight);
        flight.attachToEntity(theirs, null);
        Card mine = printing("Escaped Shapeshifter", "TMP", p, ZoneType.Battlefield);
        game.getAction().checkStateEffects(true);
        return mine.hasKeyword(forge.game.keyword.Keyword.FLYING);
    }

    @Test
    public void testEscapedShapeshifterCopiesAnotherShapeshifterAsPrinted() {
        Game game = initAndCreateGame();
        addCard("R&D's Secret Lair", game.getPlayers().get(1));
        AssertJUnit.assertTrue(shapeshifterFlies(game, game.getPlayers().get(1), game.getPlayers().get(0)));
    }

    @Test
    public void testEscapedShapeshifterIgnoresAnotherShapeshifterByOracle() {
        Game game = initAndCreateGame();
        AssertJUnit.assertFalse(shapeshifterFlies(game, game.getPlayers().get(1), game.getPlayers().get(0)));
    }
}
