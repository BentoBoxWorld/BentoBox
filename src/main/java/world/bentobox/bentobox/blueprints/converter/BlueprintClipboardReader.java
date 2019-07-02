package world.bentobox.bentobox.blueprints.converter;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by CustomEntity on 7/2/2019 for BentoBox.
 */
public class BlueprintClipboardReader implements ClipboardReader {

    private InputStream inputStream;

    public BlueprintClipboardReader(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public Clipboard read() throws IOException {
        return null;
    }

    @Override
    public void close() throws IOException {

    }

    public InputStream getInputStream() {
        return inputStream;
    }
}
