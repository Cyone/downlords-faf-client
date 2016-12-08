package com.faforever.client.user;

import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.LoginMessage;
import com.faforever.client.remote.domain.NoticeMessage;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.user.event.LogOutRequestEvent;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletionStage;

@Lazy
@Service
public class UserServiceImpl implements UserService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final StringProperty username;
  private final BooleanProperty loggedIn;
  @Inject
  FafService fafService;
  @Inject
  PreferencesService preferencesService;
  @Inject
  EventBus eventBus;
  @Inject
  ApplicationContext applicationContext;
  @Inject
  TaskService taskService;

  private String password;
  private Integer userId;
  private CompletionStage<Void> loginFuture;

  public UserServiceImpl() {
    loggedIn = new SimpleBooleanProperty();
    username = new SimpleStringProperty();
  }

  @Override
  public ReadOnlyBooleanProperty loggedInProperty() {
    return loggedIn;
  }

  @Override
  public CompletionStage<Void> login(String username, String password, boolean autoLogin) {
    preferencesService.getPreferences().getLogin()
        .setUsername(username)
        .setPassword(password)
        .setAutoLogin(autoLogin);
    preferencesService.storeInBackground();

    this.password = password;

    loginFuture = fafService.connectAndLogIn(username, password)
        .thenAccept(loginInfo -> {
          userId = loginInfo.getId();

          // Because of different case (upper/lower)
          String login = loginInfo.getLogin();
          UserServiceImpl.this.username.set(login);

          preferencesService.getPreferences().getLogin().setUsername(login);
          preferencesService.storeInBackground();

          loggedIn.set(true);
          eventBus.post(new LoginSuccessEvent(login, userId));
        })
        .whenComplete((aVoid, throwable) -> {
          if (throwable != null) {
            logger.warn("Error during login", throwable);
            fafService.disconnect();
          }
          loginFuture = null;
        });
    return loginFuture;
  }

  @Override
  public String getUsername() {
    return username.get();
  }

  @Override
  public String getPassword() {
    return password;
  }

  public Integer getUserId() {
    return userId;
  }

  @Override
  public void cancelLogin() {
    if (loginFuture != null) {
      loginFuture.toCompletableFuture().cancel(true);
      loginFuture = null;
      fafService.disconnect();
    }
  }

  @Override
  public void logOut() {
    logger.info("Logging out");
    fafService.disconnect();
    loggedIn.set(false);
  }

  @Override
  public ReadOnlyStringProperty currentUserProperty() {
    return username;
  }

  @Override
  public CompletableTask<Void> changePassword(String currentPassword, String newPassword) {
    ChangePasswordTask changePasswordTask = applicationContext.getBean(ChangePasswordTask.class);
    changePasswordTask.setCurrentPassword(currentPassword);
    changePasswordTask.setNewPassword(newPassword);

    return taskService.submitTask(changePasswordTask);
  }

  @PostConstruct
  void postConstruct() {
    fafService.addOnMessageListener(LoginMessage.class, loginInfo -> userId = loginInfo.getId());
    fafService.addOnMessageListener(NoticeMessage.class, noticeMessage -> cancelLogin());
  }

  @Subscribe
  public void onLogoutRequestEvent(LogOutRequestEvent event) {
    logOut();
  }
}
