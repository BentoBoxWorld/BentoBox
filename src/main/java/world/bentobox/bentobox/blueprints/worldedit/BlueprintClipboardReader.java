package world.bentobox.bentobox.blueprints.worldedit;

import java.io.InputStream;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;

/**
 * @since 1.6.0
 * @author CustomEntity
 */
public class BlueprintClipboardReader implements ClipboardReader {

    private final InputStream inputStream;

    public BlueprintClipboardReader(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public Clipboard read() {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException(); // TODO
    }

    public InputStream getInputStream() {
        return inputStream;
    }
}
