package model;

public class Book extends Document {
    public static final String CONTENT = "book";

    private final String content;
    private final String topic;

    public Book(String title, String author, String topic) {
        super(title, author);

        this.topic = topic;
        this.content = CONTENT;
    }

    public String getContent() {
        return content;
    }

    public String getTopic() {
        return topic;
    }
}
