package forge.game.keyword;

public class Trample extends KeywordWithType {
    // Super-Duper Death Ray: trample on an instant or sorcery, "Trample:Spell" - the spell's ExcessDamage does the work
    private boolean isOnSpell() {
        return "Spell".equals(type);
    }

    @Override
    public String getTitle() {
        if (isOnSpell()) {
            return "Trample";
        }
        if (!type.isEmpty()) {
            return "Trample Over Planeswalkers";
        }
        return "Trample";
    }
    @Override
    protected String formatReminderText(String reminderText) {
        if (isOnSpell()) {
            return "This spell can deal excess damage to its target's controller.";
        }
        if (!type.isEmpty()) {
            return "This creature can deal excess combat damage to the controller of the planeswalker it's attacking.";
        }
        return reminderText;
    }
}
