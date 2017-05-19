import controller.DocumentController;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.DocumentService;

import java.io.IOException;

import static java.nio.charset.Charset.defaultCharset;

public class MainVerticle extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(MainVerticle.class);
    private static final String CONFIG_FILE = "application-configuration.json";

    @Override
    public void start() throws IOException {
        JsonObject config = new JsonObject();

        String configFileContent = IOUtils.toString(getClass().getResourceAsStream(CONFIG_FILE), defaultCharset());
        config.mergeIn(new JsonObject(configFileContent));

        DeploymentOptions deploymentOptions = new DeploymentOptions();
        deploymentOptions.setConfig(config);

        vertx.deployVerticle(DocumentController.class.getName(), deploymentOptions);
        vertx.deployVerticle(DocumentService.class.getName(), deploymentOptions);

        LOG.info("Application started");
    }
}
