package model;

public class Journal extends Document {
    public static final String CONTENT = "journal";
    private final String content;

    public Journal(String title, String author) {
        super(title, author);

        this.content = CONTENT;
    }

    public String getContent() {
        return content;
    }
}
