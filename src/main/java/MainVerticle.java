import controller.WatermarkController;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.WatermarkService;

public class MainVerticle extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(MainVerticle.class);

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        vertx.deployVerticle(WatermarkController.class.getName());
        vertx.deployVerticle(WatermarkService.class.getName());

        LOG.info("Application started");
    }
}
