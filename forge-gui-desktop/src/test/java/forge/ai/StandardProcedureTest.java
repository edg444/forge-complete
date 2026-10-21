package forge.ai;

import java.util.Set;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.StaticData;
import forge.card.CardFacePredicates;
import forge.card.ICardFace;
import forge.game.Game;
import forge.game.GameFormat;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Standard Procedure (UNF #1), Oracle text and rulings checked against Scryfall 2026-10-20: it isn't a copy effect, it
 * becomes the chosen card until end of turn even after it's cast or shuffled away; it must still be in hand when the
 * ability resolves; the chosen card must be Standard-legal whatever the format; {X} counts as 0 for its mana value.
 */
public class StandardProcedureTest extends AITest {

    private ICardFace face(final String name) {
        return StaticData.instance().getCommonCards().getFaceByName(name);
    }

    @Test
    public void testFaceManaValueComparisons() {
        initAndCreateGame();
        AssertJUnit.assertTrue(CardFacePredicates.valid("Instant.cmcLE3").test(face("Lightning Bolt")));
        AssertJUnit.assertFalse(CardFacePredicates.valid("Instant.cmcLE3").test(face("Dig Through Time")));
        AssertJUnit.assertTrue(CardFacePredicates.valid("Instant.cmcGE8").test(face("Dig Through Time")));
        AssertJUnit.assertFalse(CardFacePredicates.valid("Sorcery.cmcLE3").test(face("Lightning Bolt")));
    }

    @Test
    public void testOnlyCurrentStandardCardsCanBeNamed() {
        initAndCreateGame();
        final GameFormat standard = GameFormat.currentStandard();
        AssertJUnit.assertNotNull(standard);
        final Set<String> legal = standard.getLegalFrontFaceNames();
        AssertJUnit.assertTrue(legal.contains("Shock"));
        AssertJUnit.assertTrue(legal.contains("Opt"));
        AssertJUnit.assertFalse(legal.contains("Lightning Bolt"));
        AssertJUnit.assertFalse(legal.contains("Brainstorm"));
    }

    @Test
    public void testBecomesTheCardInEveryZoneUntilEndOfTurn() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card proc = addCardToZone("Standard Procedure", p, ZoneType.Hand);
        final SpellAbility ability = proc.getFirstSpellAbility();
        ability.setActivatingPlayer(p);
        proc.addNamedCard("Shock");
        AbilityUtils.resolve(ability.getSubAbility());

        Card now = game.getCardState(proc);
        AssertJUnit.assertEquals("Shock", now.getName());
        AssertJUnit.assertEquals(ApiType.DealDamage, now.getFirstSpellAbility().getApi());

        game.getAction().moveToGraveyard(now, null, null);
        now = game.getCardState(proc);
        AssertJUnit.assertTrue(now.isInZone(ZoneType.Graveyard));
        AssertJUnit.assertEquals("Shock", now.getName());
        game.getAction().moveToLibrary(now, 0, null, null);
        now = game.getCardState(proc);
        AssertJUnit.assertEquals("shuffled away, it's still Shock", "Shock", now.getName());

        game.getEndOfTurn().executeUntil();
        now = game.getCardState(proc);
        AssertJUnit.assertEquals("Standard Procedure", now.getName());
        game.getAction().moveToHand(now, null, null);
        AssertJUnit.assertEquals("Standard Procedure", game.getCardState(proc).getName());
    }

    @Test
    public void testCastAsTheChosenCardItResolvesAndStaysThatCard() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCard("Mountain", p);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        final Card proc = addCardToZone("Standard Procedure", p, ZoneType.Hand);
        final SpellAbility ability = proc.getFirstSpellAbility();
        ability.setActivatingPlayer(p);
        proc.addNamedCard("Shock");
        AbilityUtils.resolve(ability.getSubAbility());

        final SpellAbility shock = game.getCardState(proc).getFirstSpellAbility();
        shock.setActivatingPlayer(p);
        shock.getTargets().add(opp);
        final int life = opp.getLife();
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(p, shock, null));
        AssertJUnit.assertEquals("Shock", game.getStack().peekAbility().getHostCard().getName());
        game.getStack().resolveStack();
        AssertJUnit.assertEquals(life - 2, opp.getLife());
        final Card after = game.getCardState(proc);
        AssertJUnit.assertTrue(after.isInZone(ZoneType.Graveyard));
        AssertJUnit.assertEquals("it went to the graveyard as Shock", "Shock", after.getName());
    }

    @Test
    public void testNothingHappensIfItLeftTheHand() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card proc = addCardToZone("Standard Procedure", p, ZoneType.Hand);
        final SpellAbility ability = proc.getFirstSpellAbility();
        ability.setActivatingPlayer(p);
        proc.addNamedCard("Shock");
        game.getAction().moveToGraveyard(proc, null, null);
        AbilityUtils.resolve(ability.getSubAbility());
        AssertJUnit.assertEquals("Standard Procedure", game.getCardState(proc).getName());
    }

    @Test
    public void testAiNamesAStandardSpellItWouldCast() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 4; i++) {
            addCard("Mountain", ai);
        }
        addCard("Grizzly Bears", opp);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card proc = addCardToZone("Standard Procedure", ai, ZoneType.Hand);
        final SpellAbility ability = proc.getFirstSpellAbility();
        ability.setActivatingPlayer(ai);

        AssertJUnit.assertTrue(forge.ai.SpecialCardAi.StandardProcedure.consider(ai, ability));
        final String named = forge.ai.SpecialCardAi.StandardProcedure.chooseName(ai,
                forge.ai.SpecialCardAi.StandardProcedure.candidates(3));
        AssertJUnit.assertFalse(named.isEmpty());
        AssertJUnit.assertTrue(named, GameFormat.currentStandard().getLegalFrontFaceNames().contains(named));
        AssertJUnit.assertTrue(named, face(named).getManaCost().getCMC() <= 3);
        AssertJUnit.assertTrue(named, face(named).getType().isInstant() || face(named).getType().isSorcery());
    }
}
