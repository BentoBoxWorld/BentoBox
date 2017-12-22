package us.tastybento.bskyblock.api;

import java.io.File;

public interface BSModule {

    String getIdentifier();
    boolean isAddon();
    File getFolder();
}
