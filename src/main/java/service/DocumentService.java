package service;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import model.Book;
import model.Document;
import model.Journal;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static addresses.Addresses.DOCUMENT_CREATE_ADDRESS;
import static addresses.Addresses.DOCUMENT_GET_ADDRESS;
import static addresses.Addresses.WATERMARK_FINISHED_ADDRESS;
import static java.lang.String.format;

public class DocumentService extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(DocumentService.class);

    private static final String WATERMARK_DELAY_CONFIG_KEY = "watermark.delay";

    private Map<String, Document> watermarkStorage = new HashMap<>();

    @Override
    public void start() {
        vertx.eventBus().consumer(DOCUMENT_CREATE_ADDRESS, this::handleCreateDocument);
        vertx.eventBus().consumer(DOCUMENT_GET_ADDRESS, this::handleGetWatermark);
    }

    public void handleCreateDocument(Message<JsonObject> event) {
        String uuid = UUID.randomUUID().toString();
        Document document = createDocument(event);

        watermarkStorage.put(uuid, document);
        event.reply(uuid);

        simulateWatermarkProcess(uuid, document);
    }

    private void handleGetWatermark(Message<Object> event) {
        String uuid = (String) event.body();
        Document document = watermarkStorage.get(uuid);

        JsonObject jsonWatermarkDocument = new JsonObject();
        if (document != null) {
            jsonWatermarkDocument = new JsonObject(Json.encode(document));
        }

        event.reply(jsonWatermarkDocument);
    }

    private Document createDocument(Message<JsonObject> event) {
        JsonObject body = event.body();
        String title = body.getString("title");
        String author = body.getString("author");
        String content = body.getString("content");

        if (content.equals(Book.CONTENT)) {
            String topic = body.getString("topic");
            return new Book(title, author, topic);
        } else {
            return new Journal(title, author);
        }
    }

    private void simulateWatermarkProcess(String uuid, Document document) {
        LOG.info("Creating watermark");
        Integer delay = config().getInteger(WATERMARK_DELAY_CONFIG_KEY);

        vertx.setTimer(delay, event -> {
            vertx.eventBus().publish(WATERMARK_FINISHED_ADDRESS, uuid);

            Document watermarkedDocument = watermarkStorage.get(uuid);
            watermarkedDocument.setWatermark(createSimulatedWatermark(watermarkedDocument));
            watermarkStorage.put(uuid, document);
        });
    }

    private String createSimulatedWatermark(Document watermarkedDocument) {
        if (watermarkedDocument instanceof Book) {
            Book book = (Book) watermarkedDocument;
            return format("watermark-%s-%s-%s-%s", book.getContent(), book.getTopic(), book.getAuthor(), book.getTitle());
        } else {
            Journal journal = (Journal) watermarkedDocument;
            return format("watermark-%s-%s-%s", journal.getContent(), journal.getAuthor(), journal.getTitle());
        }
    }
}
