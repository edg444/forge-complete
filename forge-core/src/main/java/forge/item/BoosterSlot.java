package forge.item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import forge.util.MyRandom;

public class BoosterSlot {
    private final String slotName;
    private String baseRarity;
    private float startRange = 0.0f;
    private final TreeMap<Float, String> slotPercentages = new TreeMap<>();
    private final Map<String, String> partners = new HashMap<>();

    public BoosterSlot(final String slotName, final List<String> contents) {
        this.slotName = slotName;
        this.baseRarity = null;
        parseContents(contents);
    }

    public final String getSlotName() {
        return slotName;
    }

    public static BoosterSlot parseSlot(final String slotName, final List<String> contents) {
        return new BoosterSlot(slotName, contents);
    }

    private void parseContents(List<String> contents) {
        for (String content : contents) {
            if (content.startsWith("#")) {
                continue;
            }
            String[] parts = content.split("=", 2);
            String key = parts[0];
            String value = parts[1];

            if (key.equalsIgnoreCase("Base")) {
                baseRarity = value;
            } else if (key.equalsIgnoreCase("Replace")) {
                // Are there other things?
                String[] replaceParts = value.split(" ", 2);
                float pct = Float.parseFloat(replaceParts[0]);
                startRange += pct;
                slotPercentages.put(startRange, replaceParts[1]);
            } else if (key.equalsIgnoreCase("Pair")) {
                // Pair=<card>|<card>: whenever this slot deals one, the other comes with it (Reality Fracture echoed pairs)
                String[] names = value.split("\\|");
                partners.put(names[0].trim(), names[1].trim());
                partners.put(names[1].trim(), names[0].trim());
            }
        }
    }

    public boolean hasPairs() {
        return !partners.isEmpty();
    }

    public String getPartner(final String cardName) {
        return partners.get(cardName);
    }

    public String replaceSlot() {
        float rand = MyRandom.getRandom().nextFloat();
        for (Float key : slotPercentages.keySet()) {
            if (rand < key) {
                System.out.println("Replaced a base slot! " + slotName + " -> " + slotPercentages.get(key));

                return slotPercentages.get(key);
            }
        }

        // If we didn't find a key, return the base rarity from that edition
        return baseRarity;
    }
}
