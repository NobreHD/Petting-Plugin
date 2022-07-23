package nobrehd;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
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
            List keys = new ArrayList(timeout_players.keySet());
            for (Object key: keys) {
                long unix = (Long) timeout_players.get((String) key);
                long actual_unix = System.currentTimeMillis()/1000L;
                if (unix + 3 < actual_unix){
                    timeout_players.remove((String) key);
                }
            }
        }, 0, 60);
        this.getCommand("update").setExecutor(this);
    }

    @Override
    public void onDisable() {}

    @EventHandler
    public void PlayerRightClick(PlayerInteractEntityEvent event){
        Player player = event.getPlayer();
        if (player.isSneaking()) {
            Entity entity = event.getRightClicked();
            String nametag = entity.getCustomName();
            List<String> selection = new LinkedList<String>(Arrays.asList());
            selection.addAll(this.greetings.get("both"));
            if (entity instanceof Wolf){
                if (nametag != null){
                    selection.addAll(this.greetings.get("dogs_named"));
                }
                selection.addAll(this.greetings.get("dogs"));
            }else if(entity instanceof Cat){
                if (nametag != null){
                    selection.addAll(this.greetings.get("cats_named"));
                }
                selection.addAll(this.greetings.get("cats_named"));
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

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (sender.isOp()){
            Map<String, List<String>> list = update();
            if (list != null) {
                this.greetings = list;
                sender.sendMessage("List Updated");
                return true;
            }
            sender.sendMessage("Error Receiving Data");
        }else{
            sender.sendMessage("You don't have permissions to run this command");
        }
        return false;
    }

    static public Map<String, List<String>> update(){
        try {
            URL url = new URL("https://nobrehd.github.io/Petting-Plugin/api/greetings.json");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            connection.disconnect();
            Type mapType = new TypeToken<Map<String,List<String>>>(){}.getType();
            return new Gson().fromJson(content.toString(), mapType);
        } catch (Exception e){}
        return null;
    }
}
