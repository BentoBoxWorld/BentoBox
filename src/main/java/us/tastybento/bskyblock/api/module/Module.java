package us.tastybento.bskyblock.api.module;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import us.tastybento.bskyblock.api.module.command.CommandManager;
import us.tastybento.bskyblock.module.command.ModuleCommand;
import us.tastybento.bskyblock.module.event.ListenerManager;
import us.tastybento.bskyblock.module.event.ModuleListener;

public abstract class Module{

    private ListenerManager listenerManager;
    private boolean enabled;
    private ModuleDescription description;
    private List<ModuleCommand> registeredCommands;
    
    public Module(){
        this.enabled = false;
        this.listenerManager = new ListenerManager();
        this.registeredCommands = new ArrayList<>();
    }
    
    public abstract void Enable();
    public abstract void Disable();
    public abstract void Load();

    public void registerListener(ModuleListener listener){
        this.listenerManager.registerListener(listener);
    }
    
    public void registerCommand(ModuleCommand command){
        CommandManager.registerCmd(command);
        this.registeredCommands.add(command);
    }
    
    public JavaPlugin getUsedPlugin(){
        return Main.getInstance();
    }
    
    public ListenerManager getListenerManager() {
        return listenerManager;
    }

    public void setListenerManager(ListenerManager listenerManager) {
        this.listenerManager = listenerManager;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ModuleDescription getDescription() {
        return description;
    }
    
    public void setDescription(ModuleDescription desc){
        this.description = desc;
    }

    public List<ModuleCommand> getRegisteredCommands() {
        return registeredCommands;
    }

    public void setRegisteredCommands(List<ModuleCommand> registeredCommands) {
        this.registeredCommands = registeredCommands;
    }
    
    
}