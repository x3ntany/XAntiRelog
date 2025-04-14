package me.xentany.antirelog.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import me.xentany.antirelog.manager.CooldownManager;
import me.xentany.antirelog.manager.CooldownManager.CooldownType;
import me.xentany.antirelog.manager.PvPManager;

import java.util.Arrays;
import java.util.List;

public class ProtocolLibUtils {

  private static Class<?> ITEM_CLASS;
  private static MethodAccessor getItem = null;
  private static MethodAccessor getMaterial = null;

  static {
    boolean hasProtocolLib = Bukkit.getPluginManager().isPluginEnabled("ProtocolLib");
    if (hasProtocolLib) {
      try {
        ITEM_CLASS = MinecraftReflection.getMinecraftClass("Item");
      } catch (final Exception exception) {
        ITEM_CLASS = MinecraftReflection.getMinecraftClass("world.item.Item");
      }
      getItem = Accessors.getMethodAccessor(MinecraftReflection
              .getCraftBukkitClass("util.CraftMagicNumbers"),
          "getItem", Material.class);
      getMaterial = Accessors.getMethodAccessor(MinecraftReflection
              .getCraftBukkitClass("util.CraftMagicNumbers"),
          "getMaterial", ITEM_CLASS);
    }

  }

  public static PacketContainer createCooldownPacket(Material material, int ticks) {
    PacketContainer packetContainer = new PacketContainer(PacketType.Play.Server.SET_COOLDOWN);
    packetContainer.getModifier().writeDefaults();
    packetContainer.getModifier().withType(ITEM_CLASS).write(0, getItem.invoke(null, material));
    packetContainer.getIntegers().write(0, ticks);
    return packetContainer;

  }

  public static void sendPacket(PacketContainer packetContainer, Player player) {
    ProtocolLibrary.getProtocolManager().sendServerPacket(player, packetContainer);
  }

  public static void createListener(CooldownManager cooldownManager, PvPManager pvPManager, Plugin plugin) {

    ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, ListenerPriority.LOWEST, PacketType.Play.Server.SET_COOLDOWN) {
      List<CooldownType> types = Arrays.asList(CooldownType.CHORUS, CooldownType.ENDER_PEARL);

      @Override
      public void onPacketSending(PacketEvent event) {
        Material material = (Material) getMaterial.invoke(null, event.getPacket().getModifier().withType(ITEM_CLASS).read(0));
        int duration = event.getPacket().getIntegers().read(0);
        duration = duration * 50;

        for (CooldownType cooldownType : types) {
          if (material == cooldownType.getMaterial()) {
            boolean hasCooldown = cooldownManager.hasCooldown(event.getPlayer(), cooldownType, cooldownType.getCooldown() * 1000L);
            if (hasCooldown) {
              long remaning = cooldownManager.getRemaining(event.getPlayer(), cooldownType, cooldownType.getCooldown() * 1000L);
              if (Math.abs(remaning - duration) > 100) {
                if (!pvPManager.isPvPModeEnabled() || pvPManager.isInPvP(event.getPlayer())) {
                  if (duration == 0) {
                    event.setCancelled(true);
                    return;
                  }
                  event.getPacket().getIntegers().write(0, (int) Math.ceil(remaning / 50f));
                }
              }
            }
          }
        }
      }
    });
  }
}
