package com.faforever.client.replay;

import com.faforever.client.i18n.I18n;
import com.faforever.client.task.CompletableTask;

import javax.inject.Inject;
import java.util.Collection;

public class LoadLocalReplaysTask extends CompletableTask<Collection<Replay>> {

  @Inject
  ReplayService replayService;

  @Inject
  I18n i18n;


  public LoadLocalReplaysTask() {
    super(Priority.HIGH);
  }

  @Override
  protected Collection<Replay> call() throws Exception {
    updateTitle(i18n.get("replays.loadingLocalTask.title"));
    return replayService.getLocalReplays();
  }

}
