package com.x1f4r.mmocraft.playerdata.listeners;

import com.x1f4r.mmocraft.playerdata.PlayerDataService;
import com.x1f4r.mmocraft.playerdata.model.PlayerProfile;
import com.x1f4r.mmocraft.util.LoggingUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerJoinQuitListenerTest {

    @Mock
    private PlayerDataService mockPlayerDataService;
    @Mock
    private LoggingUtil mockLogger;
    @Mock
    private Player mockPlayer;

    private PlayerJoinQuitListener listener;

    private UUID testPlayerUUID;
    private String testPlayerName;

    @BeforeEach
    void setUp() {
        listener = new PlayerJoinQuitListener(mockPlayerDataService, mockLogger);
        testPlayerUUID = UUID.randomUUID();
        testPlayerName = "TestListenerPlayer";

        // Mock player
        when(mockPlayer.getUniqueId()).thenReturn(testPlayerUUID);
        when(mockPlayer.getName()).thenReturn(testPlayerName);
    }

    @Test
    void onAsyncPlayerPreLogin_allowed_shouldLoadProfile() {
        // Arrange
        AsyncPlayerPreLoginEvent mockEvent = new AsyncPlayerPreLoginEvent(
            testPlayerName,
            mock(InetAddress.class), // Mock InetAddress, actual value doesn't matter for this test
            testPlayerUUID
        );
        // Ensure login is allowed
        mockEvent.setLoginResult(AsyncPlayerPreLoginEvent.Result.ALLOWED);

        PlayerProfile mockProfile = new PlayerProfile(testPlayerUUID, testPlayerName);
        when(mockPlayerDataService.loadPlayerProfile(testPlayerUUID, testPlayerName))
            .thenReturn(CompletableFuture.completedFuture(mockProfile));

        // Act
        listener.onAsyncPlayerPreLogin(mockEvent);

        // Assert
        verify(mockPlayerDataService).loadPlayerProfile(testPlayerUUID, testPlayerName);
        verify(mockLogger).info(contains("Profile loaded successfully for " + testPlayerName));
    }

    @Test
    void onAsyncPlayerPreLogin_allowed_profileLoadFails_shouldLogSevere() {
        AsyncPlayerPreLoginEvent mockEvent = new AsyncPlayerPreLoginEvent(testPlayerName, mock(InetAddress.class), testPlayerUUID);
        mockEvent.setLoginResult(AsyncPlayerPreLoginEvent.Result.ALLOWED);

        CompletableFuture<PlayerProfile> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("DB error"));

        when(mockPlayerDataService.loadPlayerProfile(testPlayerUUID, testPlayerName))
            .thenReturn(failedFuture);

        listener.onAsyncPlayerPreLogin(mockEvent);

        verify(mockLogger).severe(contains("Exception during profile load for " + testPlayerName), any(RuntimeException.class));
        // verify(mockEvent, atLeastOnce()).getLoginResult(); // Check that disallow wasn't called if that's the policy
    }


    @Test
    void onAsyncPlayerPreLogin_notAllowed_shouldNotLoadProfile() {
        // Arrange
        AsyncPlayerPreLoginEvent mockEvent = new AsyncPlayerPreLoginEvent(testPlayerName, mock(InetAddress.class), testPlayerUUID);
        mockEvent.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_BANNED); // Any non-ALLOWED result

        // Act
        listener.onAsyncPlayerPreLogin(mockEvent);

        // Assert
        verify(mockPlayerDataService, never()).loadPlayerProfile(any(UUID.class), anyString());
    }

    @Test
    void onPlayerQuit_shouldSaveAndUncacheProfile() {
        // Arrange
        PlayerQuitEvent mockEvent = new PlayerQuitEvent(mockPlayer, "quit message");
        when(mockPlayerDataService.savePlayerProfile(testPlayerUUID))
            .thenReturn(CompletableFuture.completedFuture(null)); // Simulate successful save

        // Act
        listener.onPlayerQuit(mockEvent);

        // Assert
        // Wait for CompletableFuture to complete (in a real test with thread pools, might need Awaitility)
        // For now, as it's completedFuture, it's synchronous in this mock setup.
        verify(mockPlayerDataService).savePlayerProfile(testPlayerUUID);
        verify(mockPlayerDataService).uncachePlayerProfile(testPlayerUUID); // This is called in thenRun
        verify(mockLogger).info(contains("Profile saved successfully for " + testPlayerName));
    }

    @Test
    void onPlayerQuit_saveFails_shouldLogErrorAndStillUncache() {
        PlayerQuitEvent mockEvent = new PlayerQuitEvent(mockPlayer, "quit message");
        CompletableFuture<Void> failedSave = new CompletableFuture<>();
        failedSave.completeExceptionally(new RuntimeException("Save failed"));

        when(mockPlayerDataService.savePlayerProfile(testPlayerUUID)).thenReturn(failedSave);

        listener.onPlayerQuit(mockEvent);

        verify(mockPlayerDataService).savePlayerProfile(testPlayerUUID);
        verify(mockLogger).severe(contains("Failed to save profile for " + testPlayerName), any(RuntimeException.class));
        verify(mockPlayerDataService).uncachePlayerProfile(testPlayerUUID); // Should still uncache
    }
}
