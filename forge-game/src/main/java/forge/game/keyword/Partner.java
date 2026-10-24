package forge.game.keyword;

public class Partner extends SimpleKeyword {

    // the Unknown Event's partner variant is printed bare, with its own reminder text
    private static final String READY_TO_RUN = "Ready to run";

    private String with = null;

    @Override
    public String getTitle() {
        if (READY_TO_RUN.equals(with)) {
            return with;
        }
        if (with != null) {
            return "Partner—" + with;
        }
        return super.getTitle();
    }

    @Override
    protected void parse(String details) {
        with = details.isEmpty() ? null : details;
    }

    @Override
    protected String formatReminderText(String reminderText) {
        if (with == null) {
            return reminderText;
        } else if (READY_TO_RUN.equals(with)) {
            return "You can have two commanders if both have ready to run.";
        } else {
            return "You can have two commanders if both have this ability.";
        }
    }
}
