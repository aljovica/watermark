import controller.WatermarkController;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.WatermarkService;

public class MainVerticle extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(MainVerticle.class);
    private static final String CONFIG_FILE = "src/main/resources/application-configuration.json";

    @Override
    public void start() {
        Vertx vertx = Vertx.vertx();

        final JsonObject config = new JsonObject();
        vertx.fileSystem().readFile(CONFIG_FILE, result -> {
            Buffer buff = result.result();
            config.mergeIn(new JsonObject(buff.toString()));

            DeploymentOptions deploymentOptions = new DeploymentOptions();
            deploymentOptions.setConfig(config);

            vertx.deployVerticle(WatermarkController.class.getName(), deploymentOptions);
            vertx.deployVerticle(WatermarkService.class.getName(), deploymentOptions);

            LOG.info("Application started");
        });

    }
}
