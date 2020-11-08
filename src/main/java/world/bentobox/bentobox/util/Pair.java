package world.bentobox.bentobox.util;


/**
 * Class to store pairs of objects, e.g. coordinates
 * @author tastybento
 *
 * @param <X> the x part of the pair
 * @param <Z> the z part of the pair
 */
public class Pair<X, Z> {
    public final X x;
    public final Z z;

    public Pair(X x, Z z) {
        this.x = x;
        this.z = z;
    }


    /**
     * Returns X element as key.
     * @return X element
     */
    public X getKey() {
        return x;
    }


    /**
     * Returns Z element as value.
     * @return Z element
     */
    public Z getValue() {
        return z;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Pair [x=" + x + ", z=" + z + "]";
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((x == null) ? 0 : x.hashCode());
        result = prime * result + ((z == null) ? 0 : z.hashCode());
        return result;
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
        if (!(obj instanceof Pair)) {
            return false;
        }
        Pair<?, ?> other = (Pair<?, ?>) obj;
        if (x == null) {
            if (other.x != null) {
                return false;
            }
        } else if (!x.equals(other.x)) {
            return false;
        }
        if (z == null) {
            return other.z == null;
        } else return z.equals(other.z);
    }
    
}