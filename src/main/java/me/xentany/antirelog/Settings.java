package me.xentany.antirelog;

import net.elytrium.commons.config.YamlConfig;
import net.kyori.adventure.bossbar.BossBar;

import java.util.List;

public final class Settings extends YamlConfig {

  @Ignore
  public static final Settings IMP = new Settings();

  public BossBar.Color BOSSBAR_COLOR = BossBar.Color.RED;
  public BossBar.Overlay BOSSBAR_OVERLAY = BossBar.Overlay.PROGRESS;
  public int GOLDEN_APPLE_COOLDOWN = 30;
  public int ENCHANTED_GOLDEN_APPLE_COOLDOWN = 60;
  public int ENDER_PEARL_COOLDOWN = 15;
  public int CHORUS_COOLDOWN = 7;
  public int FIREWORK_COOLDOWN = 60;
  public int TOTEM_COOLDOWN = 60;
  public int PVP_TIME = 30;
  public boolean DISABLE_COMMANDS_IN_PVP = true;
  public List<String> COMMANDS_WHITELIST = List.of();
  public boolean CANCEL_INTERACT_WITH_ENTITIES = false;
  public boolean KILL_ON_LEAVE = true;
  public boolean KILL_ON_KICK = true;
  public boolean RUN_COMMANDS_ON_KICK = true;
  public List<String> KICK_MESSAGES = List.of("спам", "реклама", "анти-чит");
  public List<String> COMMANDS_ON_LEAVE = List.of();
  public boolean DISABLE_POWERUPS = true;
  public List<String> COMMANDS_ON_POWERUPS_DISABLE = List.of();
  public boolean DISABLE_TELEPORTS_IN_PVP = true;
  public boolean IGNORE_WORLDGUARD = true;
  public boolean JOIN_PVP_IN_WORLDGUARD = false;
  public List<String> IGNORED_WORLDGUARD_REGIONS = List.of();
  public boolean DISABLE_PVP_IN_IGNORED_REGION = false;
  public boolean HIDE_JOIN_MESSAGE = false;
  public boolean HIDE_LEAVE_MESSAGE = false;
  public boolean HIDE_DEATH_MESSAGE = false;
  public List<String> DISABLED_WORLDS = List.of("world1", "world2");

  @Create
  public MESSAGES MESSAGES;

  public static final class MESSAGES {

    public String PVP_STARTED = "<aqua>Вы начали <yellow><bold>PVP</bold></yellow><aqua>!</aqua>";
    public String PVP_STARTED_TITLE = "<aqua>AntiRelog</aqua>";
    public String PVP_STARTED_SUBTITLE = "Вы вошли в режим <yellow>PVP</yellow><green>!</green>";
    public String PVP_STOPPED = "<yellow><bold>PVP</bold></yellow> <aqua>окончено</aqua>";
    public String PVP_STOPPED_TITLE = "<aqua>AntiRelog</aqua>";
    public String PVP_STOPPED_SUBTITLE = "Вы вышли из режима <yellow>PVP</yellow><green>!</green>";
    public String PVP_STOPPED_ACTIONBAR = "<yellow><bold>PVP</bold></yellow> <green>окончено, Вы снова можете использовать команды и выходить из игры!</green>";
    public String IN_PVP_BOSSBAR = "<bold>Режим <red>PVP</red> - <green>{0}</green> сек.</bold>";
    public String IN_PVP_ACTIONBAR = "<reset><bold>Режим <red><bold>PVP</bold></red><reset><bold>, не выходите из игры <green><bold>{0}</bold></green> <reset><bold>сек</bold></reset>.";
    public String PVP_LEAVED = "<green>Игрок <red><bold>{0}</bold></red><green> покинул игру во время <aqua><bold>ПВП</bold></aqua><green> и был наказан.</green>";
    public String COMMANDS_DISABLED = "<aqua><bold>Вы не можете использовать команды в <yellow><bold>PvP</bold></yellow><aqua><bold>. Подождите <green><bold>{0}</bold></green> <aqua><bold>сек</bold></aqua>.</bold></aqua>";
    public String ITEM_COOLDOWN = "<aqua><bold>Вы сможете использовать этот предмет через <green><bold>{0}</bold></green> <aqua><bold>сек</bold></aqua>.</bold></aqua>";
    public String ITEM_DISABLED_IN_PVP = "<aqua><bold>Вы не можете использовать этот предмет в <yellow><bold>PVP</bold></yellow><aqua><bold> режиме</bold></aqua>";
    public String TOTEM_COOLDOWN = "<aqua><bold>Тотем небыл использован, т.к был недавно использован. Тотем будет доступен через <green><bold>{0}</bold></green> <aqua><bold>сек.</bold></aqua>.</bold></aqua>";
    public String TOTEM_DISABLED_IN_PVP = "<aqua><bold>Тотем небыл использован, т.к он отключен в <yellow><bold>PVP</bold></yellow><aqua><bold> режиме</bold></aqua>";
    public String PVP_STARTED_WITH_POWERUPS = "<red><bold>Вы начали пвп с включеным GM/FLY/и тд и за это получили негативный эффект</bold></red>";
  }
}