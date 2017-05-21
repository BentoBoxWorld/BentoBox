package us.tastybento.bskyblock.database;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.database.flatfile.FlatFileDatabaseInserter;
import us.tastybento.bskyblock.database.flatfile.FlatFileDatabaseSelecter;
import us.tastybento.bskyblock.database.flatfile.FlatFileDatabaseConnecter;

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

            FlatFileDatabaseInserter<Test> inserter = new FlatFileDatabaseInserter<Test>(plugin, Test.class, connecter);

            inserter.insertObject(test);

            FlatFileDatabaseSelecter<Test> selecter = new FlatFileDatabaseSelecter<Test>(plugin, Test.class, connecter);

            test = selecter.selectObject();

            plugin.getLogger().info("DEBUG: name = " + test.getName());
            plugin.getLogger().info("DEBUG: id = " + test.getId());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}




