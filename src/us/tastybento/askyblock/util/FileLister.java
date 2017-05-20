package us.tastybento.askyblock.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import us.tastybento.askyblock.ASkyBlock;

/**
 * @author Tastybento
 * @author Poslovitch
 */
public class FileLister{
    private ASkyBlock plugin;
    
    public FileLister(ASkyBlock plugin){
        this.plugin = plugin;
    }
    
    public List<File> list(String folderPath, boolean checkJar){
        List<File> result = new ArrayList<File>();
        
        File folder = new File(plugin.getDataFolder(), folderPath);
        if(folder.exists()){
        
        }
        return result;
    }
}
