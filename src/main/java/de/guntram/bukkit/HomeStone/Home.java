package de.guntram.bukkit.HomeStone;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Home {
    
    private UUID owner;
    private String lastKnownOwnerName;
    double x, y, z;
    String worldName;
    
    public Home(Player player) {
        this(player, player.getLocation());
    }

    public Home(Player player, Location location) {
        this.owner=player.getUniqueId();
        this.lastKnownOwnerName=player.getName();
        this.x=location.getX();
        this.y=location.getY();
        this.z=location.getZ();
        this.worldName=location.getWorld().getName();
    }
    
    public UUID getOwner() { return owner; }
    public String getOwnerName() { return lastKnownOwnerName; }
    public void setOwnerName(String name) { lastKnownOwnerName=name; }
    public Location getLocation() { return new Location(Bukkit.getWorld(worldName), x, y, z); }
}
