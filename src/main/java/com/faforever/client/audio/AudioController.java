package com.faforever.client.audio;

public interface AudioController {

  void playChatMentionSound();

  void playPrivateMessageSound();

  void playInfoNotificationSound();

  void playWarnNotificationSound();

  void playErrorNotificationSound();

  void playAchievementUnlockedSound();

  void playFriendOnlineSound();

  void playFriendOfflineSound();

  void playFriendJoinsGameSound();

  void playFriendPlaysGameSound();
}
