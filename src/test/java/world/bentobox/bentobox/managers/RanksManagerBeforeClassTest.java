package world.bentobox.bentobox.managers;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.DatabaseSetup;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ BentoBox.class, DatabaseSetup.class, RanksManager.class })
public abstract class RanksManagerBeforeClassTest {

    // Constants that define the hard coded rank values
    public static final String ADMIN_RANK_REF = "ranks.admin";
    public static final String MOD_RANK_REF = "ranks.mod";
    public static final String OWNER_RANK_REF = "ranks.owner";
    public static final String SUB_OWNER_RANK_REF = "ranks.sub-owner";
    public static final String MEMBER_RANK_REF = "ranks.member";
    public static final String TRUSTED_RANK_REF = "ranks.trusted";
    public static final String COOP_RANK_REF = "ranks.coop";
    public static final String VISITOR_RANK_REF = "ranks.visitor";
    public static final String BANNED_RANK_REF = "ranks.banned";
    public static final int ADMIN_RANK = 10000;
    public static final int MOD_RANK = 5000;
    public static final int OWNER_RANK = 1000;
    public static final int SUB_OWNER_RANK = 900;
    public static final int MEMBER_RANK = 500;
    public static final int TRUSTED_RANK = 400;
    public static final int COOP_RANK = 200;
    public static final int VISITOR_RANK = 0;
    public static final int BANNED_RANK = -1;

    // The store of ranks
    public static final Map<String, Integer> DEFAULT_RANKS = Map.of(ADMIN_RANK_REF, ADMIN_RANK, MOD_RANK_REF, MOD_RANK,
            OWNER_RANK_REF, OWNER_RANK, SUB_OWNER_RANK_REF, SUB_OWNER_RANK, MEMBER_RANK_REF, MEMBER_RANK,
            TRUSTED_RANK_REF, TRUSTED_RANK, COOP_RANK_REF, COOP_RANK, VISITOR_RANK_REF, VISITOR_RANK, BANNED_RANK_REF,
            BANNED_RANK);

    @Mock
    public BentoBox plugin;
    @Mock
    public RanksManager rm;

    @Before
    public void setUp() throws Exception {
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // RanksManager
        PowerMockito.mockStatic(RanksManager.class, Mockito.RETURNS_MOCKS);
        when(RanksManager.getInstance()).thenReturn(rm);
        when(rm.getRanks()).thenReturn(DEFAULT_RANKS);
        when(rm.getRank(anyInt())).thenReturn("");
    }

    @After
    public void tearDown() throws IOException {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
        deleteAll(new File("database"));
        deleteAll(new File("database_backup"));
    }

    private void deleteAll(File file) throws IOException {
        if (file.exists()) {
            Files.walk(file.toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }

    }

}
