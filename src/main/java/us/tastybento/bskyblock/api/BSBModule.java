package us.tastybento.bskyblock.api;

import java.io.File;

public interface BSBModule {

    String getIdentifier();
    boolean isAddon();
    File getFolder();
}
