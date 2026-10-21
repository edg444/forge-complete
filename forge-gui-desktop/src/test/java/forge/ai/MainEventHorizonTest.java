package forge.ai;

import java.util.ArrayList;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.Card;
import forge.game.card.NameLetters;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Main Event Horizon (UNF #19), checked against Scryfall and its 3 rulings 2026-10-21: the half of the alphabet is
 * chosen as it's cast; names skip spaces and punctuation; letters outside A to Z are in neither half.
 */
public class MainEventHorizonTest extends AITest {

    private SpellAbility half(final Card spell, final Player p, final String which) {
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        for (final AbilitySub mode : CharmEffect.makePossibleOptions(sa)) {
            if (mode.getDescription().startsWith(which)) {
                CharmEffect.chainAbilities(sa, new ArrayList<>(List.of(mode)));
                return sa;
            }
        }
        throw new AssertionError(which);
    }

    @Test
    public void testDestroysTheChosenHalf() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card elves = addCard("Llanowar Elves", opp);
        final Card goblin = addCard("_____ Goblin", opp);
        final Card serra = addCard("Serra Angel", p);
        addCard("Far Out", p);
        final Card spell = addCardToZone("Main Event Horizon", p, ZoneType.Hand);
        final SpellAbility cast = spell.getFirstSpellAbility();
        cast.setActivatingPlayer(p);
        AssertJUnit.assertFalse("not modal, so Far Out can't add the other half",
                CharmEffect.canChooseAnyNumberOfModes(cast, p, 2, 1, 1, false));
        AbilityUtils.resolve(half(spell, p, "A through M"));
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(game.getCardState(elves).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue("_____ Goblin begins with G", game.getCardState(goblin).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue(game.getCardState(serra).isInZone(ZoneType.Battlefield));
    }

    @Test
    public void testLettersOutsideTheAlphabet() {
        AssertJUnit.assertEquals('E', NameLetters.latinBase(NameLetters.initial("Éowyn, Lady of Rohan")));
        AssertJUnit.assertEquals('A', NameLetters.latinBase(NameLetters.initial("Æther Vial")));
        AssertJUnit.assertEquals(-1, NameLetters.latinBase(NameLetters.initial("Ωmega")));
        AssertJUnit.assertEquals(-1, NameLetters.latinBase(NameLetters.initial("____________")));
    }

    @Test
    public void testTheAiPicksTheHalfThatHurtsItsOpponent() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCard("Serra Angel", opp);
        addCard("Shivan Dragon", opp);
        addCard("Llanowar Elves", ai);
        addCard("Grizzly Bears", ai);
        for (int i = 0; i < 5; i++) {
            addCard("Plains", ai);
        }
        final Card spell = addCardToZone("Main Event Horizon", ai, ZoneType.Hand);
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa));
        AssertJUnit.assertTrue(sa.getSubAbility().getDescription().startsWith("N through Z"));
    }
}
