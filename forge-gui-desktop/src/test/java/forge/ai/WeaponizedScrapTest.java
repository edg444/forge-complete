package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Weaponized Scrap (CMB1 #111), checked against Scryfall and its 6 rulings 2026-10-15: upgrade covers another
 * artifact you control as it would enter - the artifact carries on, tapped and with its counters, with only the
 * card on top counting, plus haste - or exiles it if it can't.
 */
public class WeaponizedScrapTest extends AITest {

    @Test
    public void coversAnArtifactAndCarriesItOn() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card thopter = addCard("Ornithopter", ai);
        thopter.setTapped(true);
        thopter.addCounterInternal(CounterEnumType.P1P1, 1, ai, false, null, null);
        Card scrap = addCardToZone("Weaponized Scrap", ai, ZoneType.Hand);

        game.getAction().moveToPlay(scrap, ai, null, AbilityKey.newMap());
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue(thopter.isInPlay());
        AssertJUnit.assertEquals("Weaponized Scrap", thopter.getName());
        AssertJUnit.assertEquals(7, thopter.getNetPower());
        AssertJUnit.assertTrue(thopter.isTapped());
        AssertJUnit.assertTrue(thopter.hasKeyword(Keyword.HASTE));
        AssertJUnit.assertFalse("the covered artifact is ignored", thopter.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertTrue(game.getCardState(scrap).isInZone(ZoneType.Merged));
        AssertJUnit.assertEquals(1, ai.getCardsIn(ZoneType.Battlefield).size());

        game.getAction().destroy(thopter, null, false, AbilityKey.newMap());
        AssertJUnit.assertEquals("both cards go", 2, ai.getCardsIn(ZoneType.Graveyard).size());
    }

    @Test
    public void exiledWithNothingToCover() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card scrap = addCardToZone("Weaponized Scrap", ai, ZoneType.Hand);
        game.getAction().moveToPlay(scrap, ai, null, AbilityKey.newMap());
        AssertJUnit.assertTrue(game.getCardState(scrap).isInZone(ZoneType.Exile));
        AssertJUnit.assertTrue(ai.getCardsIn(ZoneType.Battlefield).isEmpty());
    }

    @Test
    public void aiCoversItsLeastAndWontCastIntoExile() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        for (int i = 0; i < 4; i++) {
            addCard("Island", ai);
        }
        Card scrap = addCardToZone("Weaponized Scrap", ai, ZoneType.Hand);
        SpellAbility cast = scrap.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(cast));

        Card thopter = addCard("Ornithopter", ai);
        Card treasure = addToken("c_a_treasure_sac", ai);
        game.getAction().moveToPlay(scrap, ai, null, AbilityKey.newMap());
        AssertJUnit.assertEquals("the token is covered", "Weaponized Scrap", treasure.getName());
        AssertJUnit.assertEquals("Ornithopter", thopter.getName());
    }
}
