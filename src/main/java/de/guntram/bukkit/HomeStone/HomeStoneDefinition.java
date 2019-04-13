/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.bukkit.HomeStone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.World;

/**
 *
 * @author gbl
 */
public class HomeStoneDefinition {
    String material;
    String displayName;
    String lore;
    String givenName;
    String[] worldsToPlaceIn;
    int stoneNumber;
    int claimRange;
    int minDistanceFromOthers;
    String cornerGetterClass;
    
    public HomeStoneDefinition() {
        material=Material.BEDROCK.name();
        displayName="HomeStone";
        lore="Place this to claim land and set your home";
        givenName="FirstStone";
        worldsToPlaceIn=new String[]{"*"};
        stoneNumber=-1;
        claimRange=100;
        minDistanceFromOthers=110;
        cornerGetterClass = null;
    }
    
    public boolean canPlaceIn(World world) {
        for (String possible: worldsToPlaceIn) {
            if (possible.equals("*") || possible.toLowerCase().equals(world.getName().toLowerCase()))
                return true;
        }
        return false;
    }
    
    static public List<HomeStoneDefinition> load(File file) {
        Type type=new TypeToken<List<HomeStoneDefinition>>(){}.getType();
        try(JsonReader reader=new JsonReader(new FileReader(file))) {
            Gson gson=new Gson();
            return gson.fromJson(reader, type);
        } catch(FileNotFoundException ex) {
            ArrayList<HomeStoneDefinition> result = new ArrayList<>();
            result.add(new HomeStoneDefinition());
            save(result, file);
            return result;
        } catch(IOException ex) {
            System.err.println("Trying to load stones config:");
            ex.printStackTrace(System.err);            
            ArrayList<HomeStoneDefinition> result = new ArrayList<>();
            return result;
        }
    }
    
    static public void save(List<HomeStoneDefinition> def, File file) {
        if (def==null || def.isEmpty()) {
            System.err.println("Save stones config: list is empty");
            return;
        }
        try (FileWriter writer=new FileWriter(file)) {
            Gson gson=new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(def, writer);
        } catch (IOException ex) {
            System.err.println("Trying to save stones config:");
            ex.printStackTrace(System.err);            
        }
    }
}
