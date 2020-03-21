package world.bentobox.bentobox.blueprints.worldedit;

import java.io.IOException;
import java.io.OutputStream;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;

/**
 * @since 1.6.0
 * @author CustomEntity
 */
public class BlueprintClipboardWriter implements ClipboardWriter {

    private OutputStream outputStream;

    public BlueprintClipboardWriter(OutputStream outputStream) {
        this.outputStream = outputStream;
    }
    @Override
    public void write(Clipboard clipboard) throws IOException {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public void close() throws IOException {
        throw new UnsupportedOperationException(); // TODO
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }
}
