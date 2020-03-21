package world.bentobox.bentobox.blueprints.worldedit;

import java.io.IOException;
import java.io.InputStream;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;

/**
 * @since 1.6.0
 * @author CustomEntity
 */
public class BlueprintClipboardReader implements ClipboardReader {

    private InputStream inputStream;

    public BlueprintClipboardReader(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public Clipboard read() throws IOException {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public void close() throws IOException {
        throw new UnsupportedOperationException(); // TODO
    }

    public InputStream getInputStream() {
        return inputStream;
    }
}
