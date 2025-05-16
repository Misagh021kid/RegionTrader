# 🔥 RegionTrader Plugin

`RegionTrader` is a custom Minecraft plugin for Paper/Purpur servers that allows players to sell or buy region with server system.

---

## 📦 Features

- ✅ Sell And Buy House without Any Cheats
- 🏠 Simple `/sellhouse` & `/buyhouse` commands
- 💾 Lightweight and efficient
- 📜 Easy to configure
- 🧩 Compatible with Paper/Purpur 1.20+

---

## 🛠 Installation

1. Drop `RegionTrader-1.0.jar` into your server's `plugins/` directory
2. Restart your server
3. You're done! 🚀

---

## 🧪 Commands

```
/sellhouse <player> <house> <price>       → Send a Request
/buyhouse <player>                        → Accept a Request
```
---

## 📁 plugin.yml

```yaml
name: ReigonTrader
version: '1.0'
main: com.misagh.regiontrader.RegionTrader
api-version: '1.20'
load: POSTWORLD
depend:
  - Vault
  - AdvancedRegionMarket
commands:
  sellhouse:
    description: Send Request to Sell Region
    usage: /sellhouse <region-name> <player>
  buyhouse:
    description: Accept (Buy) The Request To Buy Region
    usage: /buyhouse <region-name> <player>
```

---

## 💬 Support

Found a bug or have a suggestion? Feel free to open an issue or contact me:
- 📧 Email: misaghalivi@gmail.com
- 🧵 Discord: @f35j#0000

---

## 🧠 To-DO

- Add Trade Command /TradeHouse
- Add Tax
- Add Config & Perms
- Add Logger

---

## 📜 License

MIT License – free to use, modify, and share. Just give credit where it's due. 😉
