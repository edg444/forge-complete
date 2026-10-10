package forge.ai;

import java.util.Map;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.CardRules;
import forge.card.CardStateName;
import forge.deck.DeckFormat;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.combat.Combat;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * 2018 Heroes of the Realm (PH18), Oracle text and printed cards checked against Scryfall 2026-10-10 (the only
 * ruling on either is the Commander RC's name-on-the-card legality ruling).
 * Optimus Prime, Inspiring Leader: "{1}: Turn target permanent you control to its other face." - user ruling: the
 * CR's convert (701.28a, any double-faced permanent incl. MDFCs), plus a face-down permanent turns face up.
 * Sol, Advocate Eternal: "Legendary partner (You can have two commanders if this is one of them. The other one is
 * promoted to legendary.)" - user ruling: the other one is anything that could be a commander if it were legendary.
 */
public class HeroesOfTheRealm2018Test extends AITest {

    private static SpellAbility ability(Card c, ApiType api) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == api) {
                return sa;
            }
        }
        return null;
    }

    private void turnOver(Card optimus, Card target) {
        SpellAbility sa = ability(optimus, ApiType.SetState);
        sa.setActivatingPlayer(optimus.getController());
        sa.resetTargets();
        sa.getTargets().add(target);
        AbilityUtils.resolve(sa);
    }

    @Test
    public void testOptimusTextReadsLikeOracle() {
        Game game = initAndCreateGame();
        Card optimus = addCard("Optimus Prime, Inspiring Leader", game.getPlayers().get(1));
        AssertJUnit.assertEquals("{1}: Turn target permanent you control to its other face. | "
                + "{1}: Until end of turn, Optimus Prime, Inspiring Leader becomes a Construct with base power and "
                + "toughness 6/6 and creatures you control gain trample.",
                String.join(" | ", optimus.getAbilityText().trim().split("\\s*[\\r\\n]+")));
    }

    @Test
    public void testOtherFaceTransformsAndConvertsBack() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card optimus = addCard("Optimus Prime, Inspiring Leader", p);
        Card delver = addCard("Delver of Secrets", p);
        turnOver(optimus, delver);
        AssertJUnit.assertEquals("Insectile Aberration", delver.getName());
        turnOver(optimus, delver);
        AssertJUnit.assertEquals("Delver of Secrets", delver.getName());
    }

    @Test
    public void testOtherFaceTurnsModalDoubleFacedCards() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card optimus = addCard("Optimus Prime, Inspiring Leader", p);
        Card pathway = addCard("Brightclimb Pathway", p);
        turnOver(optimus, pathway);
        AssertJUnit.assertEquals("Grimclimb Pathway", pathway.getName());
    }

    @Test
    public void testOtherFaceNeverIntoAnInstantOrSorcery() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card optimus = addCard("Optimus Prime, Inspiring Leader", p);
        Card seaGate = addCard("Sea Gate Restoration", p);
        seaGate.setState(CardStateName.Backside, true);
        seaGate.setBackSide(true);
        AssertJUnit.assertEquals("Sea Gate, Reborn", seaGate.getName());
        turnOver(optimus, seaGate);
        AssertJUnit.assertEquals("Sea Gate, Reborn", seaGate.getName());
    }

    @Test
    public void testOtherFaceTurnsFaceDownUpAndSkipsSingleFaced() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card optimus = addCard("Optimus Prime, Inspiring Leader", p);
        Card bears = addCard("Grizzly Bears", p);
        Card hidden = addCard("Hill Giant", p);
        hidden.turnFaceDown(true);
        AssertJUnit.assertTrue(hidden.isFaceDown());

        turnOver(optimus, bears);
        AssertJUnit.assertFalse(bears.isFaceDown());
        AssertJUnit.assertEquals("Grizzly Bears", bears.getName());

        turnOver(optimus, hidden);
        AssertJUnit.assertFalse(hidden.isFaceDown());
        AssertJUnit.assertEquals("Hill Giant", hidden.getName());
    }

    @Test
    public void testOptimusBecomesConstructAndGrantsTrample() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card optimus = addCard("Optimus Prime, Inspiring Leader", p);
        Card bears = addCard("Grizzly Bears", p);
        Card theirs = addCard("Hill Giant", game.getPlayers().get(0));
        SpellAbility sa = ability(optimus, ApiType.Animate);
        sa.setActivatingPlayer(p);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertEquals(6, optimus.getNetPower());
        AssertJUnit.assertEquals(6, optimus.getNetToughness());
        AssertJUnit.assertTrue(optimus.getType().hasCreatureType("Construct"));
        AssertJUnit.assertFalse(optimus.getType().hasCreatureType("Autobot"));
        AssertJUnit.assertTrue(optimus.getType().isLegendary() && optimus.getType().isArtifact());
        AssertJUnit.assertTrue(optimus.hasKeyword(Keyword.TRAMPLE));
        AssertJUnit.assertTrue(bears.hasKeyword(Keyword.TRAMPLE));
        AssertJUnit.assertFalse(theirs.hasKeyword(Keyword.TRAMPLE));
    }

    @Test
    public void testAiTurnsOnlyIntoABetterFace() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card optimus = addCard("Optimus Prime, Inspiring Leader", ai);
        addCard("Mountain", ai);
        Card delver = addCard("Delver of Secrets", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = ability(optimus, ApiType.SetState);
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).willingToPlay());
        AssertJUnit.assertEquals(delver, sa.getTargets().getFirstTargetedCard());

        turnOver(optimus, delver);
        sa.resetTargets();
        AssertJUnit.assertFalse(SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).willingToPlay());
    }

    private static CardRules rules(String name) {
        return FModel.getMagicDb().getCommonCards().getCard(name).getRules();
    }

    @Test
    public void testLegendaryPartnerDeckRules() {
        initAndCreateGame();
        CardRules sol = rules("Sol, Advocate Eternal");
        AssertJUnit.assertTrue(sol.canBePartnerCommanders(rules("Grizzly Bears")));
        AssertJUnit.assertTrue(rules("Grizzly Bears").canBePartnerCommanders(sol));
        AssertJUnit.assertTrue(sol.canBePartnerCommanders(rules("Thrasios, Triton Hero")));
        AssertJUnit.assertTrue(sol.canBePartnerCommanders(rules("Smuggler's Copter")));
        AssertJUnit.assertFalse(sol.canBePartnerCommanders(rules("Lightning Bolt")));
        AssertJUnit.assertFalse(sol.canBePartnerCommanders(rules("Forest")));
        AssertJUnit.assertFalse(rules("Grizzly Bears").canBePartnerCommanders(rules("Thrasios, Triton Hero")));

        PaperCard solCard = FModel.getMagicDb().getCommonCards().getCard("Sol, Advocate Eternal");
        PaperCard bears = FModel.getMagicDb().getCommonCards().getCard("Grizzly Bears");
        AssertJUnit.assertTrue(DeckFormat.Commander.isLegalCommanderWith(bears.getRules(), java.util.List.of(solCard, bears)));
        AssertJUnit.assertFalse(DeckFormat.Commander.isLegalCommanderWith(bears.getRules(), java.util.List.of(bears)));
    }

    private Card[] commanders(Game game, Player p, String other) {
        Card sol = addCard("Sol, Advocate Eternal", p);
        Card partner = addCard(other, p);
        p.addCommander(sol);
        p.addCommander(partner);
        p.promoteLegendaryPartners();
        game.getAction().checkStateEffects(true);
        return new Card[] {sol, partner};
    }

    @Test
    public void testPartnerIsPromotedToLegendary() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card[] cmd = commanders(game, p, "Grizzly Bears");
        AssertJUnit.assertTrue(cmd[1].getType().isLegendary());
        // perpetual: still legendary after changing zones
        Card moved = game.getAction().moveTo(ZoneType.Graveyard, cmd[1], null, null);
        AssertJUnit.assertTrue(moved.getType().isLegendary());
        Card notCommander = addCard("Grizzly Bears", p);
        AssertJUnit.assertFalse(notCommander.getType().isLegendary());
    }

    private Trigger trigger(Card sol, TriggerType mode) {
        for (Trigger t : sol.getTriggers()) {
            if (t.getMode() == mode) {
                return t;
            }
        }
        return null;
    }

    @Test
    public void testTeamworkAttack() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card[] cmd = commanders(game, p, "Grizzly Bears");
        Card other = addCard("Hill Giant", p);
        Trigger t = trigger(cmd[0], TriggerType.AttackersDeclared);

        AssertJUnit.assertTrue(cmd[1].isValid("Card.IsPartner", p, cmd[0], null));
        AssertJUnit.assertFalse(other.isValid("Card.IsPartner", p, cmd[0], null));
        AssertJUnit.assertFalse(cmd[0].isValid("Card.IsPartner", p, cmd[0], null));

        Map<AbilityKey, Object> both = AbilityKey.newMap();
        both.put(AbilityKey.AttackingPlayer, p);
        both.put(AbilityKey.Attackers, new CardCollection(java.util.List.of(cmd[0], cmd[1], other)));
        AssertJUnit.assertTrue(t.performTest(both));

        Map<AbilityKey, Object> solOnly = AbilityKey.newMap();
        solOnly.put(AbilityKey.AttackingPlayer, p);
        solOnly.put(AbilityKey.Attackers, new CardCollection(java.util.List.of(cmd[0], other)));
        AssertJUnit.assertFalse(t.performTest(solOnly));
        AssertJUnit.assertNotNull(opp);
    }

    @Test
    public void testTeamworkBlock() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card[] cmd = commanders(game, p, "Grizzly Bears");
        Card a1 = addCard("Hill Giant", opp);
        Card a2 = addCard("Hill Giant", opp);
        Trigger t = trigger(cmd[0], TriggerType.BlockersDeclared);

        Combat combat = new Combat(opp);
        combat.addAttacker(a1, p);
        combat.addAttacker(a2, p);
        combat.addBlocker(a1, cmd[0]);
        game.getPhaseHandler().setCombat(combat);
        AssertJUnit.assertFalse(t.requirementsCheck(game));

        combat.addBlocker(a2, cmd[1]);
        AssertJUnit.assertTrue(t.requirementsCheck(game));
    }

    @Test
    public void testNoPartnerNoTeamwork() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card sol = addCard("Sol, Advocate Eternal", p);
        Card bears = addCard("Grizzly Bears", p);
        Trigger t = trigger(sol, TriggerType.AttackersDeclared);
        Map<AbilityKey, Object> both = AbilityKey.newMap();
        both.put(AbilityKey.AttackingPlayer, p);
        both.put(AbilityKey.Attackers, new CardCollection(java.util.List.of(sol, bears)));
        AssertJUnit.assertFalse(t.performTest(both));
    }
}
