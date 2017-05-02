package model;

public class WatermarkBook extends WatermarkDocument {
    public static final String CONTENT = "book";

    private final String content;
    private final String topic;

    public WatermarkBook(String title, String author, String topic) {
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
