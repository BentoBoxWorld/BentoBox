package world.bentobox.bentobox.api.metadata;

import org.eclipse.jdt.annotation.NonNull;

import com.google.gson.annotations.Expose;

/**
 * Stores metadata value in a GSON friendly way so it can be serialized and deserialized.
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
        switch (value) {
            case Integer i -> intValue = i;
            case Float f -> floatValue = f;
            case Double d -> doubleValue = d;
            case Long l -> longValue = l;
            case Short s -> shortValue = s;
            case Byte b -> byteValue = b;
            case Boolean bo -> booleanValue = bo;
            case String st -> stringValue = st;
            default -> {
            }
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
