package us.tastybento.bskyblock.database;

public class Test {
    private int      id;
    private String     name;

    public Test() {}

    public Test(int id, String name) {
        super();
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String toString() {
        final String TAB = "    \n";

        StringBuilder retValue = new StringBuilder();

        retValue.append("Test (\n ")
        .append(super.toString()).append(TAB)
        .append("     id = ").append(this.id).append(TAB)
        .append("     name = ").append(this.name).append(TAB)
        .append(" )");

        return retValue.toString();
    }   
}
