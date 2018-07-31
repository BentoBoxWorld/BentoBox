package world.bentobox.bentobox.api.addons.exception;

import java.util.logging.Level;

import org.bukkit.Bukkit;

public class InvalidAddonFormatException extends AddonException {

    /**
     *
     */
    private static final long serialVersionUID = 7741502900847049986L;

    public InvalidAddonFormatException(String errorMessage) {
        super(errorMessage);
    }

    @Override
    public void printStackTrace(){
        super.printStackTrace();

        Bukkit.getLogger().log(Level.WARNING, "   Basic format : (addon.yml)");
        Bukkit.getLogger().log(Level.WARNING, "   main: path.to.your.MainClass");
        Bukkit.getLogger().log(Level.WARNING, "   name: <NameOfYourModule>");
        Bukkit.getLogger().log(Level.WARNING, "   authors: <AuthorA> | <AuthorA, AuthorB>");
        Bukkit.getLogger().log(Level.WARNING, "   version: YourVersion");
    }
}
