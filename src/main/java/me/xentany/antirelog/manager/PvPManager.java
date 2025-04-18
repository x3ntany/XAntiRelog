package me.xentany.antirelog.manager;

import me.xentany.antirelog.Settings;
import me.xentany.antirelog.util.MessageUtil;
import net.kyori.adventure.title.Title;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.codemc.worldguardwrapper.WorldGuardWrapper;
import org.codemc.worldguardwrapper.region.IWrappedRegion;
import me.xentany.antirelog.AntiRelogPlugin;
import me.xentany.antirelog.event.PvpPreStartEvent;
import me.xentany.antirelog.event.PvpPreStartEvent.PvPStatus;
import me.xentany.antirelog.event.PvpStartedEvent;
import me.xentany.antirelog.event.PvpStoppedEvent;
import me.xentany.antirelog.event.PvpTimeUpdateEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PvPManager {

  private final AntiRelogPlugin plugin;
  private final Map<Player, Integer> pvpMap = new HashMap<>();
  private final Map<Player, Integer> silentPvpMap = new HashMap<>();
  private final PowerUpsManager powerUpsManager;
  private final BossbarManager bossbarManager;
  private final Set<String> whiteListedCommands = new HashSet<>();

  public PvPManager(AntiRelogPlugin plugin) {
    this.plugin = plugin;
    this.powerUpsManager = new PowerUpsManager();
    this.bossbarManager = new BossbarManager();
    onPluginEnable();
  }

  public void onPluginDisable() {
    pvpMap.clear();
    this.bossbarManager.clearBossbars();
  }

  public void onPluginEnable() {
    whiteListedCommands.clear();
    if (Settings.IMP.DISABLE_COMMANDS_IN_PVP && !Settings.IMP.COMMANDS_WHITELIST.isEmpty()) {
      Settings.IMP.COMMANDS_WHITELIST.forEach(wcommand -> {
        Command command = Bukkit.getCommandMap().getCommand(wcommand);
        whiteListedCommands.add(wcommand.toLowerCase());
        if (command != null) {
          whiteListedCommands.add(command.getName().toLowerCase());
          command.getAliases().forEach(alias -> whiteListedCommands.add(alias.toLowerCase()));
        }
      });
    }
    plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
      if (pvpMap.isEmpty() && silentPvpMap.isEmpty()) {
        return;
      }
      iterateMap(pvpMap, false);
      iterateMap(silentPvpMap, true);

    }, 20, 20);
    this.bossbarManager.createBossBars();
  }

  private void iterateMap(Map<Player, Integer> map, boolean bypassed) {
    if (!map.isEmpty()) {
      List<Player> playersInPvp = new ArrayList<>(map.keySet());
      for (Player player : playersInPvp) {
        int currentTime = bypassed ? getTimeRemainingInPvPSilent(player) : getTimeRemainingInPvP(player);
        int timeRemaining = currentTime - 1;
        if (timeRemaining <= 0 || (Settings.IMP.DISABLE_PVP_IN_IGNORED_REGION && isInIgnoredRegion(player))) {
          if (bypassed) {
            stopPvPSilent(player);
          } else {
            stopPvP(player);
          }
        } else {
          updatePvpMode(player, bypassed, timeRemaining);
          callUpdateEvent(player, currentTime, timeRemaining);
        }
      }
    }
  }

  public boolean isInPvP(Player player) {
    return pvpMap.containsKey(player);
  }

  public boolean isInSilentPvP(Player player) {
    return silentPvpMap.containsKey(player);
  }

  public int getTimeRemainingInPvP(Player player) {
    return pvpMap.getOrDefault(player, 0);
  }

  public int getTimeRemainingInPvPSilent(Player player) {
    return silentPvpMap.getOrDefault(player, 0);
  }

  public void playerDamagedByPlayer(Player attacker, Player defender) {
    if (defender != attacker && attacker != null && defender != null && (attacker.getWorld() == defender.getWorld())) {
      if (defender.getGameMode() == GameMode.CREATIVE) { //i dont have time to determite, why some events is called when defender in creative
        return;
      }

      if (attacker.hasMetadata("NPC") || defender.hasMetadata("NPC")) {
        return;
      }

      if (defender.isDead() || attacker.isDead()) {
        return;
      }
      tryStartPvP(attacker, defender);
    }
  }

  private void tryStartPvP(Player attacker, Player defender) {
    if (isInIgnoredWorld(attacker)) {
      return;
    }

    if (isInIgnoredRegion(attacker) || isInIgnoredRegion(defender)) {
      return;
    }

    if (!isPvPModeEnabled() && Settings.IMP.DISABLE_POWERUPS) {
      if (!isHasBypassPermission(attacker)) {
        powerUpsManager.disablePowerUpsWithRunCommands(attacker);
      }
      if (!isHasBypassPermission(defender)) {
        powerUpsManager.disablePowerUps(defender);
      }
      return;
    }

    if (!isPvPModeEnabled()) {
      return;
    }

    boolean attackerBypassed = isHasBypassPermission(attacker);
    boolean defenderBypassed = isHasBypassPermission(defender);

    if (attackerBypassed && defenderBypassed) {
      return;
    }

    boolean attackerInPvp = isInPvP(attacker) || isInSilentPvP(attacker);
    boolean defenderInPvp = isInPvP(defender) || isInSilentPvP(defender);
    PvPStatus pvpStatus = PvPStatus.ALL_NOT_IN_PVP;
    if (attackerInPvp && defenderInPvp) {
      updateAttackerAndCallEvent(attacker, defender, attackerBypassed);
      updateDefenderAndCallEvent(defender, attacker, defenderBypassed);
      return;
    } else if (attackerInPvp) {
      pvpStatus = PvPStatus.ATTACKER_IN_PVP;
    } else if (defenderInPvp) {
      pvpStatus = PvPStatus.DEFENDER_IN_PVP;
    }
    if (pvpStatus == PvPStatus.ATTACKER_IN_PVP || pvpStatus == PvPStatus.DEFENDER_IN_PVP) {
      if (callPvpPreStartEvent(defender, attacker, pvpStatus)) {
        if (attackerInPvp) {
          updateAttackerAndCallEvent(attacker, defender, attackerBypassed);
          startPvp(defender, defenderBypassed, false);
        } else {
          updateDefenderAndCallEvent(defender, attacker, defenderBypassed);
          startPvp(attacker, attackerBypassed, true);
        }
        Bukkit.getPluginManager().callEvent(new PvpStartedEvent(defender, attacker, Settings.IMP.PVP_TIME, pvpStatus));
      }
      return;
    }

    if (callPvpPreStartEvent(defender, attacker, pvpStatus)) {
      startPvp(attacker, attackerBypassed, true);
      startPvp(defender, defenderBypassed, false);
      Bukkit.getPluginManager().callEvent(new PvpStartedEvent(defender, attacker, Settings.IMP.PVP_TIME, pvpStatus));
    }

  }

  public void startPvp(Player player, boolean bypassed, boolean attacker) {
    if (!bypassed) {
      MessageUtil.sendIfNotEmpty(player, Settings.IMP.MESSAGES.PVP_STARTED);

      if (attacker && Settings.IMP.DISABLE_POWERUPS) {
        powerUpsManager.disablePowerUpsWithRunCommands(player);
      }
      sendTitles(player, true);
    }
    updatePvpMode(player, bypassed, Settings.IMP.PVP_TIME);
    player.setNoDamageTicks(0);
  }

  private void updatePvpMode(Player player, boolean bypassed, int newTime) {
    if (bypassed) {
      silentPvpMap.put(player, newTime);
    } else {
      pvpMap.put(player, newTime);
      bossbarManager.setBossBar(player, newTime);

      var actionBar = Settings.IMP.MESSAGES.IN_PVP_ACTIONBAR;

      if (!actionBar.isEmpty()) {
        var component = MessageUtil.deserialize(actionBar, newTime);

        player.sendActionBar(component);
      }
      if (Settings.IMP.DISABLE_POWERUPS) {
        powerUpsManager.disablePowerUps(player);
      }
      //player.setNoDamageTicks(0);
    }
  }

  private boolean callPvpPreStartEvent(Player defender, Player attacker, PvPStatus pvpStatus) {
    PvpPreStartEvent pvpPreStartEvent = new PvpPreStartEvent(defender, attacker, Settings.IMP.PVP_TIME, pvpStatus);
    Bukkit.getPluginManager().callEvent(pvpPreStartEvent);
    if (pvpPreStartEvent.isCancelled()) {
      return false;
    }
    return true;
  }

  private void updateAttackerAndCallEvent(Player attacker, Player defender, boolean bypassed) {
    int oldTime = bypassed ? getTimeRemainingInPvPSilent(attacker) : getTimeRemainingInPvP(attacker);
    updatePvpMode(attacker, bypassed, Settings.IMP.PVP_TIME);
    PvpTimeUpdateEvent pvpTimeUpdateEvent = new PvpTimeUpdateEvent(attacker, oldTime, Settings.IMP.PVP_TIME);
    pvpTimeUpdateEvent.setDamagedPlayer(defender);
    Bukkit.getPluginManager().callEvent(pvpTimeUpdateEvent);
  }

  private void updateDefenderAndCallEvent(Player defender, Player attackedBy, boolean bypassed) {
    int oldTime = bypassed ? getTimeRemainingInPvPSilent(defender) : getTimeRemainingInPvP(defender);
    updatePvpMode(defender, bypassed, Settings.IMP.PVP_TIME);
    PvpTimeUpdateEvent pvpTimeUpdateEvent = new PvpTimeUpdateEvent(defender, oldTime, Settings.IMP.PVP_TIME);
    pvpTimeUpdateEvent.setDamagedBy(attackedBy);
    Bukkit.getPluginManager().callEvent(pvpTimeUpdateEvent);
  }

  private void callUpdateEvent(Player player, int oldTime, int newTime) {
    PvpTimeUpdateEvent pvpTimeUpdateEvent = new PvpTimeUpdateEvent(player, oldTime, newTime);
    Bukkit.getPluginManager().callEvent(pvpTimeUpdateEvent);
  }

  public void stopPvP(Player player) {
    stopPvPSilent(player);
    sendTitles(player, false);
    MessageUtil.sendIfNotEmpty(player, Settings.IMP.MESSAGES.PVP_STOPPED);

    var actionBar = Settings.IMP.MESSAGES.PVP_STOPPED_ACTIONBAR;

    if (!actionBar.isEmpty()) {
      var component = MessageUtil.deserialize(actionBar);

      player.sendActionBar(component);
    }
  }

  public void stopPvPSilent(Player player) {
    pvpMap.remove(player);
    bossbarManager.clearBossbar(player);
    silentPvpMap.remove(player);
    Bukkit.getPluginManager().callEvent(new PvpStoppedEvent(player));
  }

  public boolean isCommandWhiteListed(String command) {
    if (whiteListedCommands.isEmpty()) {
      return false; //all commands are blocked
    }
    return whiteListedCommands.contains(command.toLowerCase());
  }

  public PowerUpsManager getPowerUpsManager() {
    return powerUpsManager;
  }

  public BossbarManager getBossbarManager() {
    return bossbarManager;
  }

  private void sendTitles(Player player, boolean isPvpStarted) {
    var stringTitle = isPvpStarted ?
        Settings.IMP.MESSAGES.PVP_STARTED_TITLE :
        Settings.IMP.MESSAGES.PVP_STOPPED_TITLE;
    var stringSubtitle = isPvpStarted ?
        Settings.IMP.MESSAGES.PVP_STARTED_SUBTITLE :
        Settings.IMP.MESSAGES.PVP_STOPPED_SUBTITLE;
    var componentTitle = MessageUtil.deserialize(stringTitle);
    var componentSubtitle = MessageUtil.deserialize(stringSubtitle);
    var title = Title.title(componentTitle, componentSubtitle);
    player.showTitle(title);
  }

  public boolean isPvPModeEnabled() {
    return Settings.IMP.PVP_TIME > 0;
  }

  public boolean isBypassed(Player player) {
    return isHasBypassPermission(player) || isInIgnoredWorld(player);
  }

  public boolean isHasBypassPermission(Player player) {
    return player.hasPermission("antirelog.bypass");
  }

  public boolean isInIgnoredWorld(Player player) {
    return Settings.IMP.DISABLED_WORLDS.contains(player.getWorld().getName().toLowerCase());
  }

  public boolean isInIgnoredRegion(Player player) {
    if (!plugin.isWorldguardEnabled() || Settings.IMP.IGNORED_WORLDGUARD_REGIONS.isEmpty()) {
      return false;
    }

    Set<IWrappedRegion> wrappedRegions = WorldGuardWrapper.getInstance().getRegions(player.getLocation());
    if (wrappedRegions.isEmpty()) {
      return false;
    }
    for (IWrappedRegion region : wrappedRegions) {
      if (Settings.IMP.IGNORED_WORLDGUARD_REGIONS.contains(region.getId().toLowerCase())) {
        return true;
      }
    }

    return false;
  }
}