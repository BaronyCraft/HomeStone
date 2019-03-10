package de.guntram.bukkit.HomeStone;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.CreateClaimResult;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {
    
    public static Main instance;
    
    private File stonesDef, homesDef;
    private List<HomeStoneDefinition> stones;
    private Plugin griefPrevention;
    
    @Override 
    public void onEnable() {
        if (instance==null)
            instance=this;
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        stonesDef=new File(this.getDataFolder(), "stones.json");
        homesDef=new File(this.getDataFolder(), "homes.json");
        Homelist.load(homesDef);
        stones=HomeStoneDefinition.load(stonesDef);
        griefPrevention = getServer().getPluginManager().getPlugin("GriefPrevention");
        getServer().getPluginManager().registerEvents(this, this);
    }
    
    @Override
    public void onDisable() {
        HomeStoneDefinition.save(stones, stonesDef);
        Homelist.save(homesDef);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String commandName=command.getName();
        Location targetLocation;
        if (commandName.equals("home")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Can only be used by players");
                return true;
            }
            int homeIndex;
            if (args.length>0) {
                homeIndex=Integer.parseInt(args[0])-1;
            } else {
                homeIndex=0;
            }
            List<Home> homes = Homelist.get(((Player)sender).getUniqueId(), ((Player)sender).getWorld());
            if (homes.size()<=homeIndex) {
                sender.sendMessage("You have only "+homes.size()+" homes set");
            } else {
                ((Player)sender).teleport(homes.get(homeIndex).getLocation());
            }
            return true;
        }
        if (commandName.equals("visit")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Can only be used by players");
                return true;
            }
            if (args.length<1) {
                sender.sendMessage("Give a player name");
                return true;
            }
            int homeIndex;
            if (args.length>1) {
                homeIndex=Integer.parseInt(args[1])-1;
            } else {
                homeIndex=0;
            }
            List<Home> homes = Homelist.get(args[0], ((Player)sender).getWorld());
            if (homes.size()<=homeIndex) {
                sender.sendMessage(args[0]+" has only "+homes.size()+" homes set");
            } else {
                ((Player)sender).teleport(homes.get(homeIndex).getLocation());
            }
            return true;
        }
        if (commandName.equals("homestone") && args.length==3 && args[0].equals("give")) {
            Player player = Bukkit.getPlayer(args[1]);
            if (player==null) {
                sender.sendMessage("Player "+args[1]+" not found");
                return true;
            }
            int stoneIndex=Integer.parseInt(args[2])-1;
            if (stones.size()<stoneIndex) {
                sender.sendMessage("Not that many stones defined");
                return true;
            } else if (stoneIndex<0) {
                sender.sendMessage("Invalid index");
                return true;
            }
            ItemStack stack=new ItemStack(Material.getMaterial(stones.get(stoneIndex).material));
            ItemMeta meta=stack.getItemMeta();
            meta.setDisplayName(stones.get(stoneIndex).displayName);
            ArrayList<String>lore=new ArrayList<>();
            lore.add(stones.get(stoneIndex).lore);
            meta.setLore(lore);
            stack.setItemMeta(meta);
            player.getInventory().addItem(stack);
            return true;
        }
        return false;
    }
    
    
    @EventHandler(priority=EventPriority.NORMAL)
    public void onPlaceBlock(BlockPlaceEvent event) {
        if (event.isCancelled())
            return;
        Player player=event.getPlayer();
        if (player==null)
            return;
        Block block=event.getBlockPlaced();
        ItemStack item=event.getItemInHand();
        if (block==null || item==null)
            return;

        for (HomeStoneDefinition def: stones) {
            if (block.getType().name().equals(def.material)
            &&  item.getItemMeta().getDisplayName().equals(def.displayName)) {
                if (!def.canPlaceIn(player.getWorld())) {
                    player.sendMessage("Can't place this stone in this world");
                    event.setCancelled(true);
                    return;
                }
                List homes=Homelist.get(player.getUniqueId(), player.getWorld());
                if (def.stoneNumber >= 0 && def.stoneNumber <= homes.size()) {
                    player.sendMessage("This stone is good for home "+def.stoneNumber+" but you have "+homes.size()+" in this world");
                    event.setCancelled(true);
                    return;
                }
                if (griefPrevention != null) {
                    if (isCloseToClaim(player, def.minDistanceFromOthers) != null) {
                        player.sendMessage("You are too close to another claim");
                        event.setCancelled(true);
                        return;
                    }
                    if (!createClaim(player, def.claimRange)) {
                        player.sendMessage("Claiming didn't work for some reason");
                        event.setCancelled(true);
                        return;
                    }
                }
                player.sendMessage("Claimed this land and set a home for you");
                Homelist.add(player, event.getBlock().getLocation().add(0, 1, 0));
                Homelist.save(homesDef);
                
            }
        }
    }
    
    private Claim isCloseToClaim(Player player, int minDistance) {
        Collection<Claim> claims = GriefPrevention.instance.dataStore.getClaims();
        for (Claim claim:claims) {
            Location topleft=claim.getLesserBoundaryCorner();
            Location botright=claim.getGreaterBoundaryCorner();
            if (topleft.getWorld() != player.getWorld()) {
                continue;
            }
            if (topleft .getBlockX() - minDistance < player.getLocation().getBlockX()
            &&  topleft .getBlockZ() - minDistance < player.getLocation().getBlockZ()
            &&  botright.getBlockX() + minDistance > player.getLocation().getBlockX()
            &&  botright.getBlockZ() + minDistance > player.getLocation().getBlockZ()) {
                return claim;
            }
        }
        return null;
    }
    
    private boolean createClaim(Player player, int range) {
        Location loc=player.getLocation();
        CreateClaimResult result=GriefPrevention.instance.dataStore.createClaim(
                player.getWorld(),
                loc.getBlockX()-range, loc.getBlockX()+range,
                0, 255,
                loc.getBlockZ()-range, loc.getBlockZ()+range,
                player.getUniqueId(), null, null, player);
        return result.succeeded;
    }
}