package me.xentany.antirelog.listeners;

import me.xentany.antirelog.Settings;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.codemc.worldguardwrapper.event.WrappedDisallowedPVPEvent;
import me.xentany.antirelog.manager.PvPManager;

public class WorldGuardListener implements Listener {

  private final PvPManager pvpManager;

  public WorldGuardListener(PvPManager pvpManager) {
    this.pvpManager = pvpManager;
  }

  @EventHandler
  public void onPvP(WrappedDisallowedPVPEvent event) {
    if (!pvpManager.isPvPModeEnabled() || !Settings.IMP.IGNORE_WORLDGUARD) {
      return;
    }

    Player attacker = event.getAttacker();
    Player defender = event.getDefender();

    boolean attackerInPvp = pvpManager.isInPvP(attacker) || pvpManager.isInSilentPvP(attacker);
    boolean defenderInPvp = pvpManager.isInPvP(defender) || pvpManager.isInSilentPvP(defender);

    if (attackerInPvp && defenderInPvp) {
      event.setCancelled(true);
      event.setResult(Result.DENY); //Deny means cancelled means pvp allowed
    } else if (Settings.IMP.JOIN_PVP_IN_WORLDGUARD && defenderInPvp) {
      event.setCancelled(true);
      event.setResult(Result.DENY); //Deny means cancelled means pvp allowed
    }
  }
}
