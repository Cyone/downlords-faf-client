package com.faforever.client.update;

import com.faforever.client.game.FeaturedModBean;
import com.faforever.client.patch.GameUpdateService;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class MockGameUpdateService implements GameUpdateService {

  @Override
  public CompletionStage<Void> updateBaseMod(FeaturedModBean featuredBaseMod, Integer version, Map<String, Integer> featuredModVersions, Set<String> simModUids) {
    return CompletableFuture.completedFuture(null);
  }
}
