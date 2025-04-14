package me.xentany.antirelog.manager;

import me.xentany.antirelog.Settings;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import me.xentany.antirelog.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class BossbarManager {

  private final Map<Integer, BossBar> bossBars = new HashMap<>();

  public void createBossBars() {
    bossBars.clear();
    if (Settings.IMP.PVP_TIME > 0) {
      String title = Utils.color(Settings.IMP.MESSAGES.IN_PVP_BOSSBAR);
      if (!title.isEmpty()) {
        double add = 1d / (double) Settings.IMP.PVP_TIME;
        double progress = add;
        for (int i = 1; i <= Settings.IMP.PVP_TIME; i++) {
          String actualTitle = Utils.replaceTime(title, i);
          BossBar bar = Bukkit.createBossBar(actualTitle, BarColor.RED, BarStyle.SOLID);
          bar.setProgress(progress);
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
      for (BossBar bar : bossBars.values()) {
        bar.removePlayer(player);
      }
      bossBars.get(time).addPlayer(player);
    }
  }

  public void clearBossbar(Player player) {
    for (BossBar bar : bossBars.values()) {
      bar.removePlayer(player);
    }
  }

  public void clearBossbars() {
    if (!bossBars.isEmpty()) {
      for (BossBar bar : bossBars.values()) {
        bar.removeAll();
      }
    }
    bossBars.clear();
  }
}
