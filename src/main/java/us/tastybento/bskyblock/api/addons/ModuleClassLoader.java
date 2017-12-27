package us.tastybento.bskyblock.api.addons;

import java.io.BufferedReader;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

import us.tastybento.bskyblock.api.module.Module;
import us.tastybento.bskyblock.api.module.ModuleDescription;
import us.tastybento.bskyblock.api.module.ModuleDescription.ModuleDescriptionBuilder;
import us.tastybento.bskyblock.api.module.exception.InvalidModuleFormatException;
import us.tastybento.bskyblock.api.module.exception.InvalidModuleInheritException;

public class ModuleClassLoader extends URLClassLoader{

    public Module module;
    
    public ModuleClassLoader(Map<String, String>data, File path, BufferedReader reader, ClassLoader loaders) throws InvalidModuleInheritException, MalformedURLException, InvalidModuleFormatException {
        super(new URL[]{path.toURI().toURL()}, loaders);
        
        Module module = null;
        
        Class<?> javaClass = null;
        try {
            javaClass = Class.forName(data.get("Main"), true, this);
            if(data.get("Main").contains("net.essence")){
                throw new InvalidModuleFormatException("Packages declaration cannot start with 'net.essence'");
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        
        Class<? extends Module> moduleClass;
        try{
            moduleClass = javaClass.asSubclass(Module.class);
        }catch(ClassCastException e){
            throw new InvalidModuleInheritException("Main class doesn't not extends super class 'Module'");
        }
        
        try {
            module = moduleClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }

        module.setDescription(this.asDescription(data));
        
        this.module = module;
    }
    
    private ModuleDescription asDescription(Map<String, String> data){
        
        String[] authors = data.get("Author").split("\\,");
        
        return new ModuleDescriptionBuilder(data.get("Name"))
        .withVersion(data.get("Version"))
        .withAuthor(authors).build();
    }

}