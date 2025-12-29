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

### Bukkit Configuration (`config.yml`)

The configuration is automatically generated with detailed comments. Key settings include:

```yaml
# IP to listen on. 0.0.0.0 listens on all interfaces.
Host: 0.0.0.0

# Port to listen on (default 8192). Open this in your firewall.
Port: 8192

# Logging: NONE, INFO (basic), EXTRA (detailed packets).
DebugLevel: NONE

# Experimental: Enable V2 Token support.
TokenSupport: false

# Tokens for V2 authentication.
Tokens:
  default: 'YOUR_GENERATED_TOKEN'

# Seconds to keep votes for offline players before auto-clearing.
AutoClearDelay: 7200

# Vote Forwarding: Send votes to other servers.
Forwarding:
  server1:
    Enabled: false
    Host: '127.0.0.1'
    Port: 8193
    # RSA Public Key of the target server.
    Key: 'RSA_PUBLIC_KEY'
    # Use V2 Token authentication instead of RSA.
    UseToken: false
    Token: 'TOKEN_HERE'
```

---

## 🔵 BungeeCord & Velocity

### Commands & Permissions
| Platform | Command | Description | Permission |
| :--- | :--- | :--- | :--- |
| **BungeeCord** | `/buvotifierplus reload` | Reloads Bungee configuration. | `buvotifierplus.admin` |
| **Velocity** | `/vevotifierplus reload` | Reloads Velocity configuration. | `vevotifierplus.admin` |

### Proxy Configuration (`config.yml`)
```yaml
# +-------------------------------------------------------------------------+
# |                    VotifierPlus Proxy Configuration                     |
# +-------------------------------------------------------------------------+

Host: 0.0.0.0
Port: 8192
Debug: false
TokenSupport: false

# Tokens for V2 authentication.
Tokens:
  default: 'YOUR_GENERATED_TOKEN'

# Vote Forwarding: Send votes received by the proxy to your game servers.
Forwarding:
  survival:
    Address: 127.0.0.1:8193
    Enabled: true
    # --- RSA Method ---
    # Put the public.key of the target (survival) server here.
    Key: 'RSA_PUBLIC_KEY'
    # --- Token Method (V2) ---
    # Set UseToken to true and provide the token from the target server.
    Token: 'TOKEN_HERE'
    UseToken: false
```

---

## ⬛ Standalone Application

Use this if you want to run VotifierPlus as a separate process without any Minecraft server software.

### Installation & Commands
1.  Download `VotifierPlus-Standalone.jar`.
2.  Run: `java -jar VotifierPlus-Standalone.jar`.
3.  Available Console Commands: `uptime`, `status`, `stop`.

### Standalone Configuration (`config.yml`)
```yaml
# +-------------------------------------------------------------------------+
# |                  VotifierPlus Standalone Configuration                  |
# +-------------------------------------------------------------------------+

Host: 0.0.0.0
Port: 8192
Debug: false
TokenSupport: false

# Tokens for V2 authentication.
Tokens:
  default: 'YOUR_GENERATED_TOKEN'

Forwarding:
  lobby:
    Address: 127.0.0.1:8193
    Enabled: true
    Key: 'RSA_PUBLIC_KEY'
    Token: 'TOKEN_HERE'
    UseToken: false
```

---

## 🛠️ Technical Details

### RSA Encryption
All versions generate a pair of RSA keys in the `rsa/` folder.
*   **public.key**: Shared with voting sites or proxy configs.
*   **private.key**: **KEEP THIS SECRET!** It is used to decrypt your votes.

### V2 Token Authentication
VotifierPlus supports an alternative HMAC-SHA256 token-based authentication. This is often easier to configure than RSA keys for internal forwarding.

### Compiling from Source
Requires JDK 21 and Maven:
```bash
mvn clean install
```
All compiled artifacts (`.jar` files) will be automatically gathered in the root `target/` directory.

---
© BenCodez & VexSoftware. Fork maintained by vanes430. Distributed under the GNU General Public License v3.0.