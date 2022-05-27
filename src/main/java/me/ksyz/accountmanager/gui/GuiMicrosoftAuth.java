package me.ksyz.accountmanager.gui;

import me.ksyz.accountmanager.auth.MicrosoftAuth;
import me.ksyz.accountmanager.auth.SessionManager;
import me.ksyz.accountmanager.utils.Notification;
import me.ksyz.accountmanager.utils.TextFormatting;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GuiMicrosoftAuth extends GuiScreen {
  private final GuiScreen previousScreen;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  private GuiButton cancelButton = null;
  private CompletableFuture<Void> task = null;
  private String status = null;

  public GuiMicrosoftAuth(GuiScreen previousScreen) {
    this.previousScreen = previousScreen;
  }

  @Override
  public void initGui() {
    buttonList.clear();
    buttonList.add(cancelButton = new GuiButton(
      0, width / 2 - 100, height / 2 + fontRendererObj.FONT_HEIGHT / 2 + fontRendererObj.FONT_HEIGHT, "Cancel"
    ));

    if (task != null) {
      return;
    }
    status = "Check your browser to continue...";
    task = MicrosoftAuth
      .acquireMSAuthCode(success -> "Close this window and return to Minecraft!", executor)
      .thenComposeAsync(msAuthCode -> {
        status = "Acquiring Microsoft access token";
        return MicrosoftAuth.acquireMSAccessToken(msAuthCode, executor);
      })
      .thenComposeAsync(msAccessToken -> {
        status = "Acquiring Xbox access token";
        return MicrosoftAuth.acquireXboxAccessToken(msAccessToken, executor);
      })
      .thenComposeAsync(xboxAccessToken -> {
        status = "Acquiring Xbox XSTS token";
        return MicrosoftAuth.acquireXboxXstsToken(xboxAccessToken, executor);
      })
      .thenComposeAsync(xboxXstsData -> {
        status = "Acquiring Minecraft access token";
        return MicrosoftAuth.acquireMCAccessToken(
          xboxXstsData.get("Token"), xboxXstsData.get("uhs"), executor
        );
      })
      .thenComposeAsync(mcToken -> {
        status = "Fetching your Minecraft profile";
        return MicrosoftAuth.login(mcToken, executor);
      })
      .thenAccept(session -> {
        SessionManager.setSession(session);
        Notification.setNotification(
          String.format("Successful login! (%s)", session.getUsername()),
          TextFormatting.GREEN.getRGB()
        );
        actionPerformed(cancelButton);
      })
      .exceptionally(error -> {
        status = String.format("&c%s&r", error.getMessage());
        return null;
      });
  }

  @Override
  public void onGuiClosed() {
    if (task != null && !task.isDone()) {
      task.cancel(true);
      executor.shutdownNow();
    }
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    drawCenteredString(
      fontRendererObj, "Microsoft Authentication",
      width / 2, height / 2 - fontRendererObj.FONT_HEIGHT / 2 - fontRendererObj.FONT_HEIGHT * 2, 11184810
    );
    if (status != null) {
      drawCenteredString(
        fontRendererObj, TextFormatting.translate(status),
        width / 2, height / 2 - fontRendererObj.FONT_HEIGHT / 2, -1
      );
    }
    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  @Override
  protected void actionPerformed(GuiButton button) {
    if (button != null && button.id == 0) {
      mc.displayGuiScreen(new GuiAccountManager(previousScreen));
    }
  }

  @Override
  protected void keyTyped(char typedChar, int keyCode) {
    if (keyCode == Keyboard.KEY_ESCAPE) {
      actionPerformed(cancelButton);
    }
  }
}
