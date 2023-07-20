package me.ksyz.accountmanager;


import com.google.gson.*;
import me.ksyz.accountmanager.auth.Account;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

@Mod(modid = "accountmanager", version = "@VERSION@", clientSideOnly = true, acceptedMinecraftVersions = "1.8.9")
public class AccountManager {
  private static final Minecraft mc = Minecraft.getMinecraft();
  private static final ArrayList<Account> accounts = new ArrayList<>();
  private static final File file = new File(mc.mcDataDir, "accounts.json");
  private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  @EventHandler
  public static void init(FMLInitializationEvent event) {
    MinecraftForge.EVENT_BUS.register(new Events());

    if (!file.exists()) {
      try {
        if (file.getParentFile().exists() || file.getParentFile().mkdirs()) {
          if (file.createNewFile()) {
            System.out.print("Successfully created accounts.json!");
          }
        }
      } catch (IOException e) {
        System.err.print("Couldn't create accounts.json!");
      }
    }
  }

  public static void load() {
    accounts.clear();
    try {
      JsonElement json = new JsonParser().parse(
        new BufferedReader(new FileReader(file))
      );
      if (json instanceof JsonArray) {
        JsonArray jsonArray = json.getAsJsonArray();
        for (JsonElement jsonElement : jsonArray) {
          JsonObject jsonObject = jsonElement.getAsJsonObject();
          accounts.add(new Account(
            Optional.ofNullable(jsonObject.get("refreshToken")).map(JsonElement::getAsString).orElse(""),
            Optional.ofNullable(jsonObject.get("accessToken")).map(JsonElement::getAsString).orElse(""),
            Optional.ofNullable(jsonObject.get("username")).map(JsonElement::getAsString).orElse(""),
            Optional.ofNullable(jsonObject.get("timestamp")).map(JsonElement::getAsLong).orElse(System.currentTimeMillis())
          ));
        }
      }
    } catch (FileNotFoundException e) {
      System.err.print("Couldn't find accounts.json!");
    }
  }

  public static void save() {
    try {
      JsonArray jsonArray = new JsonArray();
      for (Account account : accounts) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("refreshToken", account.getRefreshToken());
        jsonObject.addProperty("accessToken", account.getAccessToken());
        jsonObject.addProperty("username", account.getUsername());
        jsonObject.addProperty("timestamp", account.getTimestamp());
        jsonArray.add(jsonObject);
      }
      PrintWriter printWriter = new PrintWriter(new FileWriter(file));
      printWriter.println(gson.toJson(jsonArray));
      printWriter.close();
    } catch (IOException e) {
      System.err.print("Couldn't save accounts.json!");
    }
  }

  public static Account get(int index) {
    return accounts.get(index);
  }

  public static void add(Account account) {
    accounts.add(account);
  }

  public static void remove(int index) {
    accounts.remove(index);
  }

  public static int size() {
    return accounts.size();
  }

  public static void swap(int i, int j) {
    Collections.swap(accounts, i, j);
  }
}
