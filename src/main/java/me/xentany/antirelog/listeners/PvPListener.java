package me.xentany.antirelog.listeners;

import me.xentany.antirelog.Settings;
import me.xentany.antirelog.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import me.xentany.antirelog.manager.PvPManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class PvPListener implements Listener {

  private final static String META_KEY = "ar-f-shooter";

  private final Plugin plugin;
  private final PvPManager pvpManager;
  private final Map<Player, AtomicInteger> allowedTeleports = new HashMap<>();


  public PvPListener(Plugin plugin, PvPManager pvpManager) {
    this.plugin = plugin;
    this.pvpManager = pvpManager;
    plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
      allowedTeleports.values().forEach(ai -> ai.set(ai.get() + 1));
      allowedTeleports.values().removeIf(ai -> ai.get() >= 5);
    }, 1l, 1l);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onDamageByEntity(EntityDamageByEntityEvent event) {
    if (event.getEntity().getType() != EntityType.PLAYER) {
      return;
    }
    Player target = (Player) event.getEntity();
    Player damager = getDamager(event.getDamager());
    pvpManager.playerDamagedByPlayer(damager, target);
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onInteractWithEntity(PlayerInteractEntityEvent event) {
    if (Settings.IMP.CANCEL_INTERACT_WITH_ENTITIES && pvpManager.isPvPModeEnabled() && pvpManager.isInPvP(event.getPlayer())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onCombust(EntityCombustByEntityEvent event) {
    if (!(event.getEntity() instanceof Player))
      return;
    Player target = (Player) event.getEntity();
    Player damager = getDamager(event.getCombuster());
    pvpManager.playerDamagedByPlayer(damager, target);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onShootBow(EntityShootBowEvent event) {
    if (event.getProjectile() instanceof Firework && event.getEntity().getType() == EntityType.PLAYER) {
      event.getProjectile().setMetadata(META_KEY, new FixedMetadataValue(plugin, event.getEntity().getUniqueId()));
    }
  }


  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onPotionSplash(PotionSplashEvent e) {
    if (e.getPotion() != null && e.getPotion().getShooter() instanceof Player) {
      Player shooter = (Player) e.getPotion().getShooter();
      for (LivingEntity en : e.getAffectedEntities()) {
        if (en.getType() == EntityType.PLAYER && en != shooter) {
          for (PotionEffect ef : e.getPotion().getEffects()) {
            if (ef.getType().equals(PotionEffectType.POISON)) {
              pvpManager.playerDamagedByPlayer(shooter, (Player) en);
            }
          }
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onTeleport(PlayerTeleportEvent ev) {

    if (Settings.IMP.DISABLE_TELEPORTS_IN_PVP && pvpManager.isInPvP(ev.getPlayer())) {
      if (allowedTeleports.containsKey(ev.getPlayer())) { //allow all teleport in 4-5 ticks after chorus or ender pearl
        return;
      }

      if ((ev.getCause() == TeleportCause.CHORUS_FRUIT) || ev.getCause() == TeleportCause.ENDER_PEARL) {
        allowedTeleports.put(ev.getPlayer(), new AtomicInteger(0));
        return;
      }
      if (ev.getFrom().getWorld() != ev.getTo().getWorld()) {
        ev.setCancelled(true);
        return;
      }
      if (ev.getFrom().distanceSquared(ev.getTo()) > 100) { //10 = 10 blocks
        ev.setCancelled(true);
      }
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onCommand(PlayerCommandPreprocessEvent e) {
    if (Settings.IMP.DISABLE_COMMANDS_IN_PVP && pvpManager.isInPvP(e.getPlayer())) {
      var command = e.getMessage().split(" ")[0].replaceFirst("/", "");

      if (pvpManager.isCommandWhiteListed(command)) {
        return;
      }

      e.setCancelled(true);

      var message = Settings.IMP.MESSAGES.COMMANDS_DISABLED;

      MessageUtil.sendIfNotEmpty(e.getPlayer(), message, pvpManager.getTimeRemainingInPvP(e.getPlayer()));
    }
  }


  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onKick(PlayerKickEvent e) {
    Player player = e.getPlayer();

    if (pvpManager.isInSilentPvP(player)) {
      pvpManager.stopPvPSilent(player);
      return;
    }

    if (!pvpManager.isInPvP(player)) {
      return;
    }

    pvpManager.stopPvPSilent(player);

    if (Settings.IMP.KICK_MESSAGES.isEmpty()) {
      kickedInPvp(player);
      return;
    }
    if (e.getReason() == null) {
      return;
    }
    String reason = ChatColor.stripColor(e.getReason().toLowerCase());
    for (String killReason : Settings.IMP.KICK_MESSAGES) {
      if (reason.contains(killReason.toLowerCase())) {
        kickedInPvp(player);
        return;
      }
    }
  }

  private void kickedInPvp(Player player) {
    if (Settings.IMP.KILL_ON_KICK) {
      player.setHealth(0);
      sendLeavedInPvpMessage(player);
    }
    if (Settings.IMP.RUN_COMMANDS_ON_KICK) {
      runCommands(player);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onQuit(PlayerQuitEvent e) {
    allowedTeleports.remove(e.getPlayer());
    if (Settings.IMP.HIDE_LEAVE_MESSAGE) {
      e.setQuitMessage(null);
    }
    if (pvpManager.isInPvP(e.getPlayer())) {
      pvpManager.stopPvPSilent(e.getPlayer());
      if (Settings.IMP.KILL_ON_LEAVE) {
        sendLeavedInPvpMessage(e.getPlayer());
        e.getPlayer().setHealth(0);
      } else {
        pvpManager.stopPvPSilent(e.getPlayer());
      }
      runCommands(e.getPlayer());
    }
    if (pvpManager.isInSilentPvP(e.getPlayer())) {
      pvpManager.stopPvPSilent(e.getPlayer());
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onDeath(PlayerDeathEvent e) {
    if (Settings.IMP.HIDE_DEATH_MESSAGE) {
      e.setDeathMessage(null);
    }

    if (pvpManager.isInSilentPvP(e.getEntity()) || pvpManager.isInPvP(e.getEntity())) {
      pvpManager.stopPvPSilent(e.getEntity());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onJoin(PlayerJoinEvent e) {
    if (Settings.IMP.HIDE_JOIN_MESSAGE) {
      e.setJoinMessage(null);
    }
  }

  private void sendLeavedInPvpMessage(Player p) {
    var message = Settings.IMP.MESSAGES.PVP_LEAVED;

    if (!message.isEmpty()) {
      var component = MessageUtil.deserialize(message, p.getName());

      Bukkit.getOnlinePlayers().forEach(pl -> pl.sendMessage(component));
    }
  }

  private void runCommands(Player leaved) {
    if (!Settings.IMP.COMMANDS_ON_LEAVE.isEmpty()) {
      Settings.IMP.COMMANDS_ON_LEAVE.forEach(command ->
          Bukkit.dispatchCommand(Bukkit.getConsoleSender(), MessageUtil.format(command, leaved.getName())));
    }
  }

  private @Nullable Player getDamager(final @NotNull Entity damager) {
    return switch (damager) {
      case Player player -> player;
      case Projectile projectile -> extractPlayer(projectile.getShooter());
      case TNTPrimed tntPrimed -> extractPlayer(tntPrimed.getSource());
      case AreaEffectCloud areaEffectCloud -> extractPlayer(areaEffectCloud.getSource());
      default -> null;
    };
  }

  private @Nullable Player extractPlayer(final @Nullable Object source) {
    return source instanceof final Player player ? player : null;
  }
}