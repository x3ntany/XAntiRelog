package me.xentany.antirelog.listeners;

import me.xentany.antirelog.Settings;
import net.ess3.api.events.teleport.PreTeleportEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import me.xentany.antirelog.manager.PvPManager;

public class EssentialsTeleportListener implements Listener {

  private final PvPManager pvpManager;

  public EssentialsTeleportListener(PvPManager pvpManager) {
    this.pvpManager = pvpManager;
  }

  @EventHandler
  public void onPreTeleport(PreTeleportEvent event) {
    if (Settings.IMP.DISABLE_TELEPORTS_IN_PVP && pvpManager.isInPvP(event.getTeleportee().getBase())) {
      event.setCancelled(true);
    }
  }
}