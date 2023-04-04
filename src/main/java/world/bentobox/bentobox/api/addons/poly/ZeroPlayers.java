package world.bentobox.bentobox.api.addons.poly;


public class ZeroPlayers implements PlayersPerServer {
        @Override
        public String getRange(int players) {
            return "0";
        }
    }
