package forge.card;

public record WordChangedType(String oldWord, String newWord) implements ICardChangedType {

    @Override
    public CardType applyChanges(CardType newType) {
        // a card type word (Deceptive Divination's sorcery to instant) swaps the card type itself
        final CardType.CoreType oldCore = CardType.CoreType.getEnum(oldWord);
        final CardType.CoreType newCore = CardType.CoreType.getEnum(newWord);
        if (oldCore != null && newCore != null) {
            if (newType.coreTypes.remove(oldCore)) {
                newType.coreTypes.add(newCore);
            }
            return newType;
        }
        if (newType.hasStringType(oldWord)) {
            newType.subtypes.remove(oldWord);
            newType.subtypes.add(newWord);
        }
        return newType;
    }
}
