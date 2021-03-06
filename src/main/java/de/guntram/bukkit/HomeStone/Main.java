package de.guntram.bukkit.HomeStone;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
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
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {
    
    public static Main instance;
    
    private File stonesDef, homesDef;
    private List<HomeStoneDefinition> stones;
    private Map<UUID, VisitHistoryEntry> lastVisits;
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
        lastVisits=new HashMap<>();
        griefPrevention = getServer().getPluginManager().getPlugin("GriefPrevention");
        getCommand("homestone").setTabCompleter(new HomeStoneTabCompleter(stones));
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
                try {
                    homeIndex=Integer.parseInt(args[0])-1;
                } catch (NumberFormatException ex) {
                    sender.sendMessage("Use numerical home destinations");
                    return true;
                }
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
                if (safeToVisit(homes.get(homeIndex).getLocation(), args[0], homeIndex+1)) {
                    lastVisits.put(((Player)sender).getUniqueId(), new VisitHistoryEntry(((Player)sender).getLocation(), System.currentTimeMillis()));
                    ((Player)sender).teleport(homes.get(homeIndex).getLocation());
                } else {
                    sender.sendMessage("That home is not safe, not teleporting");
                }
            }
            return true;
        }
        if (commandName.equals("homestone") && !sender.hasPermission("homestone.give")) {
            sender.sendMessage("You may not do that");
            return true;
        }
        if (commandName.equals("homestone") && args.length==3 && args[0].equals("give")) {
            Player player = Bukkit.getPlayer(args[1]);
            if (player==null) {
                sender.sendMessage("Player "+args[1]+" not found");
                return true;
            }
            int stoneIndex;
            try {
                stoneIndex=Integer.parseInt(args[2])-1;
            } catch (NumberFormatException ex) {
                stoneIndex=-1;
                for (int i=0; i<stones.size(); i++) {
                    if (args[2].equalsIgnoreCase(stones.get(i).givenName)) {
                        stoneIndex=i;
                        break;
                    }
                }
                if (stoneIndex==-1) {
                    sender.sendMessage("No stone with name "+args[2]+", try /homestone list");
                }
            }
            if (stones.size()<=stoneIndex) {
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
            lore.addAll(Arrays.asList(stones.get(stoneIndex).lore.split("_")));
            meta.setLore(lore);
            stack.setItemMeta(meta);
            player.getInventory().addItem(stack);
            return true;
        }
        if (commandName.equals("homestone") && args.length==2 && args[0].equals("resetworld")) {
            Homelist.resetWorld(args[1]);
            Homelist.save(homesDef);
            return true;
        }
        if (commandName.equals("homestone") && args.length==1 && args[0].equals("list")) {
            for (int i=0; i<stones.size(); i++) {
                sender.sendMessage("Stone "+(i+1)+": "+stones.get(i).givenName);
            }
            return true;
        }
        return false;
    }
    
    private boolean safeToVisit(Location loc, String homeOwner, int homeNumber) {
        try {
            int blockx=loc.getBlockX();
            int blocky=loc.getBlockY();
            int blockz=loc.getBlockZ();
            for (int dx=-1; dx<=1; dx++) {
                for (int dz=-1; dz<=1; dz++) {
                    for (int dy=0; dy<=2; dy++) {
                        Block block=loc.getWorld().getBlockAt(blockx+dx, blocky+dy, blockz+dz);
                        if (!(block.isEmpty())) {
                            getLogger().log(Level.WARNING, "{0}''s home {1} is not safe to visit; Block at {2}/{3}/{4} is {5}", new Object[]{homeOwner, homeNumber, blockx+dx, blocky+dy, blockz+dz, block.getType()});
                            return false;
                        }
                    }
                }
            }
            if (!(loc.getWorld().getBlockAt(blockx, blocky-1, blockz).getType().isSolid())) {
                getLogger().log(Level.WARNING, "{0}''s home {1} is not safe to visit; would stand on {2}", new Object[]{homeOwner, homeNumber, loc.getWorld().getBlockAt(blockx, blocky-1, blockz).getType()});
                return false;
            }
            return true;
        } catch (NullPointerException ex) {
                getLogger().log(Level.SEVERE, "NPE when visiting {0}''s home {1}", new Object[]{homeOwner, homeNumber});
                return false;
        }
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
                    if (findBlockingClaim(player, event.getBlock().getLocation(), def.minDistanceFromOthers) != null) {
                        player.sendMessage("You are too close to another claim");
                        event.setCancelled(true);
                        return;
                    }
                    boolean claimed=false;
                    if (def.claimRange > 0) {
                        claimed=createClaim(player, event.getBlock().getLocation(), def.claimRange);
                    } else if (def.cornerGetterClass != null) {
                        try {
                            Class getterClass=Class.forName(def.cornerGetterClass);
                            Method getNortWest=getterClass.getDeclaredMethod("getNorthWest", Location.class);
                            Method getSouthEast=getterClass.getDeclaredMethod("getSouthEast", Location.class);
                            Location nw=(Location) getNortWest.invoke(null, event.getBlock().getLocation());
                            Location se=(Location) getSouthEast.invoke(null, event.getBlock().getLocation());
                            if (nw == null || se == null) {
                                player.sendMessage("You cannot claim there");
                            } else {
                                claimed=createClaim(player, nw, se);
                            }
                        } catch (Exception ex) {
                            getLogger().log(Level.WARNING, "Exception trying to use {0}: {1}\n{2}", new Object[]{def.cornerGetterClass, ex, ex.getStackTrace()});
                            player.sendMessage("Admin's haven't set up claiming in this world correctly");
                        }
                    }
                    
                    if (!claimed) {
                        player.sendMessage("Claiming didn't work for some reason");
                        event.setCancelled(true);
                        return;
                    }
                }
                player.sendMessage("Claimed this land and set a home for you");
                Homelist.add(player, event.getBlock().getLocation().add(0.5, 1.05, 0.5));
                Homelist.save(homesDef);
                
            }
        }
    }
    
    private Claim findBlockingClaim(Player player, Location where, int minDistance) {
        Collection<Claim> claims = GriefPrevention.instance.dataStore.getClaims();
        for (Claim claim:claims) {
            Location topleft=claim.getLesserBoundaryCorner();
            Location botright=claim.getGreaterBoundaryCorner();
            if (topleft.getWorld() != where.getWorld()) {
                continue;
            }
            if (topleft .getBlockX() - minDistance < where.getBlockX()
            &&  topleft .getBlockZ() - minDistance < where.getBlockZ()
            &&  botright.getBlockX() + minDistance > where.getBlockX()
            &&  botright.getBlockZ() + minDistance > where.getBlockZ()) {
                // todo: if player may build there then it's ok
                return claim;
            }
        }
        return null;
    }
    
    private boolean createClaim(Player player, Location around, int range) {
        CreateClaimResult result=GriefPrevention.instance.dataStore.createClaim(
                player.getWorld(),
                around.getBlockX()-range, around.getBlockX()+range,
                0, 255,
                around.getBlockZ()-range, around.getBlockZ()+range,
                player.getUniqueId(), null, null, player);
        return result.succeeded;
    }
    
    private boolean createClaim(Player player, Location northwest, Location southeast) {
        CreateClaimResult result=GriefPrevention.instance.dataStore.createClaim(
                player.getWorld(),
                northwest.getBlockX(), southeast.getBlockX(),
                northwest.getBlockY(), southeast.getBlockY(),
                northwest.getBlockZ(), southeast.getBlockZ(),
                player.getUniqueId(), null, null, player);
        return result.succeeded;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        Homelist.updatePlayer(event.getPlayer());
        Homelist.save(homesDef);
    }
    
    @EventHandler(ignoreCancelled = true, priority=EventPriority.LOWEST)
    public void onPlayerDamageEvent(EntityDamageEvent event) {
        if (!(event.getEntityType() == EntityType.PLAYER))
            return;
        Player player=(Player)event.getEntity();
        VisitHistoryEntry visitInfo=lastVisits.get(player.getUniqueId());
        if (visitInfo == null || visitInfo.when < System.currentTimeMillis() - 5*1000) {
            return;
        }
        getLogger().log(Level.WARNING, "Player {0} would have taken damage at {1}, {2} seconds after visiting", new Object[]{player.getName(), player.getLocation(), visitInfo.when-System.currentTimeMillis()});
        player.sendMessage("You visited a location that isn't safe, returning you to where you were before");
        player.teleport(visitInfo.from);
        event.setCancelled(true);
    }
}
