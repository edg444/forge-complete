package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.combat.CombatUtil;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.IPaperCard;
import forge.model.FModel;

/**
 * Phoebe, Head of S.N.E.A.K. (Unstable) - Oracle text, the seven Scryfall rulings and the Unstable FAQ entry
 * checked 2026-10-06.
 */
public class PhoebeTest extends AITest {

    private static SpellAbility steal(Card phoebe) {
        for (SpellAbility sa : phoebe.getSpellAbilities()) {
            if (sa.getApi() == ApiType.StealTextBox) {
                return sa;
            }
        }
        return null;
    }

    private void stealFrom(Game game, Card phoebe, Card victim) {
        SpellAbility sa = steal(phoebe);
        sa.setActivatingPlayer(phoebe.getController());
        sa.resetTargets();
        sa.getTargets().add(victim);
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);
    }

    private Card ust(String name, Player p) {
        IPaperCard pc = FModel.getMagicDb().getCommonCards().getCard(name, "UST");
        Card c = Card.fromPaperCard(pc, p);
        c.setGameTimestamp(p.getGame().getNextTimestamp());
        p.getZone(ZoneType.Battlefield).add(c);
        return c;
    }

    @Test
    public void testTheTextBoxChangesHands() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card phoebe = addCard("Phoebe, Head of S.N.E.A.K.", me);
        Card angel = addCard("Serra Angel", opp);
        game.getAction().checkStateEffects(true);
        stealFrom(game, phoebe, angel);

        AssertJUnit.assertFalse(angel.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertFalse(angel.hasKeyword(Keyword.VIGILANCE));
        AssertJUnit.assertTrue(phoebe.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertTrue(phoebe.hasKeyword(Keyword.VIGILANCE));
        AssertJUnit.assertTrue(angel.getTextBoxOracle().isEmpty());
        AssertJUnit.assertTrue(angel.getFlavorText().isEmpty());
        AssertJUnit.assertTrue(phoebe.getTextBoxOracle().contains("Vigilance"));
        // her own text box is still there too
        AssertJUnit.assertTrue(phoebe.getTextBoxOracle().contains("permanently steals"));
        AssertJUnit.assertNotNull(steal(phoebe));

        // the Angel comes back as a new object with its text box; Phoebe keeps the stolen one
        game.getAction().moveToHand(angel, null);
        Card back = game.getAction().moveToPlay(game.getCardState(angel), opp, null, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(back.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertTrue(phoebe.hasKeyword(Keyword.FLYING));

        // Phoebe leaving loses it for good
        game.getAction().moveToHand(phoebe, null);
        Card again = game.getAction().moveToPlay(game.getCardState(phoebe), me, null, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(again.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertTrue(again.getStolenTextBoxes().isEmpty());
    }

    @Test
    public void testStolenActivatedAbilityWorksForPhoebe() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card phoebe = addCard("Phoebe, Head of S.N.E.A.K.", me);
        Card sorcerer = addCard("Prodigal Sorcerer", opp);
        game.getAction().checkStateEffects(true);
        stealFrom(game, phoebe, sorcerer);
        AssertJUnit.assertFalse(sorcerer.getSpellAbilities().anyMatch(sa -> sa.getApi() == ApiType.DealDamage));
        SpellAbility ping = null;
        for (SpellAbility sa : phoebe.getSpellAbilities()) {
            if (sa.getApi() == ApiType.DealDamage) {
                ping = sa;
            }
        }
        AssertJUnit.assertNotNull(ping);
        AssertJUnit.assertEquals(phoebe, ping.getHostCard());
        ping.setActivatingPlayer(me);
        ping.resetTargets();
        ping.getTargets().add(opp);
        int life = opp.getLife();
        AbilityUtils.resolve(ping);
        AssertJUnit.assertEquals(life - 1, opp.getLife());
    }

    @Test
    public void testLosingACharacteristicDefiningAbility() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card phoebe = addCard("Phoebe, Head of S.N.E.A.K.", me);
        addCard("Grizzly Bears", me);
        Card beast = addCard("Beast of Burden", opp);
        game.getAction().checkStateEffects(true);
        stealFrom(game, phoebe, beast);
        // FAQ: the Beast becomes 0/0 and dies; Phoebe's P/T is now the creature count
        AssertJUnit.assertTrue(game.getCardState(beast).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertEquals(2, phoebe.getNetPower());
        AssertJUnit.assertEquals(2, phoebe.getNetToughness());
    }

    @Test
    public void testFlavorTextAndBlocking() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card phoebe = addCard("Phoebe, Head of S.N.E.A.K.", me);
        // Lexivore's flavor text is printed in its art (FAQ), and still counts
        IPaperCard lexPc = FModel.getMagicDb().getCommonCards().getCard("Lexivore", "UGL");
        Card lexivore = Card.fromPaperCard(lexPc, opp);
        lexivore.setGameTimestamp(game.getNextTimestamp());
        opp.getZone(ZoneType.Battlefield).add(lexivore);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(lexivore.getFlavorText().isEmpty());
        AssertJUnit.assertFalse(CombatUtil.canBlock(phoebe, lexivore));
        stealFrom(game, phoebe, lexivore);
        // without its flavor text it can block her (ruling)
        AssertJUnit.assertTrue(CombatUtil.canBlock(phoebe, lexivore));
        AssertJUnit.assertTrue(phoebe.getFlavorText().contains("Plucking the chicken"));
    }

    @Test
    public void testWatermarksTravel() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card phoebe = ust("Phoebe, Head of S.N.E.A.K.", me);
        Card knight = ust("Knight of the Widget", opp);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(knight.isValid("Card.Watermark_orderofthewidget", opp, knight, null));
        stealFrom(game, phoebe, knight);
        AssertJUnit.assertFalse(knight.isValid("Card.Watermark_orderofthewidget", opp, knight, null));
        AssertJUnit.assertFalse(knight.isValid("Card.Watermarked", opp, knight, null));
        AssertJUnit.assertTrue(phoebe.isValid("Card.Watermark_orderofthewidget", me, phoebe, null));
        AssertJUnit.assertTrue(phoebe.isValid("Card.Watermark_agentsofsneak", me, phoebe, null));
    }

    @Test
    public void testStealingFromPhoebeTakesEverything() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card mine = addCard("Phoebe, Head of S.N.E.A.K.", me);
        Card theirs = addCard("Phoebe, Head of S.N.E.A.K.", opp);
        Card angel = addCard("Serra Angel", me);
        game.getAction().checkStateEffects(true);
        stealFrom(game, theirs, angel);
        AssertJUnit.assertTrue(theirs.hasKeyword(Keyword.FLYING));
        stealFrom(game, mine, theirs);
        // all her text boxes are one: both go
        AssertJUnit.assertFalse(theirs.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertNull(steal(theirs));
        AssertJUnit.assertTrue(mine.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertTrue(mine.getTextBoxOracle().contains("Vigilance"));
        AssertJUnit.assertTrue(theirs.getTextBoxOracle().isEmpty());
    }
}
