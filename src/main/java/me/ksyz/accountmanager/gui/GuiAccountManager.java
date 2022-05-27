package me.ksyz.accountmanager.gui;

import com.mojang.authlib.exceptions.AuthenticationException;
import me.ksyz.accountmanager.AccountManager;
import me.ksyz.accountmanager.account.Account;
import me.ksyz.accountmanager.account.LegacyAccount;
import me.ksyz.accountmanager.account.MojangAccount;
import me.ksyz.accountmanager.utils.Notification;
import me.ksyz.accountmanager.utils.TextFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class GuiAccountManager extends GuiScreen {
  private static final AccountManager am = AccountManager.getAccountManager();

  private final GuiScreen previousScreen;
  private int selectedAccount = -1;

  private GuiAccountManager.List guiAccountList;
  private GuiButton loginButton;
  private GuiButton editButton;
  private GuiButton deleteButton;

  public GuiAccountManager(GuiScreen previousScreen) {
    this.previousScreen = previousScreen;
  }

  @Override
  public void initGui() {
    Keyboard.enableRepeatEvents(true);
    this.buttonList.clear();

    // Top Row
    this.buttonList.add(loginButton = new GuiButton(
      0, this.width / 2 - 154 - 10, this.height - 52, 120, 20, "Login"
    ));
    this.buttonList.add(editButton = new GuiButton(
      1, this.width / 2 - 40, this.height - 52, 80, 20, "Edit"
    ));
    this.buttonList.add(new GuiButton(
      2, this.width / 2 + 4 + 40, this.height - 52, 120, 20, "Add"
    ));

    // Bottom Row
    this.buttonList.add(new GuiButton(
      3, this.width / 2 - 154 - 10, this.height - 28, 110, 20, "Import"
    ));
    this.buttonList.add(deleteButton = new GuiButton(
      4, this.width / 2 - 50, this.height - 28, 100, 20, "Delete"
    ));
    this.buttonList.add(new GuiButton(
      5, this.width / 2 + 4 + 50, this.height - 28, 110, 20, "Cancel"
    ));

    buttonList.add(new GuiButton(
      6, width - 106, 6, 100, 20, "Microsoft"
    ));

    // Account List
    guiAccountList = new GuiAccountManager.List(mc);
    guiAccountList.registerScrollButtons(11, 12);
    updateButtons();
  }

  @Override
  public void handleMouseInput() throws IOException {
    super.handleMouseInput();
    this.guiAccountList.handleMouseInput();
  }

  @Override
  public void updateScreen() {
    updateButtons();
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    super.mouseClicked(mouseX, mouseY, mouseButton);
  }

  @Override
  public void onGuiClosed() {
    am.save();
    Notification.resetNotification();
    Keyboard.enableRepeatEvents(false);
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float renderPartialTicks) {
    guiAccountList.drawScreen(mouseX, mouseY, renderPartialTicks);
    super.drawScreen(mouseX, mouseY, renderPartialTicks);

    // Action message
    this.drawCenteredString(
      fontRendererObj,
      TextFormatting.RESET + "Account Manager " +
      TextFormatting.DARK_GRAY + "(" +
      TextFormatting.GRAY + am.getAccounts().size() +
      TextFormatting.DARK_GRAY + ")" + TextFormatting.RESET,
      this.width / 2, 20, -1
    );
  }

  @Override
  protected void keyTyped(char character, int keyIndex) {
    switch (keyIndex) {
      case Keyboard.KEY_UP:
        if (selectedAccount > 0) {
          --selectedAccount;
        }
        break;
      case Keyboard.KEY_DOWN:
        if (selectedAccount < am.getAccounts().size() - 1) {
          ++selectedAccount;
        }
        break;
      case Keyboard.KEY_RETURN:
        if (loginButton.enabled) {
          login();
        }
        break;
      case Keyboard.KEY_DELETE:
        if (deleteButton.enabled) {
          deleteAccount();
        }
        break;
      case Keyboard.KEY_ESCAPE:
        cancel();
        break;
    }

    if (GuiScreen.isKeyComboCtrlC(keyIndex) && selectedAccount >= 0) {
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      Account account = am.getAccounts().get(selectedAccount);
      if (account instanceof LegacyAccount) {
        clipboard.setContents(new StringSelection(account.getUsername()), null);
      } else if (account instanceof MojangAccount) {
        clipboard.setContents(new StringSelection(
          ((MojangAccount) account).getEmail() + ":" + ((MojangAccount) account).getPassword()
        ), null);
      }
    } else if (GuiScreen.isKeyComboCtrlV(keyIndex)) {
      importAccount();
    }
  }

  @Override
  protected void actionPerformed(GuiButton button) {
    if (button.enabled) {
      switch (button.id) {
        case 0:
          login();
          break;
        case 1:
          editAccount();
          break;
        case 2:
          addAccount();
          break;
        case 3:
          importAccount();
          break;
        case 4:
          deleteAccount();
          break;
        case 5:
          cancel();
          break;
        case 6:
          mc.displayGuiScreen(new GuiMicrosoftAuth(previousScreen));
        default:
          guiAccountList.actionPerformed(button);
      }
    }
  }

  private void login() {
    Account acc = am.getAccounts().get(selectedAccount);
    try {
      am.login(acc);
      final String username = acc.getUsername();
      Notification.setNotification(
        String.format("Successful login!%s", StringUtils.isBlank(username) ? "" : String.format(" (%s)", username)),
        TextFormatting.GREEN.getRGB()
      );
      mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("note.pling")));
    } catch (AuthenticationException e) {
      final String username = acc.getUsername();
      Notification.setNotification(
        String.format("%s%s", e.getMessage(), StringUtils.isBlank(username) ? "" : String.format(" (%s)", username)),
        TextFormatting.RED.getRGB()
      );
      mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("note.bass")));
    }
  }

  private void editAccount() {
    if (selectedAccount >= 0) {
      mc.displayGuiScreen(new GuiEdit(previousScreen, selectedAccount));
    }
  }

  private void addAccount() {
    mc.displayGuiScreen(new GuiAdd(previousScreen));
  }

  private void importAccount() {
    String clipboardData = "";
    try {
      clipboardData = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
    } catch (HeadlessException | UnsupportedFlavorException | IOException e) {
      e.printStackTrace();
    }

    String[] lines = clipboardData.split("\\r?\\n");
    for (String line : lines) {
      if (line.contains(":")) {
        String[] combo = line.split(":");
        MojangAccount account = new MojangAccount(combo[0], combo[1]);
        if (!am.isAccountInList(account)) {
          am.getAccounts().add(account);
        }
      }
    }
  }

  private void deleteAccount() {
    am.getAccounts().remove(selectedAccount);
    --selectedAccount;
    updateButtons();
  }

  private void cancel() {
    mc.displayGuiScreen(previousScreen);
  }

  private void updateButtons() {
    loginButton.enabled = (
      selectedAccount >= 0 && !mc.getSession().getUsername().equals(am.getAccounts().get(selectedAccount).getUsername())
    );
    editButton.enabled = selectedAccount >= 0;
    deleteButton.enabled = selectedAccount >= 0;
  }

  class List extends GuiSlot {
    public List(Minecraft mc) {
      super(mc, GuiAccountManager.this.width, GuiAccountManager.this.height, 32, GuiAccountManager.this.height - 64,
        27);
    }

    @Override
    protected int getSize() {
      return am.getAccounts().size();
    }

    @Override
    protected void elementClicked(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY) {
      GuiAccountManager.this.selectedAccount = slotIndex;
      GuiAccountManager.this.updateButtons();

      if (isDoubleClick && loginButton.enabled) {
        GuiAccountManager.this.login();
      }
    }

    @Override
    protected boolean isSelected(int slotIndex) {
      return slotIndex == GuiAccountManager.this.selectedAccount;
    }

    @Override
    protected int getContentHeight() {
      return am.getAccounts().size() * 27;
    }

    @Override
    protected void drawBackground() {
      GuiAccountManager.this.drawDefaultBackground();
    }

    @Override
    protected void drawSlot(
      int entryID, int p_180791_2_, int p_180791_3_, int p_180791_4_, int mouseXIn, int mouseYIn
    ) {
      Account account = am.getAccounts().get(entryID);

      String username = account.getUsername();
      if ("".equals(username)) {
        username = "???";
      } else if (mc.getSession().getUsername().equals(username)) {
        username = (username + TextFormatting.GREEN + " \u2714" + TextFormatting.RESET);
      }

      String info = "";
      if (account instanceof LegacyAccount) {
        info = TextFormatting.DARK_GRAY + "Offline" + TextFormatting.RESET;
      } else if (account instanceof MojangAccount) {
        info = TextFormatting.GRAY + ((MojangAccount) account).getEmail() + TextFormatting.RESET;
      }

      GuiAccountManager.this.drawString(
        GuiAccountManager.this.fontRendererObj, username,
        p_180791_2_ + 2, p_180791_3_ + 2, -1
      );
      GuiAccountManager.this.drawString(
        GuiAccountManager.this.fontRendererObj, info,
        p_180791_2_ + 2, p_180791_3_ + 13, -1
      );
    }
  }
}
