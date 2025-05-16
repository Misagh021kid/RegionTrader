package com.misagh.regiontrader;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Advanced Region Market API Imports
 */
import net.alex9849.arm.AdvancedRegionMarket;
import net.alex9849.arm.regions.Region;
import net.alex9849.arm.regions.SellRegion;
/**
 * Vault API imports
 */
import net.milkbowl.vault.economy.Economy;
/**
 * Bukkit API imports
 */
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Handles the /sellhouse and /buyhouse commands.
 */
public class HouseCommand implements CommandExecutor {
   private final Economy economy;
   private static final Map<UUID, PendingSale> pendingSales = new HashMap<>();

   public HouseCommand(Economy economy) {
      this.economy = economy;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player)) {
         sender.sendMessage(ChatColor.RED + "Only players can use this command.");
         return true;
      }

      Player player = (Player) sender;
      if (label.equalsIgnoreCase("sellhouse")) {
         return handleSellHouse(player, args);
      } else if (label.equalsIgnoreCase("buyhouse")) {
         return handleBuyHouse(player, args);
      }
      return false;
   }

   /**
    * Handles The SellHouse Command.
    */
   private boolean handleSellHouse(final Player player, String[] args) {
      if (args.length != 3) {
         player.sendMessage(ChatColor.RED + "Usage: /sellhouse <player> <regionName> <price>");
         return true;
      }

      final Player targetPlayer = Bukkit.getPlayer(args[0]);
      final String regionName = args[1];
      double sellPrice;

      try {
         sellPrice = Double.parseDouble(args[2]);
      } catch (NumberFormatException e) {
         player.sendMessage(ChatColor.RED + "Price must be a valid number.");
         return true;
      }

      if (targetPlayer == null || !targetPlayer.isOnline()) {
         player.sendMessage(ChatColor.RED + "Target player is not online or not found.");
         return true;
      }

      Region region = AdvancedRegionMarket.getInstance().getRegionManager().getRegionByNameAndWorld(regionName, player.getWorld().getName());
      if (region == null) {
         player.sendMessage(ChatColor.RED + "Specified region not found.");
         return true;
      }

      if (!(region instanceof SellRegion)) {
         player.sendMessage(ChatColor.RED + "This region is not a sellable region.");
         return true;
      }

      SellRegion sellRegion = (SellRegion) region;
      if (!player.getUniqueId().equals(sellRegion.getOwner())) {
         player.sendMessage(ChatColor.RED + "You are not the owner of this region.");
         return true;
      }

      double originalPrice = sellRegion.getPriceObject().calcPrice(sellRegion.getRegion());
      double minPrice = originalPrice / 2.0;

      if (sellPrice < minPrice || sellPrice > originalPrice) {
         player.sendMessage(ChatColor.RED + "Price Should be between " + minPrice + " and " + originalPrice + ".");
         return true;
      }

      pendingSales.put(player.getUniqueId(), new PendingSale(sellRegion, targetPlayer.getUniqueId(), sellPrice));
      new BukkitRunnable() {
         @Override
         public void run() {
            PendingSale pending = pendingSales.get(player.getUniqueId());
            if (pending != null && pending.isExpired()) {
               pendingSales.remove(player.getUniqueId());
               player.sendMessage(ChatColor.RED + "Your region sale offer for '" + regionName + "' has expired.");
               targetPlayer.sendMessage(ChatColor.RED + "The offer from " + player.getName() + " for region '" + regionName + "' has expired.");
            }
         }
      }.runTaskLater(RegionTrader.getInstance(), 1200L);

      player.sendMessage(ChatColor.GREEN + "You have offered region '" + regionName + "' to " + targetPlayer.getName() + " for $" + sellPrice);
      targetPlayer.sendMessage(ChatColor.YELLOW + player.getName() + " has offered to sell region '" + regionName + "' to you for $" + sellPrice);
      return true;
   }

   /**
    * Handles The BuyHouse Command.
    */
   private boolean handleBuyHouse(Player player, String[] args) {
      if (args.length != 1) {
         player.sendMessage(ChatColor.RED + "Usage: /buyhouse <seller>");
         return true;
      }

      Player seller = Bukkit.getPlayer(args[0]);
      if (seller == null || !seller.isOnline()) {
         player.sendMessage(ChatColor.RED + "Seller is not online or not found.");
         return true;
      }

      PendingSale pendingSale = pendingSales.get(seller.getUniqueId());
      if (pendingSale == null || pendingSale.isExpired()) {
         pendingSales.remove(seller.getUniqueId());
         player.sendMessage(ChatColor.RED + "There is no active sale offer from this player.");
         return true;
      }

      if (!pendingSale.getBuyerId().equals(player.getUniqueId())) {
         player.sendMessage(ChatColor.RED + "This sale offer was not made to you.");
         return true;
      }

      SellRegion sellRegion = pendingSale.getSellRegion();
      double sellPrice = pendingSale.getPrice();

      if (!economy.has(player, sellPrice)) {
         player.sendMessage(ChatColor.RED + "You do not have enough money to buy this region. Price: $" + sellPrice);
         return true;
      }

      economy.withdrawPlayer(player, sellPrice);
      economy.depositPlayer(seller, sellPrice);
      sellRegion.setOwner(Bukkit.getOfflinePlayer(player.getUniqueId()));
      pendingSales.remove(seller.getUniqueId());

      player.sendMessage(ChatColor.GREEN + "You have successfully purchased region '" + sellRegion.getRegion().getId() + "' from " + seller.getName() + " for $" + sellPrice);
      seller.sendMessage(ChatColor.GREEN + "You have sold your region to " + player.getName() + " for $" + sellPrice);
      return true;
   }

   /**
    * Represents a Pending Region Sale.
    */
   private static class PendingSale {
      private final SellRegion sellRegion;
      private final UUID buyerId;
      private final double price;
      private final long expirationTime;

      public PendingSale(SellRegion sellRegion, UUID buyerId, double price) {
         this.sellRegion = sellRegion;
         this.buyerId = buyerId;
         this.price = price;
         this.expirationTime = System.currentTimeMillis() + 60000L;
      }

      public SellRegion getSellRegion() {
         return sellRegion;
      }

      public UUID getBuyerId() {
         return buyerId;
      }

      public double getPrice() {
         return price;
      }

      public boolean isExpired() {
         return System.currentTimeMillis() > expirationTime;
      }
   }
}