package de.guntram.bukkit.HomeStone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

class Homelist {
    
    static private List<Home> homes;

    static List get(UUID uniqueId, World world) {
        List<Home> result=new ArrayList<>();
        for (Home home:homes) {
            if (home.getOwner().equals(uniqueId)
            &&  home.getLocation().getWorld().getName().equals(world.getName())) {
                result.add(home);
            }
        }
        return result;
    }
    
    static List<Home> get(String name, World world) {
        List<Home> result=new ArrayList<>();
        for (Home home:homes) {
            if (home.getOwnerName().equals(name)
            &&  home.getLocation().getWorld().getName().equals(world.getName())) {
                result.add(home);
            }
        }
        return result;
    }

    static void load(File file) {
        Type type=new TypeToken<ArrayList<Home>>(){}.getType();
        try(JsonReader reader=new JsonReader(new FileReader(file))) {
            Gson gson=new Gson();
            homes=gson.fromJson(reader, type);
        } catch(IOException ex) {
            homes = new ArrayList<>();
        }
    }

    static void add(Player player, Location location) {
        homes.add(new Home(player, location));
    }

    static void save(File file) {
        if (homes.isEmpty()) {
            System.err.println("Save homes: list is empty");
            return;
        }
        try (FileWriter writer=new FileWriter(file)) {
            Gson gson=new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(homes, writer);
        } catch (IOException ex) {
            System.err.println("Trying to save homes:");
            ex.printStackTrace(System.err);            
        }
    }

    static void updatePlayer(Player player) {
        UUID uuid=player.getUniqueId();
        String name=player.getName();
        for (Home home:homes) {
            if (home.getOwner().equals(uuid)) {
                home.setOwnerName(name);
            }
        }
    }
}
