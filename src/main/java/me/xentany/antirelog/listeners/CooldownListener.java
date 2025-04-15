package me.xentany.antirelog.listeners;

import me.xentany.antirelog.Settings;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import me.xentany.antirelog.event.PvpStartedEvent;
import me.xentany.antirelog.event.PvpStoppedEvent;
import me.xentany.antirelog.manager.CooldownManager;
import me.xentany.antirelog.manager.CooldownManager.CooldownType;
import me.xentany.antirelog.manager.PvPManager;
import me.xentany.antirelog.util.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class CooldownListener implements Listener {

  private final CooldownManager cooldownManager;
  private final PvPManager pvpManager;

  public CooldownListener(Plugin plugin, CooldownManager cooldownManager, PvPManager pvpManager) {
    this.cooldownManager = cooldownManager;
    this.pvpManager = pvpManager;
  }

  @EventHandler
  public void onEntityResurrect(final @NotNull EntityResurrectEvent event) {
    if (!(event.getEntity() instanceof final Player player)) {
      return;
    }

    if (!this.pvpManager.isInPvP(player)) {
      return;
    }

    long cooldownTime = Settings.IMP.TOTEM_COOLDOWN;
    if (cooldownTime == 0 || pvpManager.isBypassed(player)) {
      return;
    }
    if (cooldownTime <= -1) {
      cancelEventIfInPvp(event, CooldownType.TOTEM, player);
      return;
    }
    cooldownTime = cooldownTime * 1000;
    if (checkCooldown(player, CooldownType.TOTEM, cooldownTime)) {
      event.setCancelled(true);
      return;
    }
    cooldownManager.addCooldown(player, CooldownType.TOTEM);
    addItemCooldown(player, CooldownType.TOTEM);
  }

  @EventHandler
  public void onItemEat(@NotNull PlayerItemConsumeEvent event) {
    var player = event.getPlayer();

    if (!this.pvpManager.isInPvP(player)) {
      return;
    }

    ItemStack consumeItem = event.getItem();

    CooldownType cooldownType = null;

    long cooldownTime = 0;

    if (isChorus(consumeItem)) {
      cooldownType = CooldownType.CHORUS;
      cooldownTime = Settings.IMP.CHORUS_COOLDOWN;
    }
    if (isGoldenOrEnchantedApple(consumeItem)) {
      boolean isEnchantedGoldenApple = isEnchantedGoldenApple(consumeItem);

      cooldownType = isEnchantedGoldenApple ? CooldownType.ENC_GOLDEN_APPLE : CooldownType.GOLDEN_APPLE;
      cooldownTime = isEnchantedGoldenApple ? Settings.IMP.ENCHANTED_GOLDEN_APPLE_COOLDOWN : Settings.IMP.GOLDEN_APPLE_COOLDOWN;
    }

    if (cooldownType != null) {
      if (cooldownTime == 0 || pvpManager.isBypassed(player)) {
        return;
      }
      if (cooldownTime <= -1) {
        cancelEventIfInPvp(event, cooldownType, player);
        return;
      }
      cooldownTime = cooldownTime * 1000;
      if (checkCooldown(player, cooldownType, cooldownTime)) {
        event.setCancelled(true);
        return;
      }
      cooldownManager.addCooldown(player, cooldownType);
      addItemCooldown(player, cooldownType);
    }

  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onPerlLaunch(ProjectileLaunchEvent event) {
    if (!(event.getEntity().getShooter() instanceof final Player player)) {
      return;
    }

    if (!this.pvpManager.isInPvP(player)) {
      return;
    }

    if (Settings.IMP.ENDER_PEARL_COOLDOWN <= 0 || event.getEntityType() != EntityType.ENDER_PEARL) {
      return;
    }

    if (!pvpManager.isBypassed(player)) {
      cooldownManager.addCooldown(player, CooldownType.ENDER_PEARL);
      addItemCooldown(player, CooldownType.ENDER_PEARL);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onInteract(PlayerInteractEvent event) {
    var player = event.getPlayer();

    if (!this.pvpManager.isInPvP(player)) {
      return;
    }

    if (Settings.IMP.ENDER_PEARL_COOLDOWN == 0 && Settings.IMP.FIREWORK_COOLDOWN == 0) return;
    if (!event.hasItem()) return;
    if (pvpManager.isBypassed(player)) return;

    if (Settings.IMP.ENDER_PEARL_COOLDOWN != 0 && event.getItem().getType() == Material.ENDER_PEARL) {
      if (Settings.IMP.ENDER_PEARL_COOLDOWN <= -1) {
        cancelEventIfInPvp(event, CooldownType.ENDER_PEARL, player);
        return;
      }
      if (checkCooldown(player, CooldownType.ENDER_PEARL,
          Settings.IMP.ENDER_PEARL_COOLDOWN * 1000L)) {
        event.setCancelled(true);
      }
    } else if (Settings.IMP.FIREWORK_COOLDOWN != 0 && isFirework(event.getItem())) {
      if (Settings.IMP.FIREWORK_COOLDOWN <= -1) {
        cancelEventIfInPvp(event, CooldownType.FIREWORK, player);
        return;
      }

      if (checkCooldown(player, CooldownType.FIREWORK, Settings.IMP.FIREWORK_COOLDOWN * 1000L)) {
        event.setCancelled(true);
        return;
      }
      cooldownManager.addCooldown(player, CooldownType.FIREWORK);
      addItemCooldown(player, CooldownType.FIREWORK);
    }

  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    cooldownManager.remove(event.getPlayer());
  }

  @EventHandler
  public void onPvpStart(PvpStartedEvent event) {
    switch (event.getPvpStatus()) {
      case ALL_NOT_IN_PVP:
        cooldownManager.enteredToPvp(event.getDefender());
        cooldownManager.enteredToPvp(event.getAttacker());
        break;
      case ATTACKER_IN_PVP:
        cooldownManager.enteredToPvp(event.getDefender());
        break;
      case DEFENDER_IN_PVP:
        cooldownManager.enteredToPvp(event.getAttacker());
        break;
    }
  }

  @EventHandler
  public void onPvpStop(PvpStoppedEvent event) {
    cooldownManager.removedFromPvp(event.getPlayer());
  }

  private boolean isChorus(ItemStack itemStack) {
    return itemStack.getType() == Material.CHORUS_FRUIT;
  }

  private boolean isGoldenOrEnchantedApple(ItemStack itemStack) {
    return isGoldenApple(itemStack) || isEnchantedGoldenApple(itemStack);
  }

  private boolean isGoldenApple(ItemStack itemStack) {
    return itemStack.getType() == Material.GOLDEN_APPLE;
  }

  private boolean isEnchantedGoldenApple(ItemStack itemStack) {
    return itemStack.getType() == Material.ENCHANTED_GOLDEN_APPLE;
  }

  private boolean isFirework(ItemStack itemStack) {
    return itemStack.getType() == Material.FIREWORK_ROCKET;
  }

  private void cancelEventIfInPvp(Cancellable event, CooldownType type, Player player) {
    if (pvpManager.isInPvP(player)) {
      ((Cancellable) event).setCancelled(true);
      String message = type == CooldownType.TOTEM ? Settings.IMP.MESSAGES.TOTEM_DISABLED_IN_PVP :
          Settings.IMP.MESSAGES.ITEM_DISABLED_IN_PVP;
      if (!message.isEmpty()) {
        player.sendMessage(Utils.color(message));
      }
    }
    return;
  }

  private boolean checkCooldown(Player player, CooldownType cooldownType, long cooldownTime) {
    boolean cooldownActive = !pvpManager.isPvPModeEnabled() || pvpManager.isInPvP(player);
    if (cooldownActive && cooldownManager.hasCooldown(player, cooldownType, cooldownTime)) {
      long remaining = cooldownManager.getRemaining(player, cooldownType, cooldownTime);
      int remainingInt = (int) TimeUnit.MILLISECONDS.toSeconds(remaining);
      String message = cooldownType == CooldownType.TOTEM ? Settings.IMP.MESSAGES.TOTEM_COOLDOWN :
          Settings.IMP.MESSAGES.ITEM_COOLDOWN;
      if (!message.isEmpty()) {
        player.sendMessage(Utils.color(Utils.replaceTime(message.replace("%time%",
            Math.round(remaining / 1000) + ""), remainingInt)));
      }
      return true;
    }
    return false;
  }

  private void addItemCooldown(Player player, CooldownType cooldownType) {
    cooldownManager.addItemCooldown(player, cooldownType, cooldownType.getCooldown() * 1000L);
  }
}
