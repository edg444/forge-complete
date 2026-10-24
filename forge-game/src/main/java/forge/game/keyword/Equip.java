package forge.game.keyword;

public class Equip extends KeywordWithCost {

    String type = "creature";

    public Equip() {
    }

    public String getValidDescription() { return type; }

    @Override
    protected void parse(String details) {
        String[] k = details.split(":");
        super.parse(k[0]);
        // an empty type keeps the default, as the ability itself does (Equip:<cost>:::<extra params>)
        if (k.length > 2 && !k[2].isEmpty()) {
            type = k[2];
        }
    }

    @Override
    protected String formatReminderText(String reminderText) {
        return String.format(reminderText, cost.toSimpleString(), type);
    }
}
