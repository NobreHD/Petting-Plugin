package nobrehd;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public final class Petting extends JavaPlugin implements Listener, CommandExecutor {
    Map<String, List<String>> greetings = update();
    Random rand = new Random();
    JSONObject timeout_players = new JSONObject();
    int repeat_id = 0;

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Petting Listener Added");
        repeat_id = this.getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
            // remove players from timeout
            long actual_unix = System.currentTimeMillis()/1000L;
            for (Object key : timeout_players.keySet()){
                long unix = (Long) timeout_players.get(key);
                if (unix + 3 < actual_unix) timeout_players.remove(key);
            }
        }, 0, 60);
        this.getCommand("petting").setExecutor(this);
    }

    @Override
    public void onDisable() {
        this.getServer().getScheduler().cancelTask(repeat_id);
        getLogger().info("Petting Listener Removed");
    }

    @EventHandler
    public void PlayerRightClick(PlayerInteractEntityEvent event){
        Player player = event.getPlayer();
        if (player.isSneaking()) {
            Entity entity = event.getRightClicked();

            // is the entity tamed?
            if (!(entity instanceof Tameable)) return;

            // is the player in time-out?
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

            Component nametag = entity.customName();

            List<String> selection = new LinkedList<String>(List.of());
            if (entity instanceof Wolf){
                if (nametag != null) selection.addAll(this.greetings.get("dogs_named"));
                selection.addAll(this.greetings.get("dogs"));
            }else if(entity instanceof Cat){
                if (nametag != null) selection.addAll(this.greetings.get("cats_named"));
                selection.addAll(this.greetings.get("cats_named"));
            }else return;

            TextReplacementConfig replacementConfig = TextReplacementConfig.builder()
                    .match("{name}")
                    .replacement(nametag != null ? nametag : Component.text(""))
                    .build();
            Component text = Component.text(selection.get(rand.nextInt(selection.size())))
                    .replaceText(replacementConfig);

            Component heart = Component.text(" ♡ ").color(NamedTextColor.RED);

            Location spawn = entity.getLocation();
            spawn.add(0, 1, 0);
            player.spawnParticle(Particle.VILLAGER_HAPPY, spawn, 2);
            player.sendMessage(heart.append(text).append(heart));
            event.setCancelled(true);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (sender.hasPermission("petting.admin")){
            Map<String, List<String>> list = update();
            if (list != null) {
                this.greetings = list;
                sendMessage(sender, "Data Updated", NamedTextColor.GREEN);
            } else sendMessage(sender, "Error Updating Data", NamedTextColor.RED);
        } else sendMessage(sender, "You don't have permission to use this command", NamedTextColor.RED);
        return true;
    }

    static public Map<String, List<String>> update(){
        try {
            URL url = new URL("https://api.nobrehd.pt/petting");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "application/json");

            if (con.getResponseCode() != 200) {
                return null;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((con.getInputStream())));
            String output = br.readLine();
            con.disconnect();
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, List<String>>>(){}.getType();
            return gson.fromJson(output, type);
        } catch (Exception ignored){}
        return null;
    }

    static void sendMessage(CommandSender sender, String message, NamedTextColor color){
        TextComponent prefix = Component.text("[Petting] ")
                .color(NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(message, color));
        sender.sendMessage(prefix);
    }
}
