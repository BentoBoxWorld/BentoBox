package world.bentobox.bentobox.database.json.adapters;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class LocationTypeAdapter extends TypeAdapter<Location> {

    @Override
    public void write(JsonWriter out, Location location) throws IOException {
        if (location == null) {
            out.nullValue();
            return;
        }
        if (location.getWorld() == null) {
            if (location instanceof WorldNameLocation named) {
                // World was not loaded when this was read: write the name back unchanged so the
                // saved coordinates are not lost
                out.beginArray();
                out.value(named.getWorldName());
                out.value(location.getX());
                out.value(location.getY());
                out.value(location.getZ());
                out.value(location.getYaw());
                out.value(location.getPitch());
                out.endArray();
                return;
            }
            // The world was unloaded when this object was loaded (e.g. a disabled Multiverse
            // world). Writing null here would erase centre/home/spawn coordinates permanently;
            // abort the save instead so the previous file stays intact.
            throw new IOException("Refusing to save a location whose world is not loaded ("
                    + location.getX() + ", " + location.getY() + ", " + location.getZ()
                    + "); keeping the previous database file");
        }
        out.beginArray();
        out.value(location.getWorld().getName());
        out.value(location.getX());
        out.value(location.getY());
        out.value(location.getZ());
        out.value(location.getYaw());
        out.value(location.getPitch());
        out.endArray();
    }

    @Override
    public Location read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        in.beginArray();
        String worldName = in.nextString();
        World world = Bukkit.getServer().getWorld(worldName);
        double x = in.nextDouble();
        double y = in.nextDouble();
        double z = in.nextDouble();
        float yaw = (float)in.nextDouble();
        float pitch = (float)in.nextDouble();
        in.endArray();
        if (world == null) {
            // Keep the name so this location can still be written back if the world is loaded
            // later or on the next restart
            return new WorldNameLocation(worldName, x, y, z, yaw, pitch);
        }
        return new Location(world, x, y, z, yaw, pitch);
    }
}