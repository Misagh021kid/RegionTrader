package com.misagh.tradeplugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.alex9849.arm.AdvancedRegionMarket;
import net.alex9849.arm.regions.Region;
import net.alex9849.arm.regions.SellRegion;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class HouseCommand implements CommandExecutor {
   private final Economy economy;
   private static final Map<UUID, HouseCommand.PendingSale> pendingSales = new HashMap();
   private final Map<UUID, Long> playerCooldowns = new HashMap();

   public HouseCommand(Economy economy) {
      this.economy = economy;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player)) {
         sender.sendMessage(ChatColor.RED + "In dastor faghat baraye player hast!");
         return true;
      } else {
         Player player = (Player)sender;
         if (label.equalsIgnoreCase("sellhouse")) {
            return this.handleSellHouse(player, args);
         } else {
            return label.equalsIgnoreCase("buyhouse") ? this.handleBuyHouse(player, args) : false;
         }
      }
   }

   private boolean handleSellHouse(final Player player, String[] args) {
      if (args.length != 3) {
         player.sendMessage(ChatColor.RED + "Estefade: /sellhouse <player> <esm region> <geymat>");
         return true;
      } else {
         final Player targetPlayer = Bukkit.getPlayer(args[0]);
         final String regionName = args[1];

         double sellPrice;
         try {
            sellPrice = Double.parseDouble(args[2]);
         } catch (NumberFormatException var15) {
            player.sendMessage(ChatColor.RED + "Gheymat bayad adad bashe!");
            return true;
         }

         if (targetPlayer != null && targetPlayer.isOnline()) {
            Region region = AdvancedRegionMarket.getInstance().getRegionManager().getRegionByNameAndWorld(regionName, player.getWorld().getName());
            if (region == null) {
               player.sendMessage(ChatColor.RED + "Region mored nazar peyda nashod!");
               return true;
            } else if (!(region instanceof SellRegion)) {
               player.sendMessage(ChatColor.RED + "In region baraye foroosh ghabele estefade nist!");
               return true;
            } else {
               SellRegion sellRegion = (SellRegion)region;
               if (!player.getUniqueId().equals(sellRegion.getOwner())) {
                  player.sendMessage(ChatColor.RED + "Shoma malek in region nistid!");
                  return true;
               } else {
                  double originalPrice = sellRegion.getPriceObject().calcPrice(sellRegion.getRegion());
                  double minPrice = originalPrice / 2.0D;
                  if (!(sellPrice < minPrice) && !(sellPrice > originalPrice)) {
                     pendingSales.put(player.getUniqueId(), new HouseCommand.PendingSale(sellRegion, targetPlayer.getUniqueId(), sellPrice));
                     Bukkit.getLogger().info("PendingSales after registration: " + pendingSales.toString());
                     (new BukkitRunnable() {
                        public void run() {
                           if (HouseCommand.pendingSales.containsKey(player.getUniqueId()) && ((HouseCommand.PendingSale)HouseCommand.pendingSales.get(player.getUniqueId())).isExpired()) {
                              HouseCommand.pendingSales.remove(player.getUniqueId());
                              player.sendMessage(ChatColor.RED + "Pishnahad foroosh region '" + regionName + "' shoma montaghi shod.");
                              ChatColor var10001 = ChatColor.RED;
                              targetPlayer.sendMessage(var10001 + "Pishnahad foroosh region '" + regionName + "' az player " + player.getName() + " montaghi shod.");
                           }

                           Bukkit.getLogger().info("PendingSales after task removal: " + HouseCommand.pendingSales.toString());
                        }
                     }).runTaskLater(TradePlugin.getInstance(), 1200L);
                     ChatColor var10001 = ChatColor.GREEN;
                     player.sendMessage(var10001 + "Shoma pishnahad foroosh region '" + regionName + "' ro be player " + targetPlayer.getName() + " sabt kardid! Gheymat: " + sellPrice);
                     var10001 = ChatColor.YELLOW;
                     targetPlayer.sendMessage(var10001 + "Player " + player.getName() + " pishnahad foroosh region '" + regionName + "' ro be shoma dade ast! Gheymat: " + sellPrice);
                     return true;
                  } else {
                     player.sendMessage(ChatColor.RED + "Gheymat bayad beyn " + minPrice + " va " + originalPrice + " bashe!");
                     return true;
                  }
               }
            }
         } else {
            player.sendMessage(ChatColor.RED + "Player mored nazar online nist ya peyda nashod!");
            return true;
         }
      }
   }

   private boolean handleBuyHouse(Player player, String[] args) {
      if (args.length != 1) {
         player.sendMessage(ChatColor.RED + "Estefade: /buyhouse <player>");
         return true;
      } else {
         Player seller = Bukkit.getPlayer(args[0]);
         if (seller != null && seller.isOnline()) {
            Bukkit.getLogger().info("PendingSales Map: " + pendingSales.toString());
            Bukkit.getLogger().info("Looking for seller UUID: " + seller.getUniqueId());
            if (!pendingSales.containsKey(seller.getUniqueId())) {
               Bukkit.getLogger().info("Seller UUID not found in pendingSales: " + seller.getUniqueId());
               player.sendMessage(ChatColor.RED + "In player hich regioni baraye foroosh sabt nakarde ast!");
               return true;
            } else {
               HouseCommand.PendingSale pendingSale = (HouseCommand.PendingSale)pendingSales.get(seller.getUniqueId());
               if (pendingSale.isExpired()) {
                  pendingSales.remove(seller.getUniqueId());
                  player.sendMessage(ChatColor.RED + "In pishnahad foroosh montaghi shode ast!");
                  return true;
               } else if (!pendingSale.getBuyerId().equals(player.getUniqueId())) {
                  player.sendMessage(ChatColor.RED + "In region be player digar pishnahad shode ast!");
                  return true;
               } else {
                  SellRegion sellRegion = pendingSale.getSellRegion();
                  double sellPrice = pendingSale.getPrice();
                  if (!this.economy.has(player, sellPrice)) {
                     player.sendMessage(ChatColor.RED + "Shoma pol kafi baraye kharid in region nadarin! Gheymat: " + sellPrice);
                     return true;
                  } else {
                     this.economy.withdrawPlayer(player, sellPrice);
                     this.economy.depositPlayer(seller, sellPrice);
                     sellRegion.setOwner(Bukkit.getOfflinePlayer(player.getUniqueId()));
                     pendingSales.remove(seller.getUniqueId());
                     ChatColor var10001 = ChatColor.GREEN;
                     player.sendMessage(var10001 + "Shoma region '" + sellRegion.getRegion().getId() + "' ra ba movafaghiyat az " + seller.getName() + " kharidid! Gheymat: " + sellPrice);
                     var10001 = ChatColor.GREEN;
                     seller.sendMessage(var10001 + "Region shoma be player " + player.getName() + " forookhte shod! Gheymat: " + sellPrice);
                     return true;
                  }
               }
            }
         } else {
            player.sendMessage(ChatColor.RED + "Seller online nist ya peyda nashod!");
            return true;
         }
      }
   }

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
         return this.sellRegion;
      }

      public UUID getBuyerId() {
         return this.buyerId;
      }

      public double getPrice() {
         return this.price;
      }

      public boolean isExpired() {
         return System.currentTimeMillis() > this.expirationTime;
      }
   }
}
