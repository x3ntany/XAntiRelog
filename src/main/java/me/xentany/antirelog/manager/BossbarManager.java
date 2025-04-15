package me.xentany.antirelog.manager;

import me.xentany.antirelog.Settings;
import me.xentany.antirelog.util.MessageUtil;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class BossbarManager {

  private final Map<Integer, BossBar> bossBars = new HashMap<>();

  public void createBossBars() {
    bossBars.clear();
    if (Settings.IMP.PVP_TIME > 0) {
      String title = Settings.IMP.MESSAGES.IN_PVP_BOSSBAR;
      if (!title.isEmpty()) {
        var add = 1d / (double) Settings.IMP.PVP_TIME;
        var progress = add;
        for (int i = 1; i <= Settings.IMP.PVP_TIME; i++) {
          var component = MessageUtil.deserialize(title, i);
          BossBar bar = BossBar.bossBar(component, (float) progress, Settings.IMP.BOSSBAR_COLOR, Settings.IMP.BOSSBAR_OVERLAY );
          progress += add;
          bossBars.put(i, bar);
          if (progress > 1.000d) {
            progress = 1.000d;
          }
        }
      }
    }
  }

  public void setBossBar(Player player, int time) {
    if (!bossBars.isEmpty()) {
      bossBars.values().forEach(player::hideBossBar);
      player.showBossBar(bossBars.get(time));
    }
  }

  public void clearBossbar(Player player) {
    for (BossBar bar : bossBars.values()) {
      player.hideBossBar(bar);
    }
  }

  public void clearBossbars() {
    if (!bossBars.isEmpty()) {
      Bukkit.getOnlinePlayers()
          .forEach(player ->
              bossBars.values().forEach(player::hideBossBar));
    }
    bossBars.clear();
  }
}
