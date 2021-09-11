package world.bentobox.bentobox.blueprints.worldedit;

import java.io.OutputStream;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;

/**
 * @since 1.6.0
 * @author CustomEntity
 */
public class BlueprintClipboardWriter implements ClipboardWriter {

    private final OutputStream outputStream;

    public BlueprintClipboardWriter(OutputStream outputStream) {
        this.outputStream = outputStream;
    }
    @Override
    public void write(Clipboard clipboard) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException(); // TODO
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }
}
