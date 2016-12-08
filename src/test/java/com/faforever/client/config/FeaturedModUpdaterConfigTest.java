package com.faforever.client.config;

import com.faforever.client.Main;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.google.common.base.Stopwatch;
import javafx.stage.Stage;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.testfx.util.WaitForAsyncUtils;

import java.lang.invoke.MethodHandles;

import static org.mockito.Mockito.mock;

public class FeaturedModUpdaterConfigTest extends AbstractPlainJavaFxTest {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testDoesItSmoke() throws Exception {
    WaitForAsyncUtils.waitForAsyncFx(20000, () -> {
      Stopwatch stopwatch = Stopwatch.createStarted();
      try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
        context.getBeanFactory().registerSingleton("hostService", mock(PlatformService.class));
        context.getBeanFactory().registerSingleton("stage", new Stage());
        context.register(Main.class);
        context.refresh();
        logger.debug("Loading application context took {}", stopwatch.stop());
      }
    });
  }
}
