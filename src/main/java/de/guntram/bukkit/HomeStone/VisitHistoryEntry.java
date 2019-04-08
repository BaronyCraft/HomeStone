/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.bukkit.HomeStone;

import org.bukkit.Location;

/**
 * Information about where a player was before they visited someone, and when
 * the visit command happened. Used to get them back when they take damage
 * right after visit (the visitee built a player trap)
 * 
 * @author gbl
 */
public class VisitHistoryEntry {
    public Location from;
    public long when;
    
    /**
     * Create a new History entry
     * @param l the location the player was in before visiting
     * @param t the time (in ms) when the visit happened
     */
    VisitHistoryEntry(Location l, long t) {
        from=l;
        when=t;
    }
}
