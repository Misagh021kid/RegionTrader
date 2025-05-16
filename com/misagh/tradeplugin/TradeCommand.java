package com.misagh.tradeplugin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
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

public class TradeCommand implements CommandExecutor {
   private final Economy economy;
   private static final Map<UUID, TradeCommand.PendingTrade> pendingTrades = new HashMap();

   public TradeCommand(Economy economy) {
      this.economy = economy;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player)) {
         sender.sendMessage(ChatColor.RED + "In dastor faghat baraye player hast!");
         return true;
      } else {
         Player player = (Player)sender;
         if (label.equalsIgnoreCase("tradehouse")) {
            return this.handleTradeHouse(player, args);
         } else if (label.equalsIgnoreCase("tradeaccept")) {
            return this.handleTradeAccept(player);
         } else {
            return label.equalsIgnoreCase("tradedecline") ? this.handleTradeDecline(player) : false;
         }
      }
   }

   private boolean handleTradeAccept(Player player) {
      UUID playerId = player.getUniqueId();
      Iterator var3 = pendingTrades.entrySet().iterator();

      Entry entry;
      TradeCommand.PendingTrade trade;
      do {
         if (!var3.hasNext()) {
            player.sendMessage(ChatColor.RED + "Shoma darkhaste tradei nadarid!");
            return true;
         }

         entry = (Entry)var3.next();
         trade = (TradeCommand.PendingTrade)entry.getValue();
      } while(!trade.getTargetPlayerId().equals(playerId));

      Player requester = Bukkit.getPlayer((UUID)entry.getKey());
      if (requester != null && requester.isOnline()) {
         SellRegion requesterRegion = trade.getRequesterRegion();
         SellRegion targetRegion = trade.getTargetRegion();
         double tradePrice = trade.getTradePrice();
         if (!player.getUniqueId().equals(targetRegion.getOwner())) {
            player.sendMessage(ChatColor.RED + "Shoma malek region khod nistid!");
            return true;
         } else {
            double requesterPrice = requesterRegion.getPriceObject().calcPrice(requesterRegion.getRegion());
            double targetPrice = targetRegion.getPriceObject().calcPrice(targetRegion.getRegion());
            if (!(tradePrice < targetPrice / 2.0D) && !(tradePrice > targetPrice)) {
               double priceDifference;
               if (requesterPrice > tradePrice) {
                  priceDifference = requesterPrice - tradePrice;
                  if (!this.economy.has(player, priceDifference)) {
                     player.sendMessage(ChatColor.RED + "Shoma pol kafi baraye anjam in trade nadarid! Mizan: " + priceDifference);
                     return true;
                  }

                  this.economy.withdrawPlayer(player, priceDifference);
                  this.economy.depositPlayer(requester, priceDifference);
               } else {
                  priceDifference = tradePrice - requesterPrice;
                  if (!this.economy.has(requester, priceDifference)) {
                     player.sendMessage(ChatColor.RED + "Player darkhaste konande pol kafi baraye anjam in trade nadarad!");
                     return true;
                  }

                  this.economy.withdrawPlayer(requester, priceDifference);
                  this.economy.depositPlayer(player, priceDifference);
               }

               requesterRegion.setOwner(player);
               targetRegion.setOwner(requester);
               ChatColor var10001 = ChatColor.GREEN;
               player.sendMessage(var10001 + "Shoma region khod ra ba region player " + requester.getName() + " tabadol kardid!");
               var10001 = ChatColor.GREEN;
               requester.sendMessage(var10001 + "Shoma region khod ra ba region player " + player.getName() + " tabadol kardid!");
               pendingTrades.remove(entry.getKey());
               return true;
            } else {
               player.sendMessage(ChatColor.RED + "Gheymat trade bayad beyn " + targetPrice / 2.0D + " va " + targetPrice + " bashad!");
               return true;
            }
         }
      } else {
         player.sendMessage(ChatColor.RED + "Player darkhaste konande online nist!");
         pendingTrades.remove(entry.getKey());
         return true;
      }
   }

   private boolean handleTradeDecline(Player player) {
      UUID playerId = player.getUniqueId();
      Iterator var3 = pendingTrades.entrySet().iterator();

      Entry entry;
      TradeCommand.PendingTrade trade;
      do {
         if (!var3.hasNext()) {
            player.sendMessage(ChatColor.RED + "Shoma darkhaste tradei nadarid!");
            return true;
         }

         entry = (Entry)var3.next();
         trade = (TradeCommand.PendingTrade)entry.getValue();
      } while(!trade.getTargetPlayerId().equals(playerId));

      Player requester = Bukkit.getPlayer((UUID)entry.getKey());
      if (requester != null && requester.isOnline()) {
         ChatColor var10001 = ChatColor.RED;
         requester.sendMessage(var10001 + "Player " + player.getName() + " darkhaste trade shoma ra rad kard.");
      }

      player.sendMessage(ChatColor.RED + "Shoma darkhaste trade ra rad kardid.");
      pendingTrades.remove(entry.getKey());
      return true;
   }

   private boolean handleTradeHouse(final Player player, String[] args) {
      if (args.length != 4) {
         player.sendMessage(ChatColor.RED + "Estefade: /tradehouse <player> <esm region shoma> <esm region target> <gheymat>");
         return true;
      } else {
         final Player targetPlayer = Bukkit.getPlayer(args[0]);
         String requesterRegionName = args[1];
         String targetRegionName = args[2];

         double tradePrice;
         try {
            tradePrice = Double.parseDouble(args[3]);
         } catch (NumberFormatException var12) {
            player.sendMessage(ChatColor.RED + "Gheymat vared shode eshtebah ast. Lotfan adad sahih vared konid.");
            return true;
         }

         if (targetPlayer != null && targetPlayer.isOnline()) {
            SellRegion requesterRegion = this.getSellRegion(player, requesterRegionName);
            SellRegion targetRegion = this.getSellRegion(targetPlayer, targetRegionName);
            if (requesterRegion != null && targetRegion != null) {
               double targetRegionPrice = targetRegion.getPriceObject().calcPrice(targetRegion.getRegion());
               if (!(tradePrice < targetRegionPrice / 2.0D) && !(tradePrice > targetRegionPrice)) {
                  pendingTrades.put(player.getUniqueId(), new TradeCommand.PendingTrade(requesterRegion, targetRegion, targetPlayer.getUniqueId(), tradePrice));
                  (new BukkitRunnable() {
                     public void run() {
                        if (TradeCommand.pendingTrades.containsKey(player.getUniqueId())) {
                           TradeCommand.pendingTrades.remove(player.getUniqueId());
                           player.sendMessage(ChatColor.RED + "Trade shoma montaghi shod.");
                           ChatColor var10001 = ChatColor.RED;
                           targetPlayer.sendMessage(var10001 + "Trade az player " + player.getName() + " montaghi shod.");
                        }

                     }
                  }).runTaskLater(TradePlugin.getInstance(), 1200L);
                  ChatColor var10001 = ChatColor.GREEN;
                  player.sendMessage(var10001 + "Shoma darkhaste trade region '" + requesterRegionName + "' ba region player " + targetPlayer.getName() + " be esm '" + targetRegionName + "' ra ersal kardid! Gheymat: " + tradePrice);
                  var10001 = ChatColor.YELLOW;
                  targetPlayer.sendMessage(var10001 + "Player " + player.getName() + " darkhaste trade region '" + requesterRegionName + "' ba region shoma be esm '" + targetRegionName + "' ra dade ast! Gheymat: " + tradePrice);
                  return true;
               } else {
                  player.sendMessage(ChatColor.RED + "Gheymat bayad beyn " + targetRegionPrice / 2.0D + " va " + targetRegionPrice + " bashad!");
                  return true;
               }
            } else {
               return true;
            }
         } else {
            player.sendMessage(ChatColor.RED + "Player mored nazar online nist ya peyda nashod!");
            return true;
         }
      }
   }

   private SellRegion getSellRegion(Player player, String regionName) {
      Region region = AdvancedRegionMarket.getInstance().getRegionManager().getRegionByNameAndWorld(regionName, player.getWorld().getName());
      if (region == null) {
         player.sendMessage(ChatColor.RED + "Region " + regionName + " peyda nashod!");
         return null;
      } else if (!(region instanceof SellRegion)) {
         player.sendMessage(ChatColor.RED + "Region " + regionName + " baraye trade mojaz nist!");
         return null;
      } else {
         SellRegion sellRegion = (SellRegion)region;
         if (!player.getUniqueId().equals(sellRegion.getOwner())) {
            player.sendMessage(ChatColor.RED + "Shoma malek region " + regionName + " nistid!");
            return null;
         } else {
            return sellRegion;
         }
      }
   }

   private static class PendingTrade {
      private final SellRegion requesterRegion;
      private final SellRegion targetRegion;
      private final UUID targetPlayerId;
      private final double tradePrice;
      private final long expirationTime;

      public PendingTrade(SellRegion requesterRegion, SellRegion targetRegion, UUID targetPlayerId, double tradePrice) {
         this.requesterRegion = requesterRegion;
         this.targetRegion = targetRegion;
         this.targetPlayerId = targetPlayerId;
         this.tradePrice = tradePrice;
         this.expirationTime = System.currentTimeMillis() + 60000L;
      }

      public SellRegion getRequesterRegion() {
         return this.requesterRegion;
      }

      public SellRegion getTargetRegion() {
         return this.targetRegion;
      }

      public UUID getTargetPlayerId() {
         return this.targetPlayerId;
      }

      public double getTradePrice() {
         return this.tradePrice;
      }

      public boolean isExpired() {
         return System.currentTimeMillis() > this.expirationTime;
      }
   }
}
