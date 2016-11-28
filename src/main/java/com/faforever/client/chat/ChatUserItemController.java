package com.faforever.client.chat;

import com.faforever.client.chat.avatar.AvatarService;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.game.Game;
import com.faforever.client.game.JoinGameHelper;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.ReportAction;
import com.faforever.client.notification.Severity;
import com.faforever.client.player.Player;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.google.common.eventbus.EventBus;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.WeakMapChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.PopupWindow;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import static com.faforever.client.chat.ChatColorMode.CUSTOM;
import static com.faforever.client.chat.SocialStatus.SELF;
import static com.faforever.client.game.PlayerStatus.IDLE;
import static com.faforever.client.game.PlayerStatus.PLAYING;
import static com.faforever.client.util.RatingUtil.getGlobalRating;
import static com.faforever.client.util.RatingUtil.getLeaderboardRating;
import static java.time.Instant.now;
import static java.util.Collections.singletonList;
import static java.util.Locale.US;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
// TODO null safety for "player"
public class ChatUserItemController implements Controller<Node> {

  private static final String CLAN_TAG_FORMAT = "[%s]";

  public Pane chatUserItemRoot;
  public ImageView countryImageView;
  public ImageView avatarImageView;
  public Label usernameLabel;
  public Label clanLabel;
  public Label statusLabel;

  @Inject
  AvatarService avatarService;
  @Inject
  CountryFlagService countryFlagService;
  @Inject
  PreferencesService preferencesService;
  @Inject
  ChatService chatService;
  @Inject
  ReplayService replayService;
  @Inject
  I18n i18n;
  @Inject
  UiService uiService;
  @Inject
  NotificationService notificationService;
  @Inject
  ReportingService reportingService;
  @Inject
  JoinGameHelper joinGameHelper;
  @Inject
  EventBus eventBus;
  @Inject
  TimeService timeService;

  private Player player;
  private boolean colorsAllowedInPane;
  private ChangeListener<ChatColorMode> colorModeChangeListener;
  private MapChangeListener<? super String, ? super Color> colorPerUserMapChangeListener;
  private ChangeListener<String> avatarChangeListener;
  private ChangeListener<String> clanChangeListener;
  private ChangeListener<PlayerStatus> gameStatusChangeListener;
  private ChangeListener<Instant> idleSinceListener;

  public ChatUserItemController() {
    idleSinceListener = (observable, oldValue, newValue) -> Platform.runLater(this::updateIdleTime);
  }

  public void initialize() {
    chatUserItemRoot.setUserData(this);
    countryImageView.managedProperty().bind(countryImageView.visibleProperty());
    countryImageView.setVisible(false);
    statusLabel.managedProperty().bind(statusLabel.visibleProperty());
    statusLabel.visibleProperty().bind(statusLabel.textProperty().isNotEmpty());

    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();

    colorModeChangeListener = (observable, oldValue, newValue) -> configureColor();
    colorPerUserMapChangeListener = change -> {
      String lowerUsername = player.getUsername().toLowerCase(US);
      if (lowerUsername.equalsIgnoreCase(change.getKey())) {
        Color newColor = chatPrefs.getUserToColor().get(lowerUsername);
        assignColor(newColor);
      }
    };
    avatarChangeListener = (observable, oldValue, newValue) -> Platform.runLater(() -> setAvatarUrl(newValue));
    clanChangeListener = (observable, oldValue, newValue) -> Platform.runLater(() -> setClanTag(newValue));
    gameStatusChangeListener = (observable, oldValue, newValue) -> Platform.runLater(this::updateGameStatus);
    joinGameHelper.setParentNode(getRoot());
  }

  public void onContextMenuRequested(ContextMenuEvent event) {
    ChatUserContextMenuController contextMenuController = uiService.loadFxml("theme/chat/chat_user_context_menu.fxml");
    contextMenuController.setPlayer(player);
    contextMenuController.getContextMenu().show(chatUserItemRoot.getScene().getWindow(), event.getScreenX(), event.getScreenY());
  }

  public void onUsernameClicked(MouseEvent mouseEvent) {
    if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 2) {
      eventBus.post(new InitiatePrivateChatEvent(player.getUsername()));
    }
  }

  private void configureColor() {
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();

    if (player.getSocialStatus() == SELF) {
      usernameLabel.getStyleClass().add(SELF.getCssClass());
      clanLabel.getStyleClass().add(SELF.getCssClass());
      return;
    }

    Color color = null;
    String lowerUsername = player.getUsername().toLowerCase(US);
    ChatUser chatUser = chatService.getOrCreateChatUser(lowerUsername);

    if (chatPrefs.getChatColorMode() == CUSTOM) {
      synchronized (chatPrefs.getUserToColor()) {
        if (chatPrefs.getUserToColor().containsKey(lowerUsername)) {
          color = chatPrefs.getUserToColor().get(lowerUsername);
        }

        chatPrefs.getUserToColor().addListener(new WeakMapChangeListener<>(colorPerUserMapChangeListener));
      }
    } else if (chatPrefs.getChatColorMode() == ChatColorMode.RANDOM && colorsAllowedInPane) {
      color = ColorGeneratorUtil.generateRandomColor(chatUser.getUsername().hashCode());
    }

    chatUser.setColor(color);
    assignColor(color);
  }

  private void assignColor(Color color) {
    if (color != null) {
      usernameLabel.setStyle(String.format("-fx-text-fill: %s", JavaFxUtil.toRgbCode(color)));
      clanLabel.setStyle(String.format("-fx-text-fill: %s", JavaFxUtil.toRgbCode(color)));
    } else {
      usernameLabel.setStyle("");
      clanLabel.setStyle("");
    }
  }

  private void setAvatarUrl(String avatarUrl) {
    if (StringUtils.isEmpty(avatarUrl)) {
      avatarImageView.setVisible(false);
    } else {
      avatarImageView.setImage(avatarService.loadAvatar(avatarUrl));
      avatarImageView.setVisible(true);
    }
  }

  private void setClanTag(String newValue) {
    if (StringUtils.isEmpty(newValue)) {
      clanLabel.setVisible(false);
    } else {
      clanLabel.setText(String.format(CLAN_TAG_FORMAT, newValue));
      clanLabel.setVisible(true);
    }
  }

  private void updateGameStatus() {
    if (this.player == null || player.getGame() == null) {
      statusLabel.setText("");
      return;
    }

    Game game = player.getGame();

    switch (player.getStatus()) {
      case IDLE:
        statusLabel.setText("");
        break;
      case HOSTING:
        statusLabel.setText(i18n.get("user.status.hosting", game.getTitle()));
        break;
      case LOBBYING:
        statusLabel.setText(i18n.get("user.status.waiting", game.getTitle()));
        break;
      case PLAYING:
        statusLabel.setText(i18n.get("user.status.playing", game.getTitle()));
        break;
    }
  }

  public Pane getRoot() {
    return chatUserItemRoot;
  }

  public Player getPlayer() {
    return player;
  }

  public void setPlayer(Player player) {
    this.player = player;

    configureColor();
    addChatColorModeListener();
    configureCountryImageView();
    configureAvatarImageView();
    configureClanLabel();
    configureGameStatusView();
    Platform.runLater(this::updateIdleTime);

    usernameLabel.setText(player.getUsername());
    player.idleSinceProperty().addListener(new WeakChangeListener<>(idleSinceListener));
  }

  private void addChatColorModeListener() {
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    synchronized (chatPrefs.chatColorModeProperty()) {
      chatPrefs.chatColorModeProperty().addListener(new WeakChangeListener<>(colorModeChangeListener));
    }
  }

  private void configureCountryImageView() {
    setCountry(player.getCountry());

    countryImageView.setVisible(true);

    Tooltip countryTooltip = new Tooltip(player.getCountry());
    countryTooltip.textProperty().bind(player.countryProperty());

    Tooltip.install(countryImageView, countryTooltip);
  }

  private void configureAvatarImageView() {
    player.avatarUrlProperty().addListener(new WeakChangeListener<>(avatarChangeListener));
    setAvatarUrl(player.getAvatarUrl());

    Tooltip avatarTooltip = new Tooltip(player.getAvatarTooltip());
    avatarTooltip.textProperty().bind(player.avatarTooltipProperty());
    avatarTooltip.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_LEFT);

    Tooltip.install(avatarImageView, avatarTooltip);
  }

  private void configureClanLabel() {
    setClanTag(player.getClan());
    player.clanProperty().addListener(new WeakChangeListener<>(clanChangeListener));
  }

  private void configureGameStatusView() {
    player.statusProperty().addListener(new WeakChangeListener<>(gameStatusChangeListener));
    updateGameStatus();
  }

  private void setCountry(String country) {
    if (StringUtils.isEmpty(country)) {
      countryImageView.setVisible(false);
    } else {
      countryImageView.setImage(countryFlagService.loadCountryFlag(country));
      countryImageView.setVisible(true);
    }
  }

  public void onMouseEnterUsername() {
    if (player.getChatOnly() || usernameLabel.getTooltip() != null) {
      return;
    }

    Tooltip tooltip = new Tooltip();
    Label label = new Label();
    tooltip.setGraphic(label);
    Tooltip.install(usernameLabel, tooltip);
    Tooltip.install(clanLabel, tooltip);

    label.textProperty().bind(Bindings.createStringBinding(
        () -> i18n.get("userInfo.ratingFormat", getGlobalRating(player), getLeaderboardRating(player)),
        player.leaderboardRatingMeanProperty(), player.leaderboardRatingDeviationProperty(),
        player.globalRatingMeanProperty(), player.globalRatingDeviationProperty()
    ));
  }

  public   // TODO use or remove
  void onMouseClickGameStatus(MouseEvent mouseEvent) {
    PlayerStatus playerStatus = player.getStatus();
    if (playerStatus == PlayerStatus.IDLE) {
      return;
    }
    if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 2) {
      if (playerStatus == PlayerStatus.LOBBYING || playerStatus == PlayerStatus.HOSTING) {
        joinGameHelper.join(player.getGame());
      } else if (playerStatus == PLAYING) {
        try {
          replayService.runLiveReplay(player.getGame().getId(), player.getId());
        } catch (IOException e) {
          notificationService.addNotification(new ImmediateNotification(
              i18n.get("errorTitle"), i18n.get("replayCouldNotBeStarted"),
              Severity.ERROR, e, singletonList(new ReportAction(i18n, reportingService, e))
          ));
        }
      }
    }
  }

  void setColorsAllowedInPane(boolean colorsAllowedInPane) {
    this.colorsAllowedInPane = colorsAllowedInPane;
    configureColor();
  }

  public void setVisible(boolean visible) {
    chatUserItemRoot.setVisible(visible);
    chatUserItemRoot.setManaged(visible);
  }

  void updateIdleTime() {
    JavaFxUtil.assertApplicationThread();
    if (player == null || player.getStatus() != IDLE) {
      return;
    }
    if (player.getIdleSince().isBefore(now().minus(Duration.ofMinutes(10)))) {
      statusLabel.setText(i18n.get("user.status.idle", timeService.shortDuration(Duration.between(player.getIdleSince(), now()))));
    } else {
      statusLabel.setText("");
    }
  }
}
