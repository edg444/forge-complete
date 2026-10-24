package forge.ai;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.testng.SkipException;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Not a regression test: run with -Dnoai.list=<file of card names> to record, per card, what the AI decides
 * for each of its spells and abilities on a generic board. Used to triage AI:RemoveDeck:All cards.
 */
public class NoAiProbeTest extends AITest {
    @Test
    public void probe() throws Exception {
        String list = System.getProperty("noai.list");
        if (list == null) {
            throw new SkipException("set -Dnoai.list to run the probe");
        }
        String out = System.getProperty("noai.out", list + ".out");
        List<String> names = Files.readAllLines(Paths.get(list), StandardCharsets.UTF_8);
        Method decide = AiController.class.getDeclaredMethod("canPlayAndPayFor", SpellAbility.class);
        decide.setAccessible(true);
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(Paths.get(out), StandardCharsets.UTF_8))) {
            for (String name : names) {
                if (name.isBlank()) {
                    continue;
                }
                String result;
                try {
                    result = probeCard(name.trim(), decide);
                } catch (Throwable t) {
                    result = "ERROR " + t;
                }
                w.println(name.trim() + "\t" + result.replace('\n', ' ').replace('\t', ' '));
                w.flush();
            }
        }
    }

    private Game board(Player[] who) {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (String land : new String[] {"Plains", "Island", "Swamp", "Mountain", "Forest"}) {
            for (int i = 0; i < 3; i++) {
                addCard(land, ai);
            }
            addCard(land, opp);
        }
        addCard("Grizzly Bears", ai).setSickness(false);
        addCard("Hill Giant", ai).setSickness(false);
        addCard("Grizzly Bears", opp).setSickness(false);
        addCard("Serra Angel", opp).setSickness(false);
        fillLibrary(ai, 20);
        fillLibrary(opp, 20);
        for (int i = 0; i < 2; i++) {
            addCardToZone("Runeclaw Bear", ai, ZoneType.Hand);
            addCardToZone("Runeclaw Bear", opp, ZoneType.Hand);
        }
        addCardToZone("Grizzly Bears", ai, ZoneType.Graveyard);
        addCardToZone("Grizzly Bears", opp, ZoneType.Graveyard);
        who[0] = ai;
        who[1] = opp;
        return game;
    }

    private String probeCard(String name, Method decide) throws Exception {
        // PhaseType can't be touched in a static initializer: it localizes its names, and FModel isn't loaded yet
        PhaseType[][] windows = {{PhaseType.MAIN1, null}, {PhaseType.MAIN2, null}, {PhaseType.END_OF_TURN, PhaseType.END_OF_TURN}};
        StringBuilder sb = new StringBuilder();
        boolean tries = false;
        for (ZoneType zone : new ZoneType[] {ZoneType.Hand, ZoneType.Battlefield}) {
            Player[] who = new Player[2];
            Game game = board(who);
            Player ai = who[0];
            Card c = createCard(name, ai);
            if (c == null) {
                return "MISSING";
            }
            if (zone == ZoneType.Battlefield && !c.isPermanent()) {
                continue;
            }
            c.setGameTimestamp(game.getNextTimestamp());
            ai.getZone(zone).add(c);
            if (zone == ZoneType.Battlefield) {
                c.setSickness(false);
            }
            game.getAction().checkStateEffects(true);
            AiController aic = ((PlayerControllerAi) ai.getController()).getAi();
            List<SpellAbility> sas = new ArrayList<>();
            for (SpellAbility sa : ComputerUtilAbility.getOriginalAndAltCostAbilities(
                    ComputerUtilAbility.getSpellAbilities(new CardCollection(c), ai), ai)) {
                if (sa.isManaAbility() || sa.isLandAbility()) {
                    continue;
                }
                if (zone == ZoneType.Battlefield && sa.isSpell()) {
                    continue;
                }
                sas.add(sa);
            }
            for (SpellAbility sa : sas) {
                sb.append(zone == ZoneType.Hand ? "H[" : "B[").append(sa.getApi() == null ? "-" : sa.getApi().name()).append("]");
                for (PhaseType[] win : windows) {
                    Player turn = win[1] == null ? ai : who[1];
                    game.getPhaseHandler().devModeSet(win[0], turn);
                    game.getAction().checkStateEffects(true);
                    sa.setActivatingPlayer(ai);
                    AiPlayDecision d;
                    try {
                        d = (AiPlayDecision) decide.invoke(aic, sa);
                    } catch (Exception e) {
                        Throwable cause = e.getCause() != null ? e.getCause() : e;
                        sb.append(" EX:").append(cause.getClass().getSimpleName());
                        continue;
                    } finally {
                        sa.resetTargets();
                    }
                    if (d == AiPlayDecision.WillPlay) {
                        tries = true;
                    }
                    sb.append(' ').append(d.name());
                }
                sb.append("; ");
            }
        }
        if (sb.length() == 0) {
            return "PASSIVE";
        }
        return (tries ? "TRIES\t" : "NEVER\t") + sb;
    }
}
