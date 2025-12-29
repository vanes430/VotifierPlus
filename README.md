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
# +-------------------------------------------------------------------------+
# |                      VotifierPlus Configuration                         |
# +-------------------------------------------------------------------------+

# The IP address to listen on. 0.0.0.0 listens on all available interfaces.
Host: 0.0.0.0

# The port to listen on. Default is 8192.
# Make sure this port is open in your firewall (TCP).
Port: 8192

# Debug levels: NONE, INFO, EXTRA.
# INFO will show basic vote logging, EXTRA will show data packets.
DebugLevel: NONE

# Experimental: Enable V2 Token support (NuVotifier compatible).
TokenSupport: false

# Tokens for V2 authentication.
Tokens:
  default: 'YOUR_GENERATED_TOKEN'

# Automatically clear the offline waiting list every X seconds.
# Default: 7200 (2 hours). Set to 0 to disable.
AutoClearDelay: 7200

# Vote Forwarding: Send received votes to other servers.
Forwarding:
  server1:
    # Whether forwarding to this server is enabled.
    Enabled: false
    # The IP address of the target server.
    Host: '127.0.0.1'
    # The port of the target server's VotifierPlus.
    Port: 8193
    # RSA Public Key of the target server (required if UseToken is false).
    Key: ''
    # Token used for V2 authentication.
    # Leave empty to generate a new token automatically.
    Token: ''
    # Use V2 Token authentication instead of RSA keys.
    UseToken: false
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

# The IP address to listen on. 0.0.0.0 listens on all interfaces.
Host: 0.0.0.0

# The port to listen on. Default is 8192.
Port: 8192

# Enable debug logging for troubleshooting.
Debug: false

# Experimental: Enable V2 Token support (NuVotifier compatible).
TokenSupport: false

# Tokens for V2 authentication.
Tokens:
  default: 'YOUR_GENERATED_TOKEN'

# Vote Forwarding: Send received votes by the proxy to your game servers.
Forwarding:
  server1:
    # Address of the target server (host:port).
    Address: 127.0.0.1:8193
    # RSA Public Key of the target server.
    Key: ''
    # Token for V2 authentication (if UseToken is true).
    Token: ''
    # Use V2 Token authentication instead of RSA keys.
    UseToken: false
    # Whether forwarding to this server is enabled.
    Enabled: false
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

# The IP address to listen on. 0.0.0.0 listens on all interfaces.
Host: 0.0.0.0

# The port to listen on. Default is 8192.
Port: 8192

# Enable debug logging for troubleshooting.
Debug: false

# Experimental: Enable V2 Token support (NuVotifier compatible).
TokenSupport: false

# Tokens for V2 authentication.
Tokens:
  default: 'YOUR_GENERATED_TOKEN'

# Vote Forwarding: Send votes received by this application to other servers.
Forwarding:
  lobby:
    # Address of the target server (host:port).
    Address: 127.0.0.1:8193
    # RSA Public Key of the target server.
    Key: ''
    # Token for V2 authentication (if UseToken is true).
    Token: ''
    # Use V2 Token authentication instead of RSA keys.
    UseToken: false
    # Whether forwarding to this server is enabled.
    Enabled: false
```

---

## 🛠️ Technical Details

### RSA Encryption
All versions generate a pair of RSA keys in the `rsa/` folder.
*   **public.key**: Shared with voting sites or proxy configs.
*   **private.key**: **KEEP THIS SECRET!** It is used to decrypt your votes.

### V2 Token Authentication
VotifierPlus supports an alternative HMAC-SHA256 token-based authentication (NuVotifier V2 compatible). This is often easier to configure than RSA keys for internal forwarding and provides better performance.

### Configuration Standard
All configuration files now use **PascalCase** for keys (e.g., `Host`, `Port`, `TokenSupport`) to ensure a consistent and clean structure across all platforms.

### Compiling from Source
Requires JDK 21 and Maven:
```bash
mvn clean install
```
All compiled artifacts (`.jar` files) will be automatically gathered in the root `target/` directory.

---
© BenCodez & VexSoftware. Fork maintained by vanes430. Distributed under the GNU General Public License v3.0.