package service;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import model.WatermarkBook;
import model.WatermarkDocument;
import model.WatermarkJournal;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static controller.WatermarkController.DOCUMENT_CREATE_ADDRESS;
import static controller.WatermarkController.WATERMARK_GET_ADDRESS;
import static java.lang.String.format;

public class WatermarkService extends AbstractVerticle {
    private Map<String, WatermarkDocument> watermarkStorage = new HashMap<>();

    @Override
    public void start() {
        vertx.eventBus().consumer(DOCUMENT_CREATE_ADDRESS, this::handleCreateDocument);
        vertx.eventBus().consumer(WATERMARK_GET_ADDRESS, this::handleGetWatermark);
    }

    private void handleCreateDocument(final Message<JsonObject> event) {
        String uuid = UUID.randomUUID().toString();
        WatermarkDocument document = createDocument(event);

        watermarkStorage.put(uuid, document);
        event.reply(uuid);

        simulateWatermarkProcess(uuid, document);
    }

    private void handleGetWatermark(final Message<Object> event) {
        String uuid = (String) event.body();
        WatermarkDocument watermarkDocument = watermarkStorage.get(uuid);

        JsonObject jsonWatermarkDocument = new JsonObject();
        if (watermarkDocument != null) {
            jsonWatermarkDocument = new JsonObject(Json.encode(watermarkDocument));
        }

        event.reply(jsonWatermarkDocument);
    }

    private WatermarkDocument createDocument(Message<JsonObject> event) {
        JsonObject body = event.body();
        String title = body.getString("title");
        String author = body.getString("author");
        String content = body.getString("content");
        String topic = body.getString("topic");

        if (content.equals(WatermarkBook.CONTENT)) {
            return new WatermarkBook(title, author, topic);
        } else {
            return new WatermarkJournal(title, author);
        }
    }

    private void simulateWatermarkProcess(String uuid, WatermarkDocument document) {
        Integer delay = config().getInteger("watermark.delay");

        vertx.setTimer(delay, event -> {
            WatermarkDocument watermarkedDocument = watermarkStorage.get(uuid);
            watermarkedDocument.setWatermark(createSimulatedWatermark(watermarkedDocument));
            watermarkStorage.put(uuid, document);
        });
    }

    private String createSimulatedWatermark(final WatermarkDocument watermarkedDocument) {
        if (watermarkedDocument instanceof WatermarkBook) {
            WatermarkBook book = (WatermarkBook) watermarkedDocument;
            return format("watermark-%s-%s-%s-%s", book.getContent(), book.getTopic(), book.getAuthor(), book.getTitle());
        } else {
            WatermarkJournal journal = (WatermarkJournal) watermarkedDocument;
            return format("watermark-%s-%s-%s", journal.getContent(), journal.getAuthor(), journal.getTitle());
        }
    }
}
