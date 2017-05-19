import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import model.Journal;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static addresses.Addresses.WATERMARK_FINISHED_ADDRESS;
import static java.lang.String.valueOf;

@RunWith(VertxUnitRunner.class)
public class WatermarkIntegrationTest {

    private static final String HOST = "localhost";
    private static final String DOCUMENT_RESOURCE = "/v1/document";
    private static final String WATERMARK = "watermark";
    private static final String DOCUMENT_ID = "id";
    private static final int PORT = 8080;

    private Vertx vertx;

    @Rule
    public RunTestOnContext rule = new RunTestOnContext();

    @Before
    public void before() {
        vertx = rule.vertx();
    }

    @Test
    public void shouldWatermarkDocument(TestContext context) {
        Async async = context.async();

        vertx.deployVerticle(MainVerticle.class.getName(), event -> {
            HttpClient client = vertx.createHttpClient();
            String requestBody = createJournal();

            client.post(PORT, HOST, DOCUMENT_RESOURCE, postResponse -> postResponse.bodyHandler(postResponseBody -> {
                String id = new JsonObject(postResponseBody.toString()).getString(DOCUMENT_ID);

                client.get(PORT, HOST, DOCUMENT_RESOURCE + "/" + id, getResponse -> shouldGetId(context, getResponse)).end();
                client.head(PORT, HOST, DOCUMENT_RESOURCE + "/" + id, headResponse -> shouldBeRightStatus(context, headResponse, "in progress")).end();
                vertx.eventBus().consumer(WATERMARK_FINISHED_ADDRESS, eventResponse -> shouldRetrieveWatermark(context, async, client, id));
            }))
             .putHeader("content-length", valueOf(requestBody.length()))
             .write(requestBody).end();
        });
    }

    private void shouldGetId(TestContext context, HttpClientResponse getResponse) {
        getResponse.bodyHandler(getResponseBody -> {
            JsonObject watermarkJson = new JsonObject(getResponseBody.toString());
            assertMainFields(context, watermarkJson);
            context.assertNull(watermarkJson.getString(WATERMARK));
        });
    }

    private void shouldBeRightStatus(TestContext context, HttpClientResponse headResponse, String status) {
        context.assertEquals(headResponse.getHeader("watermarked"), status);
    }

    private void shouldRetrieveWatermark(TestContext context, Async async, HttpClient client, String id) {
        client.get(PORT, HOST, DOCUMENT_RESOURCE + "/" + id, getResponse -> getResponse.bodyHandler(getResponseBody -> {
            JsonObject watermarkJson = new JsonObject(getResponseBody.toString());
            assertMainFields(context, watermarkJson);
            context.assertNotNull(watermarkJson.getString(WATERMARK));
            async.complete();
        })).end();

        client.head(PORT, HOST, DOCUMENT_RESOURCE + "/" + id, headResponse -> shouldBeRightStatus(context, headResponse, "done")).end();
    }

    private void assertMainFields(TestContext context, JsonObject watermarkJson) {
        context.assertEquals(watermarkJson.getString("title"), "The Dark Code");
        context.assertEquals(watermarkJson.getString("content"), "journal");
        context.assertEquals(watermarkJson.getString("author"), "Bruce Wayne");
    }

    private String createJournal() {
        JsonObject document = new JsonObject();

        document.put("content", Journal.CONTENT);
        document.put("title", "The Dark Code");
        document.put("author", "Bruce Wayne");

        return document.toString();
    }
}
