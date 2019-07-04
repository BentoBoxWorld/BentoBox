package world.bentobox.bentobox.blueprints.converter;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @since 1.6.0
 * @author CustomEntity
 */
public class BlueprintClipboardWriter implements ClipboardWriter{

    private OutputStream outputStream;

    public BlueprintClipboardWriter(OutputStream outputStream) {
        this.outputStream = outputStream;
    }
    @Override
    public void write(Clipboard clipboard) throws IOException {
        // TODO
    }

    @Override
    public void close() throws IOException {

    }

    public OutputStream getOutputStream() {
        return outputStream;
    }
}
