package world.bentobox.bentobox.api.placeholders.placeholderapi;

import world.bentobox.bentobox.BentoBox;

public class BentoBoxPlaceholderExpansion extends BasicPlaceholderExpansion {
  private BentoBox plugin;

  public BentoBoxPlaceholderExpansion(BentoBox plugin) {
    super();
    this.plugin = plugin;
  }

  @Override
  public String getName() {
    return plugin.getName();
  }

  @Override
  public String getAuthor() {
    return "Tastybento and Poslovitch";
  }

  @Override
  public String getVersion() {
    return plugin.getDescription().getVersion();
  }
}
