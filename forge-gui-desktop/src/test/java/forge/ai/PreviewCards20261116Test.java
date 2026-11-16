package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Previewed cards scripted ahead of release (2026-11-16): Nauctis: The Sunken Realm and Marvel: Darkhold Destiny's
 * Temple of Khonshu.
 */
public class PreviewCards20261116Test extends AITest {

    private void resolveAll(Game game) {
        do {
            playUntilStackClear(game);
            game.getAction().checkStateEffects(true);
        } while (game.getStack().addAllTriggeredAbilitiesToStack());
    }

    private Game mainPhaseGame() {
        return mainPhaseGame(true);
    }

    private Game mainPhaseGame(boolean fillAiLibrary) {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        if (fillAiLibrary) {
            fillLibrary(ai, 5);
        }
        fillLibrary(game.getPlayers().get(0), 5);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        return game;
    }

    private SpellAbility drawAbility(Card c) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == ApiType.Draw) {
                return sa;
            }
        }
        return null;
    }

    private void cast(Player ai, Card c) {
        SpellAbility sa = c.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, sa, null));
    }

    @Test
    public void templeOfKhonshuEntersTappedWithoutADimension() {
        Game game = mainPhaseGame();
        Player ai = game.getPlayers().get(1);
        Card temple = addCardToZone("Temple of Khonshu", ai, ZoneType.Hand);
        temple = game.getAction().moveToPlay(temple, ai, null, null);
        AssertJUnit.assertTrue(temple.isTapped());
    }

    @Test
    public void calmwaterMentorFindsTwoSmallCreatures() {
        Game game = mainPhaseGame();
        Player ai = game.getPlayers().get(1);
        addCards("Plains", 4, ai);
        addCardToZone("Hill Giant", ai, ZoneType.Library);
        addCardToZone("Llanowar Elves", ai, ZoneType.Library);
        addCardToZone("Grizzly Bears", ai, ZoneType.Library);
        cast(ai, addCardToZone("Calmwater Mentor", ai, ZoneType.Hand));
        resolveAll(game);
        // the filler library is Runeclaw Bears, power 2 as well
        AssertJUnit.assertEquals(2, ai.getCardsIn(ZoneType.Hand).size());
        for (Card c : ai.getCardsIn(ZoneType.Hand)) {
            AssertJUnit.assertTrue(c.isCreature() && c.getNetPower() <= 2);
        }
    }

    @Test
    public void salinaeCopiesAnArtifactAsAOneOneFish() {
        Game game = mainPhaseGame();
        Player ai = game.getPlayers().get(1);
        addCard("Salinae, the Ocean Mother", ai);
        addCards("Island", 2, ai);
        cast(ai, addCardToZone("Mind Stone", ai, ZoneType.Hand));
        resolveAll(game);
        Card copy = null;
        for (Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.getName().equals("Mind Stone") && c.isToken()) {
                copy = c;
            }
        }
        AssertJUnit.assertNotNull(copy);
        AssertJUnit.assertTrue(copy.isArtifact() && copy.isCreature() && copy.getType().hasCreatureType("Fish"));
        AssertJUnit.assertEquals(1, copy.getNetPower());
        AssertJUnit.assertEquals(1, copy.getNetToughness());
    }

    @Test
    public void tectonDrawsWhenYouSacrificeAnotherPermanent() {
        Game game = mainPhaseGame();
        Player ai = game.getPlayers().get(1);
        addCard("Tecton, Malignant Ruin", ai);
        addCard("Island", ai);
        Card stone = addCard("Mind Stone", ai);
        int hand = ai.getCardsIn(ZoneType.Hand).size();
        SpellAbility sac = drawAbility(stone);
        sac.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, sac, null));
        resolveAll(game);
        AssertJUnit.assertEquals(hand + 2, ai.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void goramouPutsADiscardedLandOntoTheBattlefieldAtTheEndStep() {
        Game game = mainPhaseGame(false);
        Player ai = game.getPlayers().get(1);
        addCard("Goramou the Coral-Taken", ai);
        addCard("Mountain", ai);
        addCardToZone("Forest", ai, ZoneType.Hand);
        addCardToZone("Forest", ai, ZoneType.Library);
        addCardToZone("Forest", ai, ZoneType.Library);
        cast(ai, addCardToZone("Faithless Looting", ai, ZoneType.Hand));
        resolveAll(game);
        AssertJUnit.assertEquals(1, countCardsWithName(game, "Forest", ZoneType.Exile));
        playUntilPhase(game, PhaseType.END_OF_TURN);
        resolveAll(game);
        Card forest = null;
        for (Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.getName().equals("Forest")) {
                forest = c;
            }
        }
        AssertJUnit.assertNotNull(forest);
        AssertJUnit.assertTrue(forest.isTapped());
    }

    @Test
    public void abyssalLotusNeedsEightPermanentCards() {
        Game game = mainPhaseGame();
        Player ai = game.getPlayers().get(1);
        Card lotus = addCard("Abyssal Lotus", ai);
        lotus.setTapped(false);
        SpellAbility mana = lotus.getManaAbilities().get(0);
        mana.setActivatingPlayer(ai);
        addCards("Island", 7, ai).forEach(c -> game.getAction().moveToGraveyard(c, null));
        AssertJUnit.assertFalse(mana.canPlay());
        game.getAction().moveToGraveyard(addCard("Island", ai), null);
        AssertJUnit.assertTrue(mana.canPlay());
    }

    @Test
    public void ringOfUnityGivesArtifactsAColorlessPair() {
        Game game = mainPhaseGame();
        Player ai = game.getPlayers().get(1);
        Card stone = addCard("Mind Stone", ai);
        addCard("Ring of Unity", ai);
        game.getAction().checkStateEffects(true);
        boolean found = false;
        for (SpellAbility sa : stone.getManaAbilities()) {
            found |= sa.getManaPart().getOrigProduced().equals("C") && "2".equals(sa.getParam("Amount"));
        }
        AssertJUnit.assertTrue(found);
    }

    @Test
    public void shimmeringCitadelExhaustsOnce() {
        Game game = mainPhaseGame();
        Player ai = game.getPlayers().get(1);
        Card citadel = addCard("Shimmering Citadel", ai);
        citadel.setTapped(false);
        addCards("Plains", 2, ai);
        addCards("Island", 2, ai);
        addCard("Tundra", ai);
        int hand = ai.getCardsIn(ZoneType.Hand).size();
        int life = ai.getLife();
        SpellAbility draw = drawAbility(citadel);
        draw.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, draw, null));
        resolveAll(game);
        AssertJUnit.assertEquals(hand + 2, ai.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(life + 1, ai.getLife());
        AssertJUnit.assertEquals(1, citadel.getCounters(CounterEnumType.DEPLETION));
        citadel.setTapped(false);
        AssertJUnit.assertFalse(draw.canPlay());
    }

    @Test
    public void khodBuffsCephalidsLikeLearnedLearner() {
        Game game = mainPhaseGame();
        Player ai = game.getPlayers().get(1);
        Card learner = addCard("Learned Learner", ai);
        addCard("Khod, Etlan Shiis Envoy", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(learner.getType().hasCreatureType("Cephalid"));
        AssertJUnit.assertEquals(1, learner.getNetPower());
        AssertJUnit.assertEquals(4, learner.getNetToughness());
    }
}
