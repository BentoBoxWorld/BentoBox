package world.bentobox.bentobox.blueprints;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintCommand;
import world.bentobox.bentobox.blueprints.converter.BlueprintClipboardReader;
import world.bentobox.bentobox.blueprints.converter.BlueprintClipboardWriter;
import world.bentobox.bentobox.database.json.BentoboxTypeAdapterFactory;
import world.bentobox.bentobox.managers.BlueprintClipboardManager;
import world.bentobox.bentobox.managers.BlueprintsManager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @since 1.6.0
 * @author CustomEntity
 */
public class BlueprintClipboardFormat implements ClipboardFormat {
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

            try (FileReader fr = new FileReader(unzippedFile)) {
                gson.fromJson(fr, Blueprint.class);
                return true;
            } catch (Exception e) {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
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
