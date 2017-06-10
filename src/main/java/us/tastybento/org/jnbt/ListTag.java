package us.tastybento.org.jnbt;

import java.util.Collections;
import java.util.List;

/**
 * The <code>TAG_List</code> tag.
 * 
 * @author Graham Edgecombe
 * 
 */
public final class ListTag extends Tag {

    /**
     * The type.
     */
    private final Class<? extends Tag> type;

    /**
     * The value.
     */
    private final List<Tag> value;

    /**
     * Creates the tag.
     * 
     * @param name
     *            The name.
     * @param type
     *            The type of item in the list.
     * @param value
     *            The value.
     */
    public ListTag(String name, Class<? extends Tag> type, List<Tag> value) {
	super(name);
	this.type = type;
	this.value = Collections.unmodifiableList(value);
    }

    /**
     * Gets the type of item in this list.
     * 
     * @return The type of item in this list.
     */
    public Class<? extends Tag> getType() {
	return type;
    }

    @Override
    public List<Tag> getValue() {
	return value;
    }

    @Override
    public String toString() {
	String name = getName();
	String append = "";
	if (name != null && !name.equals("")) {
	    append = "(\"" + this.getName() + "\")";
	}
	StringBuilder bldr = new StringBuilder();
	bldr.append("TAG_List" + append + ": " + value.size() + " entries of type " + NBTUtils.getTypeName(type) + "\r\n{\r\n");
	for (Tag t : value) {
	    bldr.append("   " + t.toString().replaceAll("\r\n", "\r\n   ") + "\r\n");
	}
	bldr.append("}");
	return bldr.toString();
    }

}