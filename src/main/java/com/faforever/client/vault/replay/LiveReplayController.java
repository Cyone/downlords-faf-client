package com.faforever.client.vault.replay;

import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.NodeTableCell;
import com.faforever.client.fx.StringCell;
import com.faforever.client.game.Game;
import com.faforever.client.game.GameService;
import com.faforever.client.game.MapPreviewTableCell;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapServiceImpl.PreviewSize;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.GameState;
import com.faforever.client.theme.UiService;
import com.google.api.client.repackaged.com.google.common.base.Joiner;
import javafx.application.Platform;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class LiveReplayController extends AbstractViewController<Node> {
  private final ObjectProperty<Game> selectedGame;
  public TableView<Game> liveReplayControllerRoot;
  public TableColumn<Game, Image> mapPreviewColumn;
  public TableColumn<Game, String> gameTitleColumn;
  public TableColumn<Game, Number> playersColumn;
  public TableColumn<Game, String> modsColumn;
  public TableColumn<Game, String> hostColumn;
  public TableColumn<Game, Number> watchColumn;
  @Inject
  FafService fafService;
  @Inject
  GameService gameService;
  @Inject
  UiService uiService;
  @Inject
  I18n i18n;
  @Inject
  PlayerService playerService;
  @Inject
  MapService mapService;

  public LiveReplayController() {
    selectedGame = new SimpleObjectProperty<>();
  }

  public void initialize() {
    initializeGameTable(gameService.getGames().filtered(game -> game.getStatus() == GameState.PLAYING));
  }

  public void initializeGameTable(ObservableList<Game> games) {
    SortedList<Game> sortedList = new SortedList<>(games);
    sortedList.comparatorProperty().bind(liveReplayControllerRoot.comparatorProperty());

    mapPreviewColumn.setCellFactory(param -> new MapPreviewTableCell(uiService));
    mapPreviewColumn.setCellValueFactory(param -> new ObjectBinding<Image>() {
      {
        bind(param.getValue().mapFolderNameProperty());
      }

      @Override
      protected Image computeValue() {
        return mapService.loadPreview(param.getValue().getMapFolderName(), PreviewSize.SMALL);
      }
    });

    gameTitleColumn.setCellValueFactory(param -> param.getValue().titleProperty());
    gameTitleColumn.setCellFactory(param -> new StringCell<>(title -> title));
    playersColumn.setCellValueFactory(param -> param.getValue().numPlayersProperty());
    playersColumn.setCellFactory(param -> new StringCell<>(number -> i18n.number(number.intValue())));
    hostColumn.setCellValueFactory(param -> param.getValue().hostProperty());
    hostColumn.setCellFactory(param -> new StringCell<>(String::toString));
    modsColumn.setCellValueFactory(this::modCell);
    modsColumn.setCellFactory(param -> new StringCell<>(String::toString));
    watchColumn.setCellFactory(param -> new NodeTableCell<>(this::watchReplayButton));

    liveReplayControllerRoot.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue)
        -> Platform.runLater(() -> selectedGame.set(newValue)));

    liveReplayControllerRoot.setItems(games);
  }

  private Button watchReplayButton(Number gameId) {
    return uiService.loadFxml("");
  }

  @Override
  public Node getRoot() {
    return liveReplayControllerRoot;
  }

  @NotNull
  private ObservableValue<String> modCell(CellDataFeatures<Game, String> param) {
    int simModCount = param.getValue().getSimMods().size();
    List<String> modNames = param.getValue().getSimMods().entrySet().stream()
        .limit(2)
        .map(Entry::getValue)
        .collect(Collectors.toList());
    if (simModCount > 2) {
      return new SimpleStringProperty(i18n.get("game.mods.twoAndMore", modNames.get(0), modNames.get(2)));
    }
    return new SimpleStringProperty(Joiner.on(i18n.get("textSeparator")).join(modNames));
  }

}
