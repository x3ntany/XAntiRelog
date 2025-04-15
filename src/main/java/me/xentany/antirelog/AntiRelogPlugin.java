package me.xentany.antirelog;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.codemc.worldguardwrapper.WorldGuardWrapper;
import me.xentany.antirelog.listeners.CooldownListener;
import me.xentany.antirelog.listeners.EssentialsTeleportListener;
import me.xentany.antirelog.listeners.PvPListener;
import me.xentany.antirelog.listeners.WorldGuardListener;
import me.xentany.antirelog.manager.BossbarManager;
import me.xentany.antirelog.manager.CooldownManager;
import me.xentany.antirelog.manager.PowerUpsManager;
import me.xentany.antirelog.manager.PvPManager;

public class AntiRelogPlugin extends JavaPlugin {

  private PvPManager pvpManager;
  private CooldownManager cooldownManager;
  private boolean worldguard;

  @Override
  public void onEnable() {
    Settings.IMP.reload(this.getDataFolder().toPath().resolve("config.yml").toFile());
    pvpManager = new PvPManager(this);
    detectPlugins();
    cooldownManager = new CooldownManager(this);

    getServer().getPluginManager().registerEvents(new PvPListener(this, pvpManager), this);
    getServer().getPluginManager().registerEvents(new CooldownListener(this, cooldownManager, pvpManager), this);
  }

  @Override
  public void onDisable() {
    pvpManager.onPluginDisable();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    pvpManager.startPvp((Player) sender, false,false);
    if (args.length > 0 && args[0].equalsIgnoreCase("reload") && sender.hasPermission("antirelog.reload")) {
      reloadSettings();
      sender.sendMessage("§aReloaded");
      getLogger().info(Settings.IMP.toString());
    }
    return true;
  }

  public void reloadSettings() {
    getServer().getScheduler().cancelTasks(this);
    pvpManager.onPluginDisable();
    pvpManager.onPluginEnable();
    cooldownManager.clearAll();
  }

  public boolean isWorldguardEnabled() {
    return worldguard;
  }

  private void detectPlugins() {
    if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
      WorldGuardWrapper.getInstance().registerEvents(this);
      Bukkit.getPluginManager().registerEvents(new WorldGuardListener(pvpManager), this);
      worldguard = true;
    }
    try {
      Class.forName("net.ess3.api.events.teleport.PreTeleportEvent");
      Bukkit.getPluginManager().registerEvents(new EssentialsTeleportListener(pvpManager), this);
    } catch (ClassNotFoundException ignored) {
    }
  }

  public PvPManager getPvpManager() {
    return pvpManager;
  }

  public PowerUpsManager getPowerUpsManager() {
    return pvpManager.getPowerUpsManager();
  }

  public BossbarManager getBossbarManager() {
    return pvpManager.getBossbarManager();
  }

  public CooldownManager getCooldownManager() {
    return cooldownManager;
  }
}
