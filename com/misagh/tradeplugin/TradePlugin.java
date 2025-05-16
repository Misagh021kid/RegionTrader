package com.misagh.tradeplugin;

import java.util.Objects;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class TradePlugin extends JavaPlugin {
   private static Economy economy = null;
   private static TradePlugin instance;

   public void onEnable() {
      instance = this;
      if (!this.setupEconomy()) {
         this.getLogger().severe("Vault پیدا نشد! پلاگین غیرفعال خواهد شد.");
         this.getServer().getPluginManager().disablePlugin(this);
      } else {
         ((PluginCommand)Objects.requireNonNull(this.getCommand("sellhouse"))).setExecutor(new HouseCommand(economy));
         ((PluginCommand)Objects.requireNonNull(this.getCommand("buyhouse"))).setExecutor(new HouseCommand(economy));
         ((PluginCommand)Objects.requireNonNull(this.getCommand("tradehouse"))).setExecutor(new TradeCommand(economy));
         ((PluginCommand)Objects.requireNonNull(this.getCommand("tradeaccept"))).setExecutor(new TradeCommand(economy));
         ((PluginCommand)Objects.requireNonNull(this.getCommand("tradeDecline"))).setExecutor(new TradeCommand(economy));
         this.getLogger().info("پلاگین با موفقیت فعال شد!");
      }
   }

   public void onDisable() {
      this.getLogger().info("پلاگین غیرفعال شد.");
   }

   public static TradePlugin getInstance() {
      return instance;
   }

   private boolean setupEconomy() {
      if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
         return false;
      } else {
         RegisteredServiceProvider<Economy> rsp = this.getServer().getServicesManager().getRegistration(Economy.class);
         if (rsp == null) {
            return false;
         } else {
            economy = (Economy)rsp.getProvider();
            return true;
         }
      }
   }
}
