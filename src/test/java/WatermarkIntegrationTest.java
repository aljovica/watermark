import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static addresses.Addresses.WATERMARK_FINISHED_ADDRESS;
import static java.lang.String.valueOf;

@RunWith(VertxUnitRunner.class)
public class WatermarkIntegrationTest {

    private static final String HOST = "localhost";
    private static final String DOCUMENT_RESOURCE = "/document";
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
    public void test(TestContext context) {
        Async async = context.async();

        vertx.deployVerticle(MainVerticle.class.getName(), event -> {
            HttpClient client = vertx.createHttpClient();
            String requestBody = createTestBody();

            client.post(PORT, HOST, DOCUMENT_RESOURCE, postResponse -> postResponse.bodyHandler(postResponseBody -> {
                String id = new JsonObject(postResponseBody.toString()).getString(DOCUMENT_ID);

                // Get the document immediately
                client.get(PORT, HOST, DOCUMENT_RESOURCE + "/" + id, getResponse -> getResponse.bodyHandler(getResponseBody -> {
                    JsonObject watermarkJson = new JsonObject(getResponseBody.toString());
                    assertMainFields(context, watermarkJson);
                    context.assertNull(watermarkJson.getString(WATERMARK));
                })).end();

                // Get the document after the watermark process is finished
                vertx.eventBus().consumer(WATERMARK_FINISHED_ADDRESS, eventResponse -> {
                    client.get(PORT, HOST, DOCUMENT_RESOURCE + "/" + id, getResponse -> getResponse.bodyHandler(getResponseBody -> {
                        JsonObject watermarkJson = new JsonObject(getResponseBody.toString());
                        assertMainFields(context, watermarkJson);
                        context.assertNotNull(watermarkJson.getString(WATERMARK));
                        async.complete();
                    })).end();
                });
            }))
            .putHeader("content-length", valueOf(requestBody.length()))
            .write(requestBody).end();
        });

    }

    private void assertMainFields(TestContext context, JsonObject watermarkJson) {
        context.assertEquals(watermarkJson.getString("title"), "The Dark Code");
        context.assertEquals(watermarkJson.getString("content"), "journal");
        context.assertEquals(watermarkJson.getString("author"), "Bruce Wayne");
    }

    private String createTestBody() {
        JsonObject document = new JsonObject();

        document.put("content", "journal");
        document.put("title", "The Dark Code");
        document.put("author", "Bruce Wayne");

        return document.toString();
    }
}
