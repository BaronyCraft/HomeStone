/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.bukkit.HomeStone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

/**
 *
 * @author gbl
 */
public class HomeStoneTabCompleter implements TabCompleter {

    List<HomeStoneDefinition> stones;
    static final List<String> subCommands=Arrays.asList("give", "list", "resetworld");

    HomeStoneTabCompleter(List<HomeStoneDefinition> stones) {
        this.stones=stones;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        
        if (!(sender.hasPermission("homestone.give")))
            return null;

        List<String> completions=new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], subCommands, completions);
        }
        else if (args.length == 2 && args[0].equals("give")) {
            for (Player player: Bukkit.getOnlinePlayers()) {
                if (StringUtil.startsWithIgnoreCase(player.getName(), args[1]))
                    completions.add(player.getName());
            }
        }
        else if (args.length == 3 && args[0].equals("give")) {
            for (HomeStoneDefinition stone: stones) {
                if (StringUtil.startsWithIgnoreCase(stone.givenName, args[2]))
                    completions.add(stone.givenName);
            }
        }
        return completions;
    }
}
