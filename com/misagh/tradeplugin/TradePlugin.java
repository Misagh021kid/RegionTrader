/**
 * ================================
 *      ReginTrader - Minecraft
 * ================================
 *
 * Plugin Name:    RegionTrader
 * Author:        Misagh021Kid (f35j#0000)
 * Version:        1.0
 * Created:        2025-05-16
 *
 * GitHub:         https://github.com/Misagh021kid/RegionTrader
 * Description:
 *      A Minecraft plugin that allows players to buy and sell houses
 *      using Vault economy systems.
 *
 * Dependencies:
 *      - Vault (required)
 *      - Any Economy plugin (e.g., EssentialsX Economy)
 *
 * Commands:
 *      - /sellhouse -> Sell a house to another player
 *      - /buyhouse  -> Buy a house from another player
 *
 *
 */

package com.misagh.tradeplugin;

/**
 * Vault imports for integrating the economy system
 */
import net.milkbowl.vault.economy.Economy;

/**
 * Bukkit API imports
 */
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for TradePlugin
 */
public class TradePlugin extends JavaPlugin {
   /**
    * Static instance of the economy provider
    */
   private static Economy economy;

   /**
    * Static instance of the plugin
    */
   private static TradePlugin instance;

   /**
    * Called when the plugin is enabled
    */
   @Override
   public void onEnable() {
      instance = this;

      if (!setupEconomy()) {
         getLogger().severe("Vault dependency not found! Disabling plugin...");
         getServer().getPluginManager().disablePlugin(this);
         return;
      }

      registerCommand("sellhouse", new HouseCommand(economy));
      registerCommand("buyhouse", new HouseCommand(economy));

      getLogger().info("TradePlugin enabled! (Author: f35j#0000)");
   }

   /**
    * Called when the plugin is disabled
    */
   @Override
   public void onDisable() {
      getLogger().info("TradePlugin disabled. Hope you enjoyed it!");
   }

   /**
    * Gets the plugin instance
    * @return instance of TradePlugin
    */
   public static TradePlugin getInstance() {
      return instance;
   }

   /**
    * Gets the economy provider
    * @return economy instance
    */
   public static Economy getEconomy() {
      return economy;
   }

   /**
    * Initializes The Economy Provider With Vault
    * @return true if economy was successfully set up, false otherwise
    */
   private boolean setupEconomy() {
      if (getServer().getPluginManager().getPlugin("Vault") == null) {
         return false;
      }

      RegisteredServiceProvider<Economy> rsp = getServer()
              .getServicesManager()
              .getRegistration(Economy.class);

      if (rsp == null) {
         return false;
      }

      economy = rsp.getProvider();
      return economy != null;
   }

   /**
    * Registers a Command and Assigns Executor
    * @param name command name
    * @param executor command executor
    */
   private void registerCommand(String name, HouseCommand executor) {
      PluginCommand cmd = getCommand(name);
      if (cmd == null) {
         return;
      }
      cmd.setExecutor(executor);
   }
}
