package nobrehd;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.function.BiConsumer;

public final class Petting extends JavaPlugin implements Listener {

    List<String> dogs_with_nametag = Arrays.asList("*?name starts wagging its tail*");
    List<String> cats_with_nametag = Arrays.asList("Pss pss pss, ?name come here");
    List<String> dogs_without_nametag = Arrays.asList("Who is the cutest doggo?", "Good boy");
    List<String> cats_without_nametag = Arrays.asList("Here, kitty, kitty");
    Random rand = new Random();
    JSONObject timeout_players = new JSONObject();
    int repeat_id = 0;

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Petting Listener Added");
        repeat_id = this.getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
            List keys = new ArrayList(timeout_players.keySet());
            for (Object key: keys) {
                long unix = (Long) timeout_players.get((String) key);
                long actual_unix = System.currentTimeMillis()/1000L;
                if (unix + 3 < actual_unix){
                    timeout_players.remove((String) key);
                }
            }
        }, 0, 60);
    }

    @Override
    public void onDisable() {}

    @EventHandler
    public void PlayerRightClick(PlayerInteractEntityEvent event){
        Player player = event.getPlayer();
        if (player.isSneaking()) {
            Entity entity = event.getRightClicked();
            String nametag = entity.getCustomName();
            List<String> selection = new ArrayList<>();
            if (entity instanceof Wolf){
                if (nametag != null){
                    selection.addAll(dogs_with_nametag);
                }
                selection.addAll(dogs_without_nametag);
            }else if(entity instanceof Cat){
                if (nametag != null){
                    selection.addAll(cats_with_nametag);
                }
                selection.addAll(cats_without_nametag);
            }else return;
            String text = selection.get(rand.nextInt(selection.size())).replace("?name", nametag == null? "" : nametag);
            Tameable cuties = (Tameable) entity;
            if (!cuties.isTamed()) return;
            Location spawn = cuties.getLocation();
            spawn.add(0, 1, 0);
            player.spawnParticle(Particle.VILLAGER_HAPPY, spawn, 2);
            TextComponent message = new TextComponent("\u2661 " + text + " \u2661");
            message.setColor(ChatColor.LIGHT_PURPLE);
            String timeout_key = player.getUniqueId() + "-" + entity.getUniqueId();
            if (timeout_players.containsKey(timeout_key)){
                long unix = (Long) timeout_players.get(timeout_key);
                long actual_unix = System.currentTimeMillis()/1000L;
                if (unix + 3 >= actual_unix){
                    event.setCancelled(true);
                    return;
                }
                timeout_players.remove(timeout_key);
            }
            timeout_players.put(timeout_key, System.currentTimeMillis()/1000L);
            player.sendMessage(message);
            event.setCancelled(true);
        }
    }
}
