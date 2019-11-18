package world.bentobox.bentobox.blueprints.worldedit;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.database.json.BentoboxTypeAdapterFactory;

/**
 * @since 1.6.0
 * @author CustomEntity
 */
public class BlueprintClipboardFormat implements ClipboardFormat {

    public BlueprintClipboardFormat() {
        ClipboardFormats.registerClipboardFormat(this);
    }

    @Override
    public String getName() {
        return "Blueprint";
    }

    @Override
    public Set<String> getAliases() {
        return Sets.newHashSet("Bp");
    }

    @Override
    public ClipboardReader getReader(InputStream inputStream) throws IOException {
        return new BlueprintClipboardReader(inputStream);
    }

    @Override
    public ClipboardWriter getWriter(OutputStream outputStream) throws IOException {
        return new BlueprintClipboardWriter(outputStream);
    }

    @Override
    public boolean isFormat(File file) {
        try {
            Gson gson = getGson();

            unzip(file.getAbsolutePath());

            File unzippedFile = new File(file.getParentFile(), file.getName());

            return gsonCheck(gson, unzippedFile);

        } catch (IOException e) {
            BentoBox.getInstance().logStacktrace(e);
        }
        return false;
    }

    private boolean gsonCheck(Gson gson, File unzippedFile) {
        try (FileReader fr = new FileReader(unzippedFile)) {
            gson.fromJson(fr, Blueprint.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getPrimaryFileExtension() {
        return "blueprint";
    }

    @Override
    public Set<String> getFileExtensions() {
        return ImmutableSet.of("blu", "blueprint");
    }

    private Gson getGson() {
        GsonBuilder builder = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().enableComplexMapKeySerialization();
        // Disable <>'s escaping etc.
        builder.disableHtmlEscaping();
        // Register adapter factory
        builder.registerTypeAdapterFactory(new BentoboxTypeAdapterFactory(BentoBox.getInstance()));
        return builder.create();
    }

    private void unzip(final String zipFilePath) throws IOException {
        Path path = Paths.get(zipFilePath);
        if (!(path.toFile().exists())) {
            throw new IOException("No file exists!");
        }
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry = zipInputStream.getNextEntry();
            while (entry != null) {
                Path filePath = Paths.get(path.getParent().toString(), entry.getName());
                if (!entry.isDirectory()) {
                    unzipFiles(zipInputStream, filePath);
                } else {
                    Files.createDirectories(filePath);
                }

                zipInputStream.closeEntry();
                entry = zipInputStream.getNextEntry();
            }
        }
    }

    private void unzipFiles(final ZipInputStream zipInputStream, final Path unzipFilePath) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(unzipFilePath.toAbsolutePath().toString()))) {
            byte[] bytesIn = new byte[1024];
            int read;
            while ((read = zipInputStream.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }
    }
}
