package me.xentany.antirelog.manager;

import com.comphenix.protocol.events.PacketContainer;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import me.xentany.antirelog.Settings;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import me.xentany.antirelog.AntiRelogPlugin;
import me.xentany.antirelog.util.ProtocolLibUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CooldownManager {

  private final AntiRelogPlugin plugin;
  private final ScheduledExecutorService scheduledExecutorService;
  private final Table<Player, CooldownType, Long> cooldowns = HashBasedTable.create();
  private final Table<Player, CooldownType, ScheduledFuture> futures = HashBasedTable.create();

  public CooldownManager(AntiRelogPlugin plugin) {
    this.plugin = plugin;
    if (plugin.isProtocolLibEnabled()) {
      scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    } else {
      scheduledExecutorService = null;
    }
  }

  public void addCooldown(Player player, CooldownType type) {
    cooldowns.put(player, type, System.currentTimeMillis());
  }

  public void addItemCooldown(Player player, CooldownType type, long duration) {
    if (!plugin.isProtocolLibEnabled()) {
      return;
    }
    int durationInTicks = (int) Math.ceil(duration / 50.0);

    PacketContainer packetContainer = ProtocolLibUtils.createCooldownPacket(type.getMaterial(), durationInTicks);

    ProtocolLibUtils.sendPacket(packetContainer, player);


    ScheduledFuture future = scheduledExecutorService.schedule(() -> {
      removeItemCooldown(player, type);
      //futures.remove(player, type);
    }, duration, TimeUnit.MILLISECONDS);
    futures.put(player, type, future);
  }

  public void removeItemCooldown(Player player, CooldownType type) {
    if (!plugin.isProtocolLibEnabled()) {
      return;
    }
    ScheduledFuture future = futures.get(player, type);
    if (future != null && !future.isCancelled()) {
      future.cancel(false);
      futures.remove(player, type);
    }
    PacketContainer packetContainer1 = ProtocolLibUtils.createCooldownPacket(type.getMaterial(), 0);
    ProtocolLibUtils.sendPacket(packetContainer1, player);
  }

  public void enteredToPvp(Player player) {
    for (CooldownType cooldownType : CooldownType.values()) {
      int cooldown = cooldownType.getCooldown();
      if (cooldown == 0) {
        continue;
      }
      if (cooldown > 0 && hasCooldown(player, cooldownType, cooldown * 1000)) {
        addItemCooldown(player, cooldownType, getRemaining(player, cooldownType, cooldown * 1000));
      }
      if (cooldown < 0) {
        addItemCooldown(player, cooldownType, 300 * 1000);
      }
    }
  }

  public void removedFromPvp(Player player) {
    for (CooldownType cooldownType : CooldownType.values()) {
      int cooldown = cooldownType.getCooldown();
      if (cooldown > 0 && hasCooldown(player, cooldownType, cooldown * 1000)) {
        removeItemCooldown(player, cooldownType);
      }
    }
  }

  public boolean hasCooldown(Player player, CooldownType type, long duration) {
    Long added = cooldowns.get(player, type);
    if (added == null) {
      return false;
    }
    return (System.currentTimeMillis() - added) < duration;
  }

  public long getRemaining(Player player, CooldownType type, long duration) {
    Long added = cooldowns.get(player, type);
    return duration - (System.currentTimeMillis() - added);
  }

  public void remove(Player player) {
    cooldowns.row(player).clear();
    futures.row(player).forEach((ignore, future) -> future.cancel(false));
    futures.row(player).clear();
  }

  public void clearAll() {
    futures.rowMap().forEach((p, map) -> map.forEach((i, f) -> {
      f.cancel(true);
      removeItemCooldown(p, i);
    }));
    futures.clear();
    cooldowns.clear();
  }

  public enum CooldownType {
    GOLDEN_APPLE(Material.GOLDEN_APPLE, Settings.IMP.GOLDEN_APPLE_COOLDOWN),
    ENC_GOLDEN_APPLE(Material.ENCHANTED_GOLDEN_APPLE, Settings.IMP.ENCHANTED_GOLDEN_APPLE_COOLDOWN),
    ENDER_PEARL(Material.ENDER_PEARL, Settings.IMP.ENDER_PEARL_COOLDOWN),
    CHORUS(Material.CHORUS_FRUIT, Settings.IMP.CHORUS_COOLDOWN),
    TOTEM(Material.TOTEM_OF_UNDYING, Settings.IMP.TOTEM_COOLDOWN),
    FIREWORK(Material.FIREWORK_ROCKET, Settings.IMP.FIREWORK_COOLDOWN);

    final Material material;
    final Integer cooldown;

    CooldownType(Material material, Integer cooldown) {
      this.material = material;
      this.cooldown = cooldown;
    }

    public int getCooldown() {
      return cooldown;
    }

    public Material getMaterial() {
      return material;
    }
  }
}
