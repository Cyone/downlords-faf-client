package com.faforever.client.mod;

import com.faforever.client.io.FileUtils;
import com.faforever.client.task.CompletableTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.Objects;

public class UninstallModTask extends CompletableTask<Void> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Inject
  ModService modService;

  private ModInfoBean mod;

  public UninstallModTask() {
    super(CompletableTask.Priority.LOW);
  }

  public void setMod(ModInfoBean mod) {
    this.mod = mod;
  }

  @Override
  protected Void call() throws Exception {
    Objects.requireNonNull(mod, "mod has not been set");

    logger.info("Uninstalling mod '{}' ({})", mod.getName(), mod.getId());
    Path modPath = modService.getPathForMod(mod);

    FileUtils.deleteRecursively(modPath);

    return null;
  }
}
