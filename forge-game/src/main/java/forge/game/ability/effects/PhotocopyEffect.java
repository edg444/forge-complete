package forge.game.ability.effects;

import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.mutable.MutableBoolean;

import forge.card.GamePieceType;
import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.card.Card;
import forge.game.card.CardFactory;
import forge.game.card.CardState;
import forge.game.card.CardZoneTable;
import forge.game.card.CounterType;
import forge.game.card.TokenCreateTable;
import forge.game.event.GameEventCombatChanged;
import forge.game.event.GameEventTokenCreated;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;

/**
 * Mobile Clone: a picture of the target creature enters as a token that's a photocopy of it. The picture shows the card
 * itself, so it starts from the printed card - copy effects can't be seen in it (rulings). Everything in the shot is
 * part of the photocopy and copiable: the P/T its counters give and its keyword counters, the Auras and Equipment on it
 * (their abilities now about the photocopy itself), and its stickers.
 */
public class PhotocopyEffect extends TokenEffectBase {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getActivatingPlayer() + " takes a picture of " + getTargetCards(sa) + ".";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Player you = sa.getActivatingPlayer();
        final Game game = you.getGame();
        final TokenCreateTable tokenTable = new TokenCreateTable();
        for (final Card target : getTargetCards(sa)) {
            final Card pictured = game.getCardState(target);
            if (pictured == null || !pictured.isInPlay()) {
                continue;
            }
            game.getGameLog().add(GameLogEntryType.INFORMATION, you + " takes a picture of " + pictured + ".");
            final Card photo = CardFactory.getCard(pictured.getPaperCard(), you, game);
            // a token from the start, so making it copies what's baked in rather than the printed card again
            photo.setGamePieceType(GamePieceType.TOKEN);
            if (photo.hasState(pictured.getCurrentStateName())) {
                photo.setState(pictured.getCurrentStateName(), false);
            }
            final CardState state = photo.getCurrentState();

            state.setBasePower(state.getBasePower() + pictured.getPowerBonusFromCounters());
            state.setBaseToughness(state.getBaseToughness() + pictured.getToughnessBonusFromCounters());
            for (final CounterType ct : pictured.getCounters().elementSet()) {
                if (ct.isKeywordCounter()) {
                    final String kw = ct.toString();
                    final int n = Keyword.smartValueOf(kw.split(":")[0]).isMultipleRedundant() ? 1 : pictured.getCounters(ct);
                    for (int i = 0; i < n; i++) {
                        state.addIntrinsicKeyword(kw, true);
                    }
                }
            }
            for (final Card attachment : pictured.getAttachedCards()) {
                if (attachment.isAura() || attachment.isEquipment()) {
                    bakeIn(attachment, photo);
                }
            }
            if (pictured.isStickered()) {
                photo.setStickers(pictured);
            }
            tokenTable.put(you, photo, 1);
        }
        if (tokenTable.isEmpty()) {
            return;
        }
        final CardZoneTable triggerList = new CardZoneTable();
        final MutableBoolean combatChanged = new MutableBoolean(false);
        makeTokenTable(tokenTable, false, triggerList, combatChanged, sa);
        triggerList.triggerChangesZoneAll(game, sa);
        game.fireEvent(new GameEventTokenCreated());
        if (combatChanged.isTrue()) {
            game.updateCombatForView();
            game.fireEvent(new GameEventCombatChanged());
        }
    }

    /** An Aura's or Equipment's statics and triggers about the creature it's on, now about the photocopy itself. */
    private static void bakeIn(final Card attachment, final Card photo) {
        final CardState from = attachment.getCurrentState();
        final CardState to = photo.getCurrentState();
        for (final Map.Entry<String, String> svar : from.getSVars().entrySet()) {
            if (!to.hasSVar(svar.getKey())) {
                to.setSVar(svar.getKey(), aboutItself(svar.getValue()));
            }
        }
        for (final StaticAbility st : from.getStaticAbilities()) {
            final String text = join(st.getMapParams());
            if (refersToAttached(text)) {
                to.addStaticAbility(StaticAbility.create(aboutItself(text), photo, to, true));
            }
        }
        for (final Trigger t : from.getTriggers()) {
            final String text = join(t.getMapParams());
            if (refersToAttached(text)) {
                to.addTrigger(TriggerHandler.parseTrigger(aboutItself(text), photo, true, to));
            }
        }
    }

    private static String join(final Map<String, String> params) {
        return params.entrySet().stream().map(e -> e.getKey() + "$ " + e.getValue()).collect(Collectors.joining(" | "));
    }

    private static boolean refersToAttached(final String text) {
        return text.contains("EnchantedBy") || text.contains("EquippedBy") || text.contains("AttachedBy")
                || text.contains("Defined$ Enchanted") || text.contains("Defined$ Equipped");
    }

    private static String aboutItself(final String text) {
        return text.replaceAll("\\b(EnchantedBy|EquippedBy|AttachedBy)\\b", "Self")
                .replaceAll("Defined\\$ (Enchanted|Equipped)\\b", "Defined\\$ Self")
                .replace("Enchanted creature", "This creature").replace("Equipped creature", "This creature")
                .replace("enchanted creature", "this creature").replace("equipped creature", "this creature");
    }
}
