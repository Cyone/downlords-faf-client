package com.faforever.client.config;

import com.faforever.client.patch.GameUpdater;
import com.faforever.client.patch.GameUpdaterImpl;
import com.faforever.client.patch.GitRepositoryFeaturedModUpdater;
import com.faforever.client.patch.SimpleHttpFeaturedModUpdater;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import javax.inject.Inject;

@Configuration
@Import(BaseConfig.class)
public class FeaturedModUpdaterConfig {

  @Inject
  private GitRepositoryFeaturedModUpdater gitFeaturedModUpdater;
  @Inject
  private SimpleHttpFeaturedModUpdater httpFeaturedModUpdater;

  @Bean
  @Profile("!local")
  GameUpdater gameUpdater() {
    return new GameUpdaterImpl()
        .addFeaturedModUpdater(gitFeaturedModUpdater)
        .addFeaturedModUpdater(httpFeaturedModUpdater);
  }
}
