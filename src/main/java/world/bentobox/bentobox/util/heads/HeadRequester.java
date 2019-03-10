package world.bentobox.bentobox.util.heads;

import world.bentobox.bentobox.api.panels.PanelItem;

public interface HeadRequester {

    /**
     * Replaces the head in an open inventory panel with the supplied panel item
     * @param item - panel item, must be a player head
     */
    void setHead(PanelItem item);
}
