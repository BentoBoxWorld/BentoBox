package us.tastybento.bskyblock.api.addons;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.plugin.InvalidDescriptionException;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.addons.AddonDescription.AddonDescriptionBuilder;
import us.tastybento.bskyblock.api.addons.exception.InvalidAddonFormatException;
import us.tastybento.bskyblock.api.addons.exception.InvalidAddonInheritException;
import us.tastybento.bskyblock.managers.AddonsManager;

/**
 * @author Tastybento, ComminQ
 */
public class AddonClassLoader extends URLClassLoader {

    private final Map<String, Class<?>> classes = new HashMap<String, Class<?>>();
	private Addon addon;
    private AddonsManager loader;
	
	public AddonClassLoader(AddonsManager addonsManager, Map<String, String>data, File path, ClassLoader parent) 
	        throws InvalidAddonInheritException, 
	        MalformedURLException, 
	        InvalidAddonFormatException, 
	        InvalidDescriptionException, 
	        InstantiationException, 
	        IllegalAccessException {
		super(new URL[]{path.toURI().toURL()}, parent);
		
		this.loader = addonsManager;
				
		Class<?> javaClass = null;
		try {
		    //Bukkit.getLogger().info("data " + data.get("main"));
		    /*
		    for (Entry<String, String> en : data.entrySet()) {
		        Bukkit.getLogger().info(en.getKey() + " => " + en.getValue());
		    }*/
			javaClass = Class.forName(data.get("main"), true, this);
			if(data.get("main").contains("us.tastybento")){
				throw new InvalidAddonFormatException("Packages declaration cannot start with 'us.tastybento'");
			}
		} catch (ClassNotFoundException e) {
			BSkyBlock.getInstance().getLogger().severe("Could not load '" + path.getName() + "' in folder '" + path.getParent() + "'");
			throw new InvalidDescriptionException("Invalid addon.yml");
		}
		
		Class<? extends Addon> addonClass;
		try{
			addonClass = javaClass.asSubclass(Addon.class);
		} catch(ClassCastException e){
			throw new InvalidAddonInheritException("Main class doesn't not extends super class 'Addon'");
		}
		
		Addon addon = addonClass.newInstance();

		addon.setDescription(this.asDescription(data));
		
		this.addon = addon;
	}
	
	private AddonDescription asDescription(Map<String, String> data){
		String[] authors = data.get("authors").split("\\,");
		
		return new AddonDescriptionBuilder(data.get("name"))
		.withVersion(data.get("version"))
		.withAuthor(authors).build();
	}

	
    /* (non-Javadoc)
     * @see java.net.URLClassLoader#findClass(java.lang.String)
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return findClass(name, true);
    }

    /**
     * This is a custom findClass that enables classes in other addons to be found
     * (This code was copied from Bukkit's PluginLoader class
     * @param name
     * @param checkGlobal
     * @return Class
     * @throws ClassNotFoundException
     */
    public Class<?> findClass(String name, boolean checkGlobal) throws ClassNotFoundException {
        if (name.startsWith("us.tastybento.")) {
            throw new ClassNotFoundException(name);
        }
        Class<?> result = classes.get(name);

        if (result == null) {
            if (checkGlobal) {
                result = loader.getClassByName(name);
            }

            if (result == null) {
                result = super.findClass(name);

                if (result != null) {
                    loader.setClass(name, result);
                }
            }

            classes.put(name, result);
        }

        return result;
    }

    /**
     * @return the addon
     */
    public Addon getAddon() {
        return addon;
    }
    
}
