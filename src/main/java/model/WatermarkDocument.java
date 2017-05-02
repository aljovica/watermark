package model;

public abstract class WatermarkDocument {
    private String title;
    private String author;
    private String watermark;

    public WatermarkDocument(String title, String author) {
        this.title = title;
        this.author = author;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getWatermark() {
        return watermark;
    }

    public void setWatermark(final String watermark) {
        this.watermark = watermark;
    }
}
