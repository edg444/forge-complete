package forge.ai.ability;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import forge.StaticData;
import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.card.CardFlavorText;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.MyRandom;

/**
 * My First Tome, Everythingamajig: the AI hears a card's flavor text and names the card. User's
 * ruling (2026-10-09): it recognizes the flavor text some of the time, like a player who has read a
 * lot of cards - then it names a card that really prints those words - and otherwise guesses a card
 * it has seen the sayer play.
 */
public class GuessNameAi extends SpellAbilityAi {
    static final float RECALL_CHANCE = 0.35f;

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    public AiAbilityDecision chkDrawback(final Player ai, final SpellAbility sa) {
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    public static String guessName(final Player ai, final SpellAbility sa) {
        final List<Card> hidden = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("DefinedCard"), sa);
        // only the words said aloud are read off the card, never its name
        final String flavor = hidden.isEmpty() ? "" : hidden.get(0).getFlavorText();

        if (!flavor.isEmpty() && MyRandom.getRandom().nextFloat() < RECALL_CHANCE) {
            final List<String> recalled = namesPrinting(flavor);
            if (!recalled.isEmpty()) {
                return Aggregates.random(recalled);
            }
        }

        final Player sayer = sa.getActivatingPlayer();
        final List<String> seen = new ArrayList<>();
        for (final Card c : sayer.getGame().getCardsIn(ZoneType.listValueOf("Battlefield,Graveyard,Exile"))) {
            if (c.getOwner().equals(sayer) && c.getPaperCard() != null && !c.isToken()) {
                seen.add(c.getPaperCard().getName());
            }
        }
        if (!seen.isEmpty()) {
            return Aggregates.random(seen);
        }
        final PaperCard any = Aggregates.random(StaticData.instance().getCommonCards().getAllCards());
        return any == null ? "" : any.getName();
    }

    /** Every card name with a printing whose flavor text is exactly these words. */
    public static List<String> namesPrinting(final String flavor) {
        final Set<String> names = new LinkedHashSet<>();
        for (final PaperCard pc : StaticData.instance().getCommonCards().getAllCards()) {
            if (flavor.equals(CardFlavorText.get(pc.getEdition(), pc.getCollectorNumber()))) {
                names.add(pc.getName());
            }
        }
        return new ArrayList<>(names);
    }
}
