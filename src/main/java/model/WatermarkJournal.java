package model;

public class WatermarkJournal extends WatermarkDocument {
    public static final String CONTENT = "journal";
    private final String content;

    public WatermarkJournal(final String title, final String author) {
        super(title, author);

        this.content = CONTENT;
    }

    public String getContent() {
        return content;
    }
}
