package world.bentobox.bentobox;

public class WhiteBox {
    /**
     * Sets the value of a private static field using Java Reflection.
     * @param targetClass The class containing the static field.
     * @param fieldName The name of the private static field.
     * @param value The value to set the field to.
     */
    public static void setInternalState(Class<?> targetClass, String fieldName, Object value) {
        try {
            // 1. Get the Field object from the class
            java.lang.reflect.Field field = targetClass.getDeclaredField(fieldName);

            // 2. Make the field accessible (required for private fields)
            field.setAccessible(true);

            // 3. Set the new value. The first argument is 'null' for static fields.
            field.set(null, value);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Wrap reflection exceptions in a runtime exception for clarity
            throw new RuntimeException("Failed to set static field '" + fieldName + "' on class " + targetClass.getName(), e);
        }
    }
}
