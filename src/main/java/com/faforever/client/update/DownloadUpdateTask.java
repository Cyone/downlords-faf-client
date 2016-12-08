package com.faforever.client.update;

import com.faforever.client.i18n.I18n;
import com.faforever.client.io.ByteCopier;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.ResourceLocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class DownloadUpdateTask extends CompletableTask<Path> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Inject
  I18n i18n;

  @Inject
  PreferencesService preferencesService;

  private UpdateInfo updateInfo;

  public DownloadUpdateTask() {
    super(Priority.MEDIUM);
  }

  @Override
  protected Path call() throws Exception {
    updateTitle(i18n.get("clientUpdateDownloadTask.title"));
    URL url = updateInfo.getUrl();

    Path updateDirectory = preferencesService.getCacheDirectory().resolve("update");
    Path targetFile = updateDirectory.resolve(updateInfo.getFileName());
    Files.createDirectories(targetFile.getParent());

    Path tempFile = Files.createTempFile(targetFile.getParent(), "update", null);

    try (InputStream inputStream = url.openStream();
         OutputStream outputStream = Files.newOutputStream(tempFile)) {
      ResourceLocks.acquireDownloadLock();
      ByteCopier.from(inputStream)
          .to(outputStream)
          .totalBytes(updateInfo.getSize())
          .listener(this::updateProgress)
          .copy();

      Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
    } finally {
      ResourceLocks.freeDownloadLock();
      try {
        Files.deleteIfExists(tempFile);
      } catch (IOException e) {
        logger.warn("Could not delete temporary file: " + tempFile.toAbsolutePath(), e);
      }
    }

    return targetFile;
  }

  public void setUpdateInfo(UpdateInfo updateInfo) {
    this.updateInfo = updateInfo;
  }
}
