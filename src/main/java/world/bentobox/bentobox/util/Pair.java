package world.bentobox.bentobox.util;


import org.eclipse.jdt.annotation.NonNull;

/**
 * Class to store pairs of objects, e.g. coordinates
 *
 * @param <X> the x part of the pair
 * @param <Z> the z part of the pair
 * @author tastybento
 */
public record Pair<X, Z>(X x, Z z) {
    /**
     * Static factory method to create a Pair.
     *
     * @param x the x part
     * @param z the z part
     * @return a new Pair containing x and z
     */
    public static <X, Z> Pair<X, Z> of(X x, Z z) {
        return new Pair<>(x, z);
    }


    /**
     * Returns X element as key.
     *
     * @return X element
     */
    public X getKey() {
        return x;
    }


    /**
     * Returns Z element as value.
     *
     * @return Z element
     */
    public Z getValue() {
        return z;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public @NonNull String toString() {
        return "Pair [x=" + x + ", z=" + z + "]";
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Pair<?, ?>(Object x1, Object z1))) {
            return false;
        }
        if (x == null) {
            return false;
        } else if (!x.equals(x1)) {
            return false;
        }
        if (z == null) {
            return false;
        } else return z.equals(z1);
    }

}