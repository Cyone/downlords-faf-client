package com.faforever.client.game;

import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class GamesControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  I18n i18n;
  private GamesController instance;
  @Mock
  private EnterPasswordController enterPasswordController;
  @Mock
  private CreateGameController createGameController;
  @Mock
  private GameService gameService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private Preferences preferences;
  @Mock
  private UiService uiService;
  @Mock
  private GamesTableController gamesTableController;
  @Mock
  private GamesTilesContainerController gamesTilesContainerController;
  @Mock
  private MapService mapService;

  @Before
  public void setUp() throws Exception {
    instance = new GamesController();
    instance.gameService = gameService;
    instance.preferencesService = preferencesService;
    instance.uiService = uiService;
    instance.mapService = mapService;
    instance.i18n = i18n;

    when(enterPasswordController.getRoot()).thenReturn(new Pane());
    when(createGameController.getRoot()).thenReturn(new Pane());
    when(gameService.getGames()).thenReturn(FXCollections.observableArrayList());
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getGamesViewMode()).thenReturn("tableButton");
    when(uiService.loadFxml("theme/enter_password.fxml")).thenReturn(gamesTilesContainerController);
    when(uiService.loadFxml("theme/play/create_game.fxml")).thenReturn(gamesTilesContainerController);
    when(uiService.loadFxml("theme/play/game_tile.fxml")).thenReturn(gamesTilesContainerController);
    when(uiService.loadFxml("theme/play/games_table.fxml")).thenReturn(gamesTableController);
    when(gamesTableController.selectedGameProperty()).thenReturn(new SimpleObjectProperty<>());
    when(gamesTilesContainerController.getRoot()).thenReturn(new Pane());

    loadFxml("theme/play/custom_games.fxml", clazz -> instance);
  }

  @Test
  public void testSetSelectedGameShowsDetailPane() throws Exception {
    assertFalse(instance.gameDetailPane.isVisible());
    instance.setSelectedGame(GameBuilder.create().defaultValues().get());
    assertTrue(instance.gameDetailPane.isVisible());
  }

  @Test
  public void testSetSelectedGameNullHidesDetailPane() throws Exception {
    instance.setSelectedGame(GameBuilder.create().defaultValues().get());
    assertTrue(instance.gameDetailPane.isVisible());
    instance.setSelectedGame(null);
    assertFalse(instance.gameDetailPane.isVisible());
  }
}
