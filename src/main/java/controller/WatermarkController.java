package controller;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import model.WatermarkBook;
import model.WatermarkJournal;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.HEAD;
import static io.vertx.core.http.HttpMethod.POST;

public class WatermarkController extends AbstractVerticle {
    private static final String DOCUMENT_PATH = "/document";
    private static final String WATERMARK_PATH = "/watermark";
    private static final String DOCUMENT_ID = "id";

    public static final String DOCUMENT_CREATE_ADDRESS = "document.create";
    public static final String WATERMARK_GET_ADDRESS = "watermark.get";

    @Override
    public void start() {
        HttpServer server = vertx.createHttpServer();

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router.route(POST, DOCUMENT_PATH).handler(this::handleCreateWatermarkDocument);
        router.route(GET, String.format("%s/:%s", WATERMARK_PATH, DOCUMENT_ID)).handler(this::handleGetWatermarkDocument);
        router.route(HEAD, String.format("%s/:%s", WATERMARK_PATH, DOCUMENT_ID)).handler(this::handleWatermarkDocumentExists);

        server.requestHandler(router::accept).listen(8080);
    }

    private void handleCreateWatermarkDocument(final RoutingContext routingContext) {
        JsonObject body = routingContext.getBodyAsJson();
        HttpServerResponse response = routingContext.response();

        if (!contentExists(body.getString("content"))) {
            response.setStatusCode(HttpResponseStatus.UNPROCESSABLE_ENTITY.code());
            response.end();
            return;
        }

        vertx.eventBus().send(DOCUMENT_CREATE_ADDRESS, body, event -> {
            String uuid = (String) event.result().body();
            String responseBody = new JsonObject().put("id", uuid).encode();
            response.putHeader("Content-Type", "application/json").end(responseBody);
        });
    }

    private void handleGetWatermarkDocument(final RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        String id = routingContext.request().getParam(DOCUMENT_ID);

        vertx.eventBus().send(WATERMARK_GET_ADDRESS, id, event -> {
            JsonObject document = (JsonObject) event.result().body();

            if (document.isEmpty()) {
                response.setStatusCode(NOT_FOUND.code()).end();
            } else {
                response.putHeader("Content-Type", "application/json").end(document.encode());
            }
        });
    }

    private void handleWatermarkDocumentExists(final RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        String id = routingContext.request().getParam(DOCUMENT_ID);

        vertx.eventBus().send(WATERMARK_GET_ADDRESS, id, event -> {
            JsonObject document = (JsonObject) event.result().body();
            if (document.isEmpty()) {
                response.setStatusCode(NOT_FOUND.code()).end();
            } else if (StringUtils.isEmpty(document.getString("watermark"))) {
                response.putHeader("watermarked", "in progress").end();
            } else {
                response.putHeader("watermarked", "done").end();
            }
        });
    }

    private boolean contentExists(final String content) {
        return Arrays.asList(WatermarkBook.CONTENT, WatermarkJournal.CONTENT).contains(content);
    }
}
