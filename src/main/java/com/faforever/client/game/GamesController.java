package com.faforever.client.game;

import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapServiceImpl.PreviewSize;
import com.faforever.client.mod.FeaturedModBean;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.domain.GameState;
import com.google.common.annotations.VisibleForTesting;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static javafx.beans.binding.Bindings.createObjectBinding;
import static javafx.beans.binding.Bindings.createStringBinding;

public class GamesController {

  private static final Collection<String> HIDDEN_FEATURED_MODS = Arrays.asList(
      KnownFeaturedMod.COOP.getString(),
      KnownFeaturedMod.LADDER_1V1.getString(),
      KnownFeaturedMod.GALACTIC_WAR.getString(),
      KnownFeaturedMod.MATCHMAKER.getString()
  );

  private static final Predicate<GameInfoBean> OPEN_CUSTOM_GAMES_PREDICATE = gameInfoBean ->
      gameInfoBean.getStatus() == GameState.OPEN
          && !HIDDEN_FEATURED_MODS.contains(gameInfoBean.getFeaturedMod());

  @FXML
  ToggleButton tableButton;
  @FXML
  ToggleButton tilesButton;
  @FXML
  ToggleGroup viewToggleGroup;
  @FXML
  VBox teamListPane;
  @FXML
  Label mapLabel;
  @FXML
  Button createGameButton;
  @FXML
  Pane gameViewContainer;
  @FXML
  Node gamesRoot;
  @FXML
  ImageView mapImageView;
  @FXML
  Label gameTitleLabel;
  @FXML
  Label numberOfPlayersLabel;
  @FXML
  Label hostLabel;
  @FXML
  Label gameTypeLabel;
  @FXML
  ScrollPane gameDetailPane;

  @Resource
  ApplicationContext applicationContext;
  @Resource
  I18n i18n;
  @Resource
  GameService gameService;
  @Resource
  MapService mapService;
  @Resource
  CreateGameController createGameController;
  @Resource
  EnterPasswordController enterPasswordController;
  @Resource
  PreferencesService preferencesService;
  @Resource
  NotificationService notificationService;
  @Resource
  ModService modService;

  private FilteredList<GameInfoBean> filteredItems;
  private Stage mapDetailPopup;

  private GameInfoBean currentGameInfoBean;
  private InvalidationListener teamsChangeListener;

  @PostConstruct
  void postConstruct() {

    ObservableList<GameInfoBean> gameInfoBeans = gameService.getGameInfoBeans();

    filteredItems = new FilteredList<>(gameInfoBeans);
    filteredItems.setPredicate(OPEN_CUSTOM_GAMES_PREDICATE);

    if (tilesButton.getId().equals(preferencesService.getPreferences().getGamesViewMode())) {
      viewToggleGroup.selectToggle(tilesButton);
      tilesButton.getOnAction().handle(null);
    } else {
      viewToggleGroup.selectToggle(tableButton);
      tableButton.getOnAction().handle(null);
    }
    viewToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
      preferencesService.getPreferences().setGamesViewMode(((ToggleButton) newValue).getId());
      preferencesService.storeInBackground();
    });

    setSelectedGame(null);
  }

  @FXML
  void onShowPrivateGames(ActionEvent actionEvent) {
    CheckBox checkBox = (CheckBox) actionEvent.getSource();
    boolean selected = checkBox.isSelected();
    if (selected) {
      filteredItems.setPredicate(OPEN_CUSTOM_GAMES_PREDICATE);
    } else {
      filteredItems.setPredicate(OPEN_CUSTOM_GAMES_PREDICATE.and(gameInfoBean -> !gameInfoBean.getPasswordProtected()));
    }
  }

  @FXML
  void onCreateGameButtonClicked(ActionEvent actionEvent) {
    if (preferencesService.getPreferences().getForgedAlliance().getPath() == null) {
      preferencesService.letUserChooseGameDirectory()
          .thenAccept(path -> {
            if (path != null) {
              onCreateGameButtonClicked(actionEvent);
            }
          });
      return;
    }

    Bounds screenBounds = createGameButton.localToScreen(createGameButton.getBoundsInLocal());
    createGameController.show(gamesRoot.getScene().getWindow(), screenBounds.getMinX(), screenBounds.getMaxY());
  }

  private Stage getMapDetailPopup() {
    if (mapDetailPopup == null) {
      mapDetailPopup = new Stage(StageStyle.TRANSPARENT);
      mapDetailPopup.initModality(Modality.NONE);
      mapDetailPopup.initOwner(getRoot().getScene().getWindow());
    }
    return mapDetailPopup;
  }

  public Node getRoot() {
    return gamesRoot;
  }

  @FXML
  void onTableButtonClicked() {
    GamesTableController gamesTableController = applicationContext.getBean(GamesTableController.class);
    gamesTableController.selectedGameProperty()
        .addListener((observable, oldValue, newValue) -> setSelectedGame(newValue));
    Platform.runLater(() -> {
      gamesTableController.initializeGameTable(filteredItems);

      Node root = gamesTableController.getRoot();
      populateContainer(root);
    });
  }

  private void populateContainer(Node root) {
    gameViewContainer.getChildren().setAll(root);
    AnchorPane.setBottomAnchor(root, 0d);
    AnchorPane.setLeftAnchor(root, 0d);
    AnchorPane.setRightAnchor(root, 0d);
    AnchorPane.setTopAnchor(root, 0d);
  }

  @FXML
  void onTilesButtonClicked() {
    GamesTilesContainerController gamesTilesContainerController = applicationContext.getBean(GamesTilesContainerController.class);
    gamesTilesContainerController.selectedGameProperty()
        .addListener((observable, oldValue, newValue) -> setSelectedGame(newValue));
    gamesTilesContainerController.createTiledFlowPane(filteredItems);

    Node root = gamesTilesContainerController.getRoot();
    populateContainer(root);
  }

  @VisibleForTesting
  void setSelectedGame(GameInfoBean gameInfoBean) {
    if (gameInfoBean == null) {
      gameDetailPane.setVisible(false);
      return;
    }

    gameDetailPane.setVisible(true);

    gameTitleLabel.textProperty().bind(gameInfoBean.titleProperty());

    mapImageView.imageProperty().bind(createObjectBinding(
        () -> mapService.loadPreview(gameInfoBean.getMapFolderName(), PreviewSize.LARGE),
        gameInfoBean.mapFolderNameProperty()
    ));

    numberOfPlayersLabel.textProperty().bind(createStringBinding(
        () -> i18n.get("game.detail.players.format", gameInfoBean.getNumPlayers(), gameInfoBean.getMaxPlayers()),
        gameInfoBean.numPlayersProperty(),
        gameInfoBean.maxPlayersProperty()
    ));

    hostLabel.textProperty().bind(gameInfoBean.hostProperty());
    mapLabel.textProperty().bind(gameInfoBean.mapFolderNameProperty());

    gameTypeLabel.textProperty().bind(createStringBinding(() -> {
      FeaturedModBean gameType = modService.getFeaturedMod(gameInfoBean.getFeaturedMod()).get();
      String fullName = gameType != null ? gameType.getDisplayName() : null;
      return StringUtils.defaultString(fullName);
    }, gameInfoBean.featuredModProperty()));

    if (currentGameInfoBean != null) {
      currentGameInfoBean.getTeams().removeListener(teamsChangeListener);
    }

    teamsChangeListener = observable -> createTeams(gameInfoBean.getTeams());
    teamsChangeListener.invalidated(gameInfoBean.getTeams());
    gameInfoBean.getTeams().addListener(teamsChangeListener);

    currentGameInfoBean = gameInfoBean;
  }

  private void createTeams(ObservableMap<? extends String, ? extends List<String>> playersByTeamNumber) {
    teamListPane.getChildren().clear();
    synchronized (playersByTeamNumber) {
      for (Map.Entry<? extends String, ? extends List<String>> entry : playersByTeamNumber.entrySet()) {
        TeamCardController teamCardController = applicationContext.getBean(TeamCardController.class);
        teamCardController.setPlayersInTeam(entry.getKey(), entry.getValue());
        teamListPane.getChildren().add(teamCardController.getRoot());
      }
    }
  }
}
