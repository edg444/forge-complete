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

/** Mirage script review and R&D's Secret Lair printed text (2026-10-23). */
public class MirageAuditTest extends AITest {

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
    public void testRadiantEssenceCountsAnyBlackPermanent() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card essence = addCard("Radiant Essence", p);
        addCard("Bad Moon", opp);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(3, essence.getNetPower());
    }

    @Test
    public void testLionsEyeDiamondIsAManaSourceAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card led = printing("Lion's Eye Diamond", "MIR", p, ZoneType.Battlefield);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(usable(led, ApiType.Mana, p).getRestrictions().isInstantSpeed());
        addCard("R&D's Secret Lair", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(usable(led, ApiType.Mana, p).getRestrictions().isInstantSpeed());
    }

    @Test
    public void testPsychicTransferNeedsLifeAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("R&D's Secret Lair", p);
        p.setLife(0, null);
        opp.setLife(3, null);
        Card transfer = printing("Psychic Transfer", "MIR", p, ZoneType.Hand);
        SpellAbility sa = transfer.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        sa.setHostCard(game.getAction().moveToStack(transfer, sa));
        sa.getTargets().add(opp);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertEquals("no exchange at 0 life", 0, p.getLife());

        Card modern = printing("Psychic Transfer", "6ED", p, ZoneType.Hand);
        SpellAbility sa6 = modern.getFirstSpellAbility();
        sa6.setActivatingPlayer(p);
        sa6.setHostCard(game.getAction().moveToStack(modern, sa6));
        sa6.getTargets().add(opp);
        AbilityUtils.resolve(sa6);
        AssertJUnit.assertEquals(3, p.getLife());
    }

    @Test
    public void testForbiddenCryptLetsCreaturesDieAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("R&D's Secret Lair", p);
        printing("Forbidden Crypt", "MIR", p, ZoneType.Battlefield);
        Card bear = addCard("Grizzly Bears", p);
        game.getAction().checkStateEffects(true);
        Player opp = game.getPlayers().get(0);
        addCard("Zulaport Cutthroat", p);
        game.getAction().checkStateEffects(true);
        game.copyLastState();
        bear.setDamage(2);
        resolveAll(game);
        AssertJUnit.assertEquals("it died, so Zulaport Cutthroat drained", 19, opp.getLife());
        AssertJUnit.assertEquals(1, p.getCardsIn(ZoneType.Exile).size());
        AssertJUnit.assertTrue(p.getCardsIn(ZoneType.Graveyard).isEmpty());
    }
}
