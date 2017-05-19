package controller;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import model.Book;
import model.Journal;

import static addresses.Addresses.DOCUMENT_CREATE_ADDRESS;
import static addresses.Addresses.DOCUMENT_GET_ADDRESS;
import static io.netty.handler.codec.http.HttpResponseStatus.ACCEPTED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.UNPROCESSABLE_ENTITY;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.HEAD;
import static io.vertx.core.http.HttpMethod.POST;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class DocumentController extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(DocumentController.class);

    private static final String PORT_CONFIG_KEY = "server.port";
    private static final String POST_DOCUMENT = "/v1/document";
    private static final String GET_DOCUMENT = "/v1/document/:id";
    private static final String DOCUMENT_ID = "id";

    @Override
    public void start() {
        HttpServer server = vertx.createHttpServer();

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router.route(POST, POST_DOCUMENT).handler(this::handleCreateDocument);
        router.route(GET, GET_DOCUMENT).handler(this::handleGetDocument);
        router.route(HEAD, GET_DOCUMENT).handler(this::handleWatermarkFinished);

        Integer port = config().getInteger(PORT_CONFIG_KEY);
        server.requestHandler(router::accept).listen(port);
    }

    private void handleCreateDocument(RoutingContext routingContext) {
        LOG.info("Creating document");

        JsonObject requestBody = routingContext.getBodyAsJson();
        HttpServerResponse response = routingContext.response();

        if (!validate(requestBody)) {
            LOG.error("request body is not valid");
            response.setStatusCode(UNPROCESSABLE_ENTITY.code()).end();
            return;
        }

        vertx.eventBus().send(DOCUMENT_CREATE_ADDRESS, requestBody, responseEvent -> {
            String uuid = (String) responseEvent.result().body();
            String responseBody = new JsonObject().put("id", uuid).encode();

            response.putHeader("Content-Type", "application/json")
                    .setStatusCode(ACCEPTED.code())
                    .end(responseBody);
        });
    }

    private void handleGetDocument(RoutingContext routingContext) {
        LOG.info("Fetching document");
        HttpServerResponse response = routingContext.response();
        String id = routingContext.request().getParam(DOCUMENT_ID);

        vertx.eventBus().send(DOCUMENT_GET_ADDRESS, id, responseEvent -> {
            JsonObject document = (JsonObject) responseEvent.result().body();

            if (document.isEmpty()) {
                response.setStatusCode(NOT_FOUND.code()).end();
            } else {
                response.putHeader("Content-Type", "application/json").end(document.encode());
            }
        });
    }

    private void handleWatermarkFinished(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        String id = routingContext.request().getParam(DOCUMENT_ID);

        vertx.eventBus().send(DOCUMENT_GET_ADDRESS, id, responseEvent -> {
            JsonObject document = (JsonObject) responseEvent.result().body();
            if (document.isEmpty()) {
                response.setStatusCode(NOT_FOUND.code()).end();
            } else if (isEmpty(document.getString("watermark"))) {
                response.putHeader("watermarked", "in progress").end();
            } else {
                response.putHeader("watermarked", "done").end();
            }
        });
    }

    private boolean validate(JsonObject body) {
        boolean rightContent = asList(Book.CONTENT, Journal.CONTENT).contains(body.getString("content"));
        boolean authorNotNull = body.getString("author") != null;
        boolean titleNotNull = body.getString("title") != null;

        boolean valid = rightContent && authorNotNull && titleNotNull;

        if (body.getString("content").equals(Book.CONTENT)) {
            boolean topicNotNull = body.getString("topic") != null;
            return valid && topicNotNull;
        }

        return valid;
    }
}
