package nobrehd;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class Petting extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Petting Listener Added");
    }

    @Override
    public void onDisable() {}

    @EventHandler
    public void PlayerRightClick(PlayerInteractEntityEvent event){
        Entity entity = event.getRightClicked();
        if (!(entity instanceof Wolf || entity instanceof Cat)) return;
        Tameable cuties = (Tameable) entity;
        if (!cuties.isTamed()) return;
        Player player = event.getPlayer();
        Location spawn = cuties.getLocation();
        spawn.add(0,1,0);
        player.spawnParticle(Particle.HEART, spawn, 2);
    }
}
