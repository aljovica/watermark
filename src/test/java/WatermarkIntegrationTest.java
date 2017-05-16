import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static java.lang.String.valueOf;

@RunWith(VertxUnitRunner.class)
public class WatermarkIntegrationTest {

    private static final String HOST = "localhost";
    private static final String DOCUMENT_URI = "/document";
    private static final String WATERMARK_URI = "/watermark";
    private static final String WATERMARK = "watermark";
    private static final int PORT = 8080;
    @Rule
    public RunTestOnContext rule = new RunTestOnContext();

    @Test
    public void test(TestContext context) {
        Async async = context.async();

        Vertx vertx = rule.vertx();

        vertx.deployVerticle(MainVerticle.class.getName(), event -> {
            HttpClient client = vertx.createHttpClient();

            String requestBody = createTestBody();

            client.post(PORT, HOST, DOCUMENT_URI, response -> response.bodyHandler(documentBody -> {
                String id = new JsonObject(documentBody.toString()).getString("id");
                // Get watermark immediately
                client.get(PORT, HOST, WATERMARK_URI + "/" + id, watermarkResponse -> watermarkResponse.bodyHandler(watermarkBody -> {
                    JsonObject watermarkJson = new JsonObject(watermarkBody.toString());
                    assertMainFields(context, watermarkJson);
                    context.assertNull(watermarkJson.getString(WATERMARK));
                })).end();
                // Get watermark after 3 seconds. Watermark process takes 2 seconds to finish

                vertx.setTimer(3000, timer -> client.get(PORT, HOST, WATERMARK_URI + "/" + id, watermarkResponse -> watermarkResponse.bodyHandler(watermarkBody -> {
                    JsonObject watermarkJson = new JsonObject(watermarkBody.toString());
                    assertMainFields(context, watermarkJson);
                    context.assertNotNull(watermarkJson.getString(WATERMARK));
                    async.complete();
                })).end());
            }))
            .putHeader("content-length", valueOf(requestBody.length()))
            .putHeader("content-type", "application/json")
            .write(requestBody).end();
        });

    }

    private void assertMainFields(final TestContext context, final JsonObject watermarkJson) {
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
