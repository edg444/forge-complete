package forge.ai;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.testng.SkipException;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Triage harness for the no-AI pass, skipped unless -Dnoai.triage=&lt;names file&gt; -Dnoai.triageOut=&lt;file&gt;: for each
 * card, the AI's decision on its spell (from hand) and its activated abilities (on the battlefield) in five standard
 * situations, bypassing the AI:RemoveDeck:All filter.
 */
public class NoAiTriageTest extends AITest {

    private enum Spot { OWN_MAIN1, OWN_MAIN2, THEIR_END, THEIR_ATTACK, OWN_ATTACK }

    private void board(Player ai, Player opp) {
        for (String l : new String[] {"Plains", "Island", "Swamp", "Mountain", "Forest"}) {
            for (int i = 0; i < 2; i++) {
                addCard(l, ai).setSickness(false);
                addCard(l, opp).setSickness(false);
            }
            addCardToZone(l, ai, ZoneType.Library);
            addCardToZone(l, opp, ZoneType.Library);
        }
        addCard("Grizzly Bears", ai).setSickness(false);
        addCard("Serra Angel", ai).setSickness(false);
        addCard("Hill Giant", opp).setSickness(false);
        addCard("Serra Angel", opp).setSickness(false);
        for (String c : new String[] {"Craw Wurm", "Lightning Bolt", "Counterspell"}) {
            addCardToZone(c, ai, ZoneType.Library);
            addCardToZone(c, opp, ZoneType.Library);
            addCardToZone(c, ai, ZoneType.Graveyard);
            addCardToZone(c, opp, ZoneType.Graveyard);
            addCardToZone(c, opp, ZoneType.Hand);
        }
        addCardToZone("Hill Giant", ai, ZoneType.Hand);
    }

    private void spot(Game game, Player ai, Player opp, Spot s) {
        switch (s) {
            case OWN_MAIN1 -> game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
            case OWN_MAIN2 -> game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
            case THEIR_END -> game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
            case THEIR_ATTACK, OWN_ATTACK -> {
                final Player att = s == Spot.THEIR_ATTACK ? opp : ai;
                final Player def = s == Spot.THEIR_ATTACK ? ai : opp;
                game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, att, false, 1);
                final Combat combat = new Combat(att);
                for (Card c : att.getCreaturesInPlay()) {
                    combat.addAttacker(c, def);
                    combat.setBlocked(c, false);
                }
                game.getPhaseHandler().setCombat(combat);
            }
        }
    }

    private static String decide(Player ai, SpellAbility sa) {
        try {
            sa.setActivatingPlayer(ai);
            final AiPlayDecision d = SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
            return d == AiPlayDecision.WillPlay ? "YES" + (sa.getTargets().isEmpty() ? "" : sa.getTargets().toString()) : d.toString();
        } catch (Exception e) {
            return "EXC " + e.getClass().getSimpleName() + ":" + e.getMessage();
        }
    }

    @Test
    public void triage() throws Exception {
        final String in = System.getProperty("noai.triage");
        final String out = System.getProperty("noai.triageOut");
        if (in == null || out == null) {
            throw new SkipException("no triage list");
        }
        final List<String> lines = new ArrayList<>();
        for (final String name : Files.readAllLines(Path.of(in), StandardCharsets.UTF_8)) {
            if (name.isBlank()) {
                continue;
            }
            final StringBuilder sb = new StringBuilder(name.trim());
            for (final Spot s : Spot.values()) {
                final StringBuilder cell = new StringBuilder();
                try {
                    // the spell from hand
                    Game game = initAndCreateGame();
                    Player ai = game.getPlayers().get(1);
                    Player opp = game.getPlayers().get(0);
                    board(ai, opp);
                    Card c = addCardToZone(name.trim(), ai, ZoneType.Hand);
                    spot(game, ai, opp, s);
                    for (SpellAbility sa : c.getSpellAbilities()) {
                        if (sa.isSpell()) {
                            cell.append("cast=").append(decide(ai, sa)).append(' ');
                        }
                    }
                    // its abilities in play
                    game = initAndCreateGame();
                    ai = game.getPlayers().get(1);
                    opp = game.getPlayers().get(0);
                    board(ai, opp);
                    c = addCardToZone(name.trim(), ai, ZoneType.Hand);
                    if (c.isPermanent()) {
                        c = game.getAction().moveToPlay(c, null, null);
                        c.setSickness(false);
                        spot(game, ai, opp, s);
                        for (SpellAbility sa : c.getSpellAbilities()) {
                            if (sa.isActivatedAbility() && !sa.isManaAbility()) {
                                cell.append("ab=").append(decide(ai, sa)).append(' ');
                            }
                        }
                    }
                } catch (Exception e) {
                    cell.append("EXC ").append(e);
                }
                sb.append(" | ").append(s).append(": ").append(cell.toString().trim());
            }
            lines.add(sb.toString());
        }
        Files.write(Path.of(out), lines, StandardCharsets.UTF_8);
    }
}
