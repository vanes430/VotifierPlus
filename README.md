# VotifierPlus

VotifierPlus is an enhanced, multi-platform fork of [BenCodez/VotifierPlus](https://github.com/BenCodez/VotifierPlus), designed to receive and process Minecraft server votes from server lists. It supports the standard Votifier protocol, RSA encryption, vote forwarding, and is optimized for modern server environments.

This fork focuses on maintaining native compatibility with **Folia**, **Velocity**, and providing a clean, high-performance build structure.

---

## 📥 Downloads

You can always find the latest builds for all platforms here:
**[Latest Release](https://github.com/vanes430/VotifierPlus/releases/tag/latest)**

---

## 🚀 Platforms Supported

*   **VotifierPlus-Bukkit**: For Spigot, Paper, and **Folia** (Native region-based threading support).
*   **VotifierPlus-BungeeCord**: For BungeeCord and Waterfall proxies.
*   **VotifierPlus-Velocity**: For Velocity proxies.
*   **VotifierPlus-Standalone**: Lightweight Java application for dedicated receivers.

---

## 🟢 Bukkit / Spigot / Paper / Folia

### Installation
1.  Download `VotifierPlus-Bukkit.jar`.
2.  Place it in your `plugins` folder.
3.  Restart the server to generate configurations.
4.  Share `plugins/VotifierPlus/rsa/public.key` with your voting sites.

### Commands & Permissions
| Command | Description | Permission |
| :--- | :--- | :--- |
| `/votifierplus reload` | Reloads config and keys. | `votifierplus.admin` |
| `/votifierplus test [user] [service]` | Simulates a vote. | `votifierplus.admin` |
| `/votifierplus check` | View current waiting list. | `votifierplus.admin` |
| `/votifierplus clear` | Manually clear waiting list. | `votifierplus.admin` |

---

## 🔵 BungeeCord & Velocity

### Commands & Permissions
| Platform | Command | Description | Permission |
| :--- | :--- | :--- | :--- |
| **BungeeCord** | `/buvotifierplus reload` | Reloads Bungee configuration. | `buvotifierplus.admin` |
| **Velocity** | `/vevotifierplus reload` | Reloads Velocity configuration. | `vevotifierplus.admin` |

### Proxy Configuration (`config.yml`)
```yaml
host: 0.0.0.0
port: 8192
debug: false
forwarding:
  survival:
    address: 127.0.0.1:8193
    enabled: true
    # RSA Method: Put the public.key of the survival server here
    key: 'RSA_PUBLIC_KEY'
    # Token Method (V2): Set usetoken to true and provide the token
    token: 'TOKEN_HERE'
    usetoken: false
```

---

## ⬛ Standalone Application

Use this if you want to run VotifierPlus as a separate process without any Minecraft server software.

### Installation
1.  Download `VotifierPlus-Standalone.jar`.
2.  Run it using: `java -jar VotifierPlus-Standalone.jar`.
3.  On the first run, it will generate `config.yml` and `rsa/` keys.
4.  Configure your forwarding targets in `config.yml` and restart the app.

### Console Commands
| Command | Description |
| :--- | :--- |
| `uptime` | Shows how long the application has been running. |
| `status` | Displays JVM memory usage and system load. |
| `stop` | Safely shuts down the application. |

### Standalone Configuration (`config.yml`)
```yaml
host: 0.0.0.0
port: 8192
debug: false
forwarding:
  lobby:
    address: 127.0.0.1:8193
    enabled: true
    key: 'RSA_PUBLIC_KEY'
    token: 'TOKEN_HERE'
    usetoken: false
```
---

## 🛠️ Technical Details

### RSA Encryption
All versions generate a pair of RSA keys in the `rsa/` folder.
*   **public.key**: Shared with voting sites or proxy configs.
*   **private.key**: Keep this file secret!

### Compiling from Source
Requires JDK 21 and Maven:
```bash
mvn clean install
```
All compiled artifacts (`.jar` files) will be automatically gathered in the root `target/` directory.

---
© BenCodez & VexSoftware. Fork maintained by vanes430. Distributed under the GNU General Public License v3.0.
