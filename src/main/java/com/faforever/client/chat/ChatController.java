package com.faforever.client.chat;

import com.faforever.client.chat.event.ChatMessageEvent;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.ProgrammingError;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ChatController extends AbstractViewController<Node> {

  private final Map<String, AbstractChatTabController> nameToChatTabController;
  public Node chatRoot;
  public TabPane tabPane;
  public Pane connectingProgressPane;
  public VBox noOpenTabsContainer;
  public TextField channelNameTextField;
  @Inject
  ChatService chatService;
  @Inject
  UiService uiService;
  @Inject
  UserService userService;
  @Inject
  EventBus eventBus;

  public ChatController() {
    nameToChatTabController = new HashMap<>();
  }

  private void onChannelLeft(Channel channel) {
    removeTab(channel.getName());
  }

  private void onChannelJoined(Channel channel) {
    Platform.runLater(() -> getOrCreateChannelTab(channel.getName()));
  }

  private void onDisconnected() {
    connectingProgressPane.setVisible(true);
    tabPane.setVisible(false);
    noOpenTabsContainer.setVisible(false);
  }

  private void onConnected() {
    connectingProgressPane.setVisible(false);
    tabPane.setVisible(true);
    noOpenTabsContainer.setVisible(false);
  }

  private void onConnecting() {
    connectingProgressPane.setVisible(true);
    tabPane.setVisible(false);
    noOpenTabsContainer.setVisible(false);
  }

  private void onLoggedOut() {
    Platform.runLater(() -> tabPane.getTabs().clear());
  }

  private void removeTab(String playerOrChannelName) {
    nameToChatTabController.remove(playerOrChannelName);

    if (nameToChatTabController.containsKey(playerOrChannelName)) {
      tabPane.getTabs().remove(nameToChatTabController.remove(playerOrChannelName).getRoot());
    }
  }

  private AbstractChatTabController getOrCreateChannelTab(String channelName) {
    if (!nameToChatTabController.containsKey(channelName)) {
      Platform.runLater(() -> {
        ChannelTabController tab = uiService.loadFxml("theme/channel_tab.fxml");
        tab.setChannel(chatService.getOrCreateChannel(channelName));
        addTab(channelName, tab);
      });
    }
    return nameToChatTabController.get(channelName);
  }

  private void addTab(String playerOrChannelName, AbstractChatTabController tabController) {
    JavaFxUtil.assertApplicationThread();
    nameToChatTabController.put(playerOrChannelName, tabController);
    Platform.runLater(() -> tabPane.getTabs().add(tabController.getRoot()));
  }

  @Override
  public void initialize() {
    super.initialize();
    eventBus.register(this);

    tabPane.getTabs().addListener((InvalidationListener) observable ->
        noOpenTabsContainer.setVisible(tabPane.getTabs().isEmpty()));

    chatService.addChannelsListener(change -> {
      if (change.wasRemoved()) {
        onChannelLeft(change.getValueRemoved());
      }
      if (change.wasAdded()) {
        onChannelJoined(change.getValueAdded());
      }
    });

    chatService.connectionStateProperty().addListener((observable, oldValue, newValue) -> onConnectionStateChange(newValue));
    onConnectionStateChange(chatService.connectionStateProperty().get());

    userService.loggedInProperty().addListener((observable, oldValue, newValue) -> onLoggedInChanged(newValue));
    onLoggedInChanged(userService.loggedInProperty().get());


    tabPane.getTabs().addListener((ListChangeListener<Tab>) change -> {
      while (change.next()) {
        change.getRemoved().forEach(tab -> nameToChatTabController.remove(tab.getId()));
      }
    });
  }

  private void onLoggedInChanged(Boolean newValue) {
    if (!newValue) {
      onLoggedOut();
    }
  }

  private void onConnectionStateChange(ConnectionState newValue) {
    switch (newValue) {
      case DISCONNECTED:
        onDisconnected();
        break;
      case CONNECTED:
        onConnected();
        break;
      case CONNECTING:
        onConnecting();
        break;
      default:
        throw new ProgrammingError("Uncovered connection state: " + newValue);
    }
  }

  @Subscribe
  public void onChatMessage(ChatMessageEvent event) {
    ChatMessage message = event.getMessage();
    if (Objects.toString(message.getSource(), "").startsWith("#")) {
      getOrCreateChannelTab(message.getSource()).onChatMessage(message);
    } else {
      addAndGetPrivateMessageTab(message.getSource()).onChatMessage(message);
    }
  }

  private AbstractChatTabController addAndGetPrivateMessageTab(String username) {
    if (!nameToChatTabController.containsKey(username)) {
      Platform.runLater(() -> {
        PrivateChatTabController tab = uiService.loadFxml("theme/private_chat_tab.fxml");
        tab.setReceiver(username);
        addTab(username, tab);
      });
    }

    return nameToChatTabController.get(username);
  }

  public Node getRoot() {
    return chatRoot;
  }

  @Subscribe
  public void onInitiatePrivateChatEvent(InitiatePrivateChatEvent event) {
    openPrivateMessageTabForUser(event.getUsername());
  }

  private void openPrivateMessageTabForUser(String username) {
    if (username.equalsIgnoreCase(userService.getUsername())) {
      return;
    }
    AbstractChatTabController controller = addAndGetPrivateMessageTab(username);
    tabPane.getSelectionModel().select(controller.getRoot());
  }

  public void onJoinChannelButtonClicked() {
    String channelName = channelNameTextField.getText();
    channelNameTextField.clear();

    joinChannel(channelName);
  }

  private void joinChannel(String channelName) {
    chatService.addUsersListener(channelName, change -> {
      if (change.wasRemoved()) {
        onChatUserLeftChannel(channelName, change.getValueRemoved().getUsername());
      }
      if (change.wasAdded()) {
        onUserJoinedChannel(channelName, change.getValueAdded());
      }
    });
    chatService.joinChannel(channelName);
  }

  private void onChatUserLeftChannel(String channelName, String username) {
    if (!username.equalsIgnoreCase(userService.getUsername())) {
      return;
    }
    AbstractChatTabController chatTab = nameToChatTabController.get(channelName);
    if (chatTab != null) {
      tabPane.getTabs().remove(chatTab.getRoot());
    }
  }

  private void onUserJoinedChannel(String channelName, ChatUser chatUser) {
    Platform.runLater(() -> {
      if (isCurrentUser(chatUser)) {
        onConnected();
      }
    });
  }

  private boolean isCurrentUser(ChatUser chatUser) {
    return chatUser.getUsername().equalsIgnoreCase(userService.getUsername());
  }

  @Override
  protected void onDisplay() {
    super.onDisplay();
    if (!tabPane.getTabs().isEmpty()) {
      Tab tab = tabPane.getSelectionModel().getSelectedItem();
      nameToChatTabController.get(tab.getId()).onDisplay();
    }
  }

  @Override
  protected void onHide() {
    super.onHide();
    if (!tabPane.getTabs().isEmpty()) {
      Tab tab = tabPane.getSelectionModel().getSelectedItem();
      nameToChatTabController.get(tab.getId()).onHide();
    }
  }
}
