package forge.game.combat;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Table;

import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameEntityCounterTable;
import forge.game.card.Card;
import forge.game.card.CardCopyService;
import forge.game.card.CardDamageTable;
import forge.game.player.Player;
import forge.game.spellability.Ability;
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityMode;
import forge.game.zone.ZoneType;

/**
 * Stack of Paperwork: "Combat damage uses the stack." ({@code S:Mode$ CombatDamageUsesStack}). The combat damage
 * assigned in a damage step goes on the stack as one object controlled by no player, and is dealt as assigned when it
 * resolves. It's not a spell or an activated or triggered ability, so nothing that counters or copies those can touch
 * it. Rulings: a source that left the battlefield deals its damage as it last existed there; damage to a permanent that
 * left, or to a creature that stopped being a creature, isn't dealt; once the object is on the stack, it resolves even
 * if Stack of Paperwork is gone.
 */
public class CombatDamageOnStack extends Ability {

    /** What a damaged permanent was when the damage was assigned - it has to still be that to be dealt it. */
    private record Recipient(boolean creature, boolean planeswalker, boolean battle) {
        static Recipient of(final Card c) {
            return new Recipient(c.isCreature(), c.isPlaneswalker(), c.isBattle());
        }

        boolean stillFits(final Card c) {
            return (!creature || c.isCreature()) && (!planeswalker || c.isPlaneswalker()) && (!battle || c.isBattle());
        }
    }

    private final CardDamageTable assigned = new CardDamageTable();
    private final Map<Card, Card> sourceLki = new HashMap<>();
    private final Map<Card, Recipient> recipients = new HashMap<>();

    /** Whether combat damage uses the stack right now. */
    public static Card usesStackHost(final Game game) {
        for (final Card c : game.getCardsIn(ZoneType.Battlefield)) {
            for (final StaticAbility st : c.getStaticAbilities()) {
                if (st.checkConditions(StaticAbilityMode.CombatDamageUsesStack)) {
                    return c;
                }
            }
        }
        return null;
    }

    public CombatDamageOnStack(final Card host, final Player activePlayer, final CardDamageTable damage) {
        super(host, ManaCost.NO_COST);
        // "controlled by no player": the active player only stands in for whose priority comes first
        setActivatingPlayer(activePlayer);
        for (final Table.Cell<Card, GameEntity, Integer> cell : damage.cellSet()) {
            final Card source = cell.getRowKey();
            assigned.put(source, cell.getColumnKey(), cell.getValue());
            sourceLki.computeIfAbsent(source, CardCopyService::getLKICopy);
            if (cell.getColumnKey() instanceof Card c) {
                recipients.computeIfAbsent(c, Recipient::of);
            }
        }
        setStackDescription(describe());
    }

    private String describe() {
        final StringBuilder sb = new StringBuilder("Combat damage: ");
        boolean first = true;
        for (final Table.Cell<Card, GameEntity, Integer> cell : assigned.cellSet()) {
            if (!first) {
                sb.append("; ");
            }
            first = false;
            sb.append(cell.getRowKey()).append(" deals ").append(cell.getValue()).append(" to ").append(cell.getColumnKey());
        }
        return sb.append(".").toString();
    }

    public CardDamageTable getAssignedDamage() {
        return assigned;
    }

    @Override
    public boolean canPlay() {
        return false;
    }

    @Override
    public void resolve() {
        final Game game = getHostCard().getGame();
        final CardDamageTable toDeal = new CardDamageTable();
        for (final Table.Cell<Card, GameEntity, Integer> cell : assigned.cellSet()) {
            final Card source = currentSource(game, cell.getRowKey());
            final GameEntity recipient = currentRecipient(game, cell.getColumnKey());
            if (recipient == null) {
                continue;
            }
            final Integer already = toDeal.get(source, recipient);
            toDeal.put(source, recipient, cell.getValue() + (already == null ? 0 : already));
        }
        game.copyLastState();
        game.getAction().dealDamage(true, toDeal, new CardDamageTable(), new GameEntityCounterTable(), null);
        // copy last state again for dying replacement effects
        game.copyLastState();
    }

    /** The source as it is now, or as it last existed on the battlefield. */
    private Card currentSource(final Game game, final Card source) {
        final Card now = game.getCardState(source, null);
        if (now != null && now.isInPlay() && source.equalsWithGameTimestamp(now)) {
            return now;
        }
        return sourceLki.get(source);
    }

    /** The recipient as it is now, or null when it can't be dealt this damage anymore. */
    private GameEntity currentRecipient(final Game game, final GameEntity recipient) {
        if (recipient instanceof Player p) {
            return p.isInGame() ? p : null;
        }
        if (recipient instanceof Card c) {
            final Card now = game.getCardState(c, null);
            if (now == null || !now.isInPlay() || !c.equalsWithGameTimestamp(now) || !recipients.get(c).stillFits(now)) {
                return null;
            }
            return now;
        }
        return null;
    }
}
