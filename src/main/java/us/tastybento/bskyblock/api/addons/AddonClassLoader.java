package us.tastybento.bskyblock.api.addons;

import java.io.BufferedReader;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

import us.tastybento.bskyblock.api.addons.AddonDescription.AddonDescriptionBuilder;
import us.tastybento.bskyblock.api.addons.exception.InvalidAddonFormatException;
import us.tastybento.bskyblock.api.addons.exception.InvalidAddonInheritException;

/**
 * @author Tastybento, ComminQ
 */
public class AddonClassLoader extends URLClassLoader {

	public Addon addon;
	
	public AddonClassLoader(Map<String, String>data, File path, BufferedReader reader, ClassLoader loaders) throws InvalidAddonInheritException, MalformedURLException, InvalidAddonFormatException {
		super(new URL[]{path.toURI().toURL()}, loaders);
		
		Addon addon = null;
		
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
			e.printStackTrace();
		}
		
		Class<? extends Addon> addonClass;
		try{
			addonClass = javaClass.asSubclass(Addon.class);
		}catch(ClassCastException e){
			throw new InvalidAddonInheritException("Main class doesn't not extends super class 'Addon'");
		}
		
		try {
			addon = addonClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}

		addon.setDescription(this.asDescription(data));
		
		this.addon = addon;
	}
	
	private AddonDescription asDescription(Map<String, String> data){
		String[] authors = data.get("authors").split("\\,");
		
		return new AddonDescriptionBuilder(data.get("name"))
		.withVersion(data.get("version"))
		.withAuthor(authors).build();
	}

}
