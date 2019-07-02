package world.bentobox.bentobox.blueprints.converter;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by CustomEntity on 7/2/2019 for BentoBox.
 */
public class BlueprintClipboardWriter implements ClipboardWriter{

    private OutputStream outputStream;

    public BlueprintClipboardWriter(OutputStream outputStream) {
        this.outputStream = outputStream;
    }
    @Override
    public void write(Clipboard clipboard) throws IOException {
    }

    @Override
    public void close() throws IOException {

    }

    public OutputStream getOutputStream() {
        return outputStream;
    }
}
