package us.tastybento.bskyblock.api.addons;

import java.io.BufferedReader;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

import us.tastybento.bskyblock.api.addons.AddOnDescription.AddonDescriptionBuilder;
import us.tastybento.bskyblock.api.addons.exception.InvalidAddOnFormatException;
import us.tastybento.bskyblock.api.addons.exception.InvalidAddOnInheritException;

public class AddOnClassLoader extends URLClassLoader{

	public AddOn addon;
	
	public AddOnClassLoader(Map<String, String>data, File path, BufferedReader reader, ClassLoader loaders) throws InvalidAddOnInheritException, MalformedURLException, InvalidAddOnFormatException {
		super(new URL[]{path.toURI().toURL()}, loaders);
		
		AddOn addon = null;
		
		Class<?> javaClass = null;
		try {
		    //Bukkit.getLogger().info("data " + data.get("main"));
		    /*
		    for (Entry<String, String> en : data.entrySet()) {
		        Bukkit.getLogger().info(en.getKey() + " => " + en.getValue());
		    }*/
			javaClass = Class.forName(data.get("main"), true, this);
			if(data.get("main").contains("us.tastybento")){
				throw new InvalidAddOnFormatException("Packages declaration cannot start with 'us.tastybento'");
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		Class<? extends AddOn> addonClass;
		try{
			addonClass = javaClass.asSubclass(AddOn.class);
		}catch(ClassCastException e){
			throw new InvalidAddOnInheritException("Main class doesn't not extends super class 'AddOn'");
		}
		
		try {
			addon = addonClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}

		addon.setDescription(this.asDescription(data));
		
		this.addon = addon;
	}
	
	private AddOnDescription asDescription(Map<String, String> data){
		
		String[] authors = data.get("authors").split("\\,");
		
		return new AddonDescriptionBuilder(data.get("name"))
		.withVersion(data.get("version"))
		.withAuthor(authors).build();
	}

}
