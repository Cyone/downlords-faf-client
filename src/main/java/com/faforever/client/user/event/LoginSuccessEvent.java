package com.faforever.client.user.event;

public class LoginSuccessEvent {
  private final String username;
  private final int userId;

  public LoginSuccessEvent(String username, int userId) {
    this.username = username;
    this.userId = userId;
  }

  public String getUsername() {
    return username;
  }

  public int getUserId() {
    return userId;
  }
}
