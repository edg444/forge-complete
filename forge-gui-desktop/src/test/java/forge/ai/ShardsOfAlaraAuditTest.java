package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Shards of Alara script review and R&D's Secret Lair printed text (2026-10-23). */
public class ShardsOfAlaraAuditTest extends AITest {

    /** Returns {creature you had when the ultimate resolved, creature that arrived afterwards}. */
    private Card[] elspethUltimate(final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card elspeth = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Elspeth, Knight-Errant", "ALA"), p);
        p.getZone(ZoneType.Battlefield).add(elspeth);
        elspeth.setCounters(CounterEnumType.LOYALTY, 8);
        Card early = addCard("Grizzly Bears", p);
        game.getAction().checkStateEffects(true);
        for (SpellAbility sa : elspeth.getSpellAbilities()) {
            sa.setActivatingPlayer(p);
            if (sa.isActivatedAbility() && sa.getApi().name().equals("Effect")
                    && sa.getRestrictions().checkOtherRestrictions(elspeth, sa, p)) {
                AbilityUtils.resolve(sa);
                break;
            }
        }
        Card late = addCard("Hill Giant", p);
        game.getAction().checkStateEffects(true);
        return new Card[] { early, late };
    }

    @Test
    public void testElspethEmblemCoversLaterPermanents() {
        Card[] c = elspethUltimate(false);
        AssertJUnit.assertTrue(c[0].hasKeyword(Keyword.INDESTRUCTIBLE));
        AssertJUnit.assertTrue(c[1].hasKeyword(Keyword.INDESTRUCTIBLE));
    }

    @Test
    public void testElspethPrintedUltimateLocksInItsPermanents() {
        Card[] c = elspethUltimate(true);
        AssertJUnit.assertTrue(c[0].hasKeyword(Keyword.INDESTRUCTIBLE));
        AssertJUnit.assertFalse(c[1].hasKeyword(Keyword.INDESTRUCTIBLE));
    }
}
