package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Phone a Friend (UNF #55), checked against Scryfall and its 7 rulings 2026-10-21: someone outside the game picks a
 * legal mode; unanswered, an opponent picks. The AI's friend is simulated (user, 2026-10-21).
 */
public class PhoneAFriendTest extends AITest {

    @Test
    public void testTheFriendPicksALegalMode() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 12; i++) {
            addCardToZone("Island", ai, ZoneType.Library);
        }
        final Card spell = addCardToZone("Phone a Friend", ai, ZoneType.Hand);
        for (int i = 0; i < 20; i++) {
            final SpellAbility sa = spell.getFirstSpellAbility();
            sa.setActivatingPlayer(ai);
            sa.setSubAbility(null);
            sa.setChosenList(null);
            AssertJUnit.assertTrue(CharmEffect.makeChoices(sa));
            final AbilitySub chosen = sa.getSubAbility();
            AssertJUnit.assertNotNull(chosen);
            // no creatures anywhere, so only C and D are legal
            AssertJUnit.assertTrue(chosen.getApi() == ApiType.AddTurn || chosen.getApi() == ApiType.Draw);
        }
    }

    @Test
    public void testTheAiOpponentPicksWhatHelpsTheCasterLeast() {
        final Game game = initAndCreateGame();
        final Player caster = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        addCardToZone("Island", caster, ZoneType.Library);
        final Card spell = addCardToZone("Phone a Friend", caster, ZoneType.Hand);
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(caster);
        sa.setChoosingPlayer(ai);
        final List<AbilitySub> choices = CharmEffect.makePossibleOptions(sa);
        final List<AbilitySub> picked = ai.getController().chooseModeForAbility(sa, choices, 1, 1, false);
        AssertJUnit.assertEquals("one card in their library: draw seven", ApiType.Draw, picked.get(0).getApi());
    }
}
