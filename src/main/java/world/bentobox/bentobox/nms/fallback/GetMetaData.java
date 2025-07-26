package world.bentobox.bentobox.nms.fallback;

import org.bukkit.block.Block;

import world.bentobox.bentobox.nms.AbstractMetaData;

/**
 * Fallback
 */
public class GetMetaData extends AbstractMetaData {

    @Override
    public String nmsData(Block block) {
        return ""; // We cannot read it if we have no NMS
    }

}
