package us.tastybento.bskyblock.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.database.flatfile.FlatFileDatabaseConnecter;
import us.tastybento.bskyblock.database.flatfile.FlatFileDatabaseInserter;
import us.tastybento.bskyblock.database.flatfile.FlatFileDatabaseSelecter;

public class RunTest {

    public RunTest(BSkyBlock plugin) {
        try {

            DatabaseConnecter connecter = new FlatFileDatabaseConnecter(plugin, null);
            
            /*
                    new DatabaseConnectionSettingsImpl(
                            "127.0.0.1", 3306, "exampleDatabase","user", "pass"));
             */
            Test test = new Test();
            test.setId(34);
            test.setName("testname");
            HashMap<Integer, Location> homes = new HashMap<Integer, Location>();
            homes.put(1, new Location(plugin.getServer().getWorld("world"), 1, 2, 3, 1, 1));
            homes.put(2, new Location(plugin.getServer().getWorld("world"), 3, 3, 3, 3, 3));
            test.setHomeLocations(homes);
            List<ItemStack> items = new ArrayList<ItemStack>();
            items.add(new ItemStack(Material.ACTIVATOR_RAIL, 2));
            items.add(new ItemStack(Material.FEATHER,5));
            test.setInventory(items);

            FlatFileDatabaseInserter<Test> inserter = new FlatFileDatabaseInserter<Test>(plugin, Test.class, connecter);

            inserter.insertObject(test);

            FlatFileDatabaseSelecter<Test> selecter = new FlatFileDatabaseSelecter<Test>(plugin, Test.class, connecter);

            test = selecter.selectObject();

            plugin.getLogger().info("DEBUG: name = " + test.getName());
            plugin.getLogger().info("DEBUG: id = " + test.getId());
            homes = test.getHomeLocations();
            plugin.getLogger().info("DEBUG: homes = " + homes);
            items = test.getInventory();
            plugin.getLogger().info("DEBUG: items = " + items);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}




