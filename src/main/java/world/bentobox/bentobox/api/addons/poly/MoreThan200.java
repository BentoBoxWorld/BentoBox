package world.bentobox.bentobox.api.addons.poly;

public  class MoreThan200 implements PlayersPerServer {
    @Override
    public String getRange(int players) {
        return "201+";
    }
}