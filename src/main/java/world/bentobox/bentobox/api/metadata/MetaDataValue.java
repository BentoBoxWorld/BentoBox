package world.bentobox.bentobox.api.metadata;

import org.eclipse.jdt.annotation.NonNull;

import com.google.gson.annotations.Expose;

/**
 * Stores meta data value in a GSON friendly way so it can be serialized and deserialized.
 * Values that are null are not stored in the database, so only the appropriate type is stored.
 * @author tastybento
 * @since 1.15.4
 *
 */
public class MetaDataValue {

    // Use classes so null value is supported
    @Expose
    private Integer intValue;
    @Expose
    private Float floatValue;
    @Expose
    private Double doubleValue;
    @Expose
    private Long longValue;
    @Expose
    private Short shortValue;
    @Expose
    private Byte byteValue;
    @Expose
    private Boolean booleanValue;
    @Expose
    private String stringValue;

    /**
     * Initialize this meta data value
     * @param value the value assigned to this metadata value
     */
    public MetaDataValue(@NonNull Object value) {
        if (value instanceof Integer) {
            intValue = (int)value;
        } else if (value instanceof Float) {
            floatValue = (float)value;
        } else if (value instanceof Double) {
            doubleValue = (double)value;
        } else if (value instanceof Long) {
            longValue = (long)value;
        } else if (value instanceof Short) {
            shortValue = (short)value;
        } else if (value instanceof Byte) {
            byteValue = (byte)value;
        } else if (value instanceof Boolean) {
            booleanValue = (boolean)value;
        } else if (value instanceof String) {
            stringValue = (String)value;
        }
    }

    public int asInt() {
        return intValue;
    }

    public float asFloat() {
        return floatValue;
    }

    public double asDouble() {
        return doubleValue;
    }

    public long asLong() {
        return longValue;
    }

    public short asShort() {
        return shortValue;
    }

    public byte asByte() {
        return byteValue;
    }

    public boolean asBoolean() {
        return booleanValue;
    }

    @NonNull
    public String asString() {
        return stringValue == null ? "" : stringValue;
    }
}
