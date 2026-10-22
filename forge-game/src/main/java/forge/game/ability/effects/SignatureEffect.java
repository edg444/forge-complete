package forge.game.ability.effects;

import java.util.List;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * The Collector: who signed a physical card. Its owner is asked the first time it matters - whether that copy is
 * signed by its artist, designer or someone else strongly associated with it, and if so by whom - and the answer is
 * kept for the match (user ruling 2026-10-22, like Mark Ritual). The AI's cards are unsigned.
 * <p>
 * Mode$ Ask (default): Defined$ cards get asked; RememberSigned$ remembers the signed ones on the host.
 * Mode$ DestroyByName: the targeted creatures are destroyed if their names are within five letters' length of the
 * name of whoever signed the discarded card (the cost's paid list).
 */
public class SignatureEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "DestroyByName".equals(sa.getParam("Mode"))
                ? "Destroy " + getTargetCards(sa) + " if its name's length is within five letters of the signer's."
                : "If " + getDefinedCardsOrTargeted(sa) + " is signed, copy it.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        if ("DestroyByName".equals(sa.getParam("Mode"))) {
            final List<Card> discarded = sa.getRootAbility().getPaidList("Discarded", true);
            if (discarded == null || discarded.isEmpty()) {
                return;
            }
            final String signer = signer(discarded.get(0), sa);
            if (signer.isEmpty()) {
                return;
            }
            for (final Card c : getTargetCards(sa)) {
                final Card cur = host.getGame().getCardState(c);
                if (cur.isInPlay() && Math.abs(letters(cur.getName()) - letters(signer)) <= 5) {
                    host.getGame().getAction().destroy(cur, sa, true, null);
                }
            }
            return;
        }
        for (final Card c : getDefinedCardsOrTargeted(sa)) {
            if (!signer(c, sa).isEmpty() && sa.hasParam("RememberSigned")) {
                host.addRemembered(c);
            }
        }
    }

    private static int letters(final String name) {
        int n = 0;
        for (int i = 0; i < name.length(); i++) {
            if (Character.isLetter(name.charAt(i))) {
                n++;
            }
        }
        return n;
    }

    private static String key(final Card c) {
        final String printed = c.getPaperCard() != null ? c.getPaperCard().getName() : c.getName();
        final List<Card> same = CardLists.filter(c.getOwner().getAllCards(),
                x -> x.getPaperCard() != null && printed.equals(x.getPaperCard().getName()));
        same.sort(java.util.Comparator.comparingInt(Card::getId));
        return "Signer:" + printed + "#" + (same.indexOf(c) + 1);
    }

    /** Whoever signed it, "" if it isn't signed, or null if its owner hasn't been asked yet. */
    public static String knownSigner(final Card c) {
        if (c.isToken() || c.getOwner() == null) {
            return "";
        }
        if (c.getOwner().getController().isAI()) {
            return "";
        }
        return c.getOwner().getMatchNote(key(c)) instanceof String s ? s : null;
    }

    /** Whoever signed it ("" if no one), asking its owner if they haven't been asked about that copy yet. */
    public static String signer(final Card c, final SpellAbility sa) {
        final String known = knownSigner(c);
        if (known != null) {
            return known;
        }
        final Player owner = c.getOwner();
        String name = "";
        if (owner.getController().confirmAction(sa, null, "Is your copy of " + c.getTranslatedName()
                + " signed by its artist, designer, or someone else strongly associated with it?", null)) {
            final String entered = owner.getController().guessString(sa, "Who signed " + c.getTranslatedName() + "?");
            name = entered == null ? "" : entered.trim();
        }
        owner.setMatchNote(key(c), name);
        return name;
    }
}
