# 🗳️ VotifierPlus

VotifierPlus is a powerful, high-performance, and multi-platform fork of the original [BenCodez/VotifierPlus](https://github.com/BenCodez/VotifierPlus). It is designed to receive and process Minecraft server votes from toplists with maximum compatibility and modern security features.

This fork is maintained by **vanes430** and is optimized for modern environments like **Folia** and **Velocity**, while maintaining full support for legacy servers starting from **Minecraft 1.8**.

---

## ✨ Key Features

*   **Ultimate Compatibility:** Supports Java 8 through Java 21+ and Minecraft 1.8 through 1.21.
*   **Multi-Platform:** Native modules for Spigot (Legacy/Modern), Sponge (Legacy/Modern), BungeeCord, Velocity, and a Standalone CLI application.
*   **Modern Security:** Supports standard RSA (V1) and secure HMAC-SHA256 Token-based (V2) protocols.
*   **Dynamic Reloading:** Change ports, hostnames, or keys and reload instantly—**no server restart required**.
*   **Folia Ready:** Native region-based threading support for Folia servers.
*   **Smart Key Management:** Automatically detects missing or empty RSA keys and regenerates them on the fly.
*   **Secure Tokens:** Generates 16-character lowercase hexadecimal tokens using `SecureRandom` for maximum entropy.
*   **Vote Forwarding:** Robust system to forward votes across your entire network.

---

## 🚀 Supported Platforms

| Module | Target Environment | Version / API |
| :--- | :--- | :--- |
| **Spigot-Legacy** | Spigot, Paper, CraftBukkit | Minecraft 1.8 - 1.12.2 |
| **Spigot-Modern** | Spigot, Paper, **Folia** | Minecraft 1.13 - 1.21+ |
| **Sponge-Legacy** | SpongeForge, SpongeVanilla | Sponge API 7 (MC 1.12.2) |
| **Sponge-Modern** | SpongeForge, SpongeVanilla | Sponge API 8+ (MC 1.16.5+) |
| **BungeeCord** | BungeeCord, Waterfall | Latest Proxy API |
| **Velocity** | Velocity Proxy | Latest Velocity API |
| **Standalone** | Linux/Windows/Mac Terminal | Pure Java 8+ |

---

## 📥 Installation

1.  **Download** the correct `.jar` for your platform from the [Releases](https://github.com/vanes430/VotifierPlus/releases) page.
2.  **Place** the file in your server's appropriate directory:
    *   **Spigot/Bungee:** `/plugins/`
    *   **Velocity/Sponge:** `/mods/` or `/plugins/`
    *   **Standalone:** Any dedicated folder.
3.  **Start** your server. VotifierPlus will generate a `config.yml` and an `rsa/` folder containing your public/private keys.
4.  **Configure** your firewall to open the port (default: `8192`) for **TCP** traffic.
5.  **Vote Sites:** Provide your server's IP and the contents of `rsa/public.key` to your voting lists.

---

## 🛠️ Commands & Permissions

### Spigot / Paper / Folia
| Command | Description | Permission |
| :--- | :--- | :--- |
| `/votifierplus reload` | Reloads config, tokens, and restarts listener. | `votifierplus.admin` |
| `/votifierplus test [user] [service]` | Sends a simulated vote. | `votifierplus.admin` |
| `/votifierplus check` | Checks the current offline waiting list. | `votifierplus.admin` |
| `/votifierplus clear` | Wipes the offline waiting list. | `votifierplus.admin` |

### BungeeCord & Velocity
| Platform | Command | Description | Permission |
| :--- | :--- | :--- | :--- |
| **Bungee** | `/buvotifierplus reload` | Full reload of the proxy listener. | `buvotifierplus.admin` |
| **Velocity** | `/vevotifierplus reload` | Full reload of the proxy listener. | `vevotifierplus.admin` |

### Standalone CLI
Available terminal commands:
*   `reload` / `restart`: Reloads config and restarts the socket.
*   `status`: Shows JVM memory and system load.
*   `uptime`: Shows how long the receiver has been running.
*   `stop`: Safely shuts down the application.

---

## ⚙️ Configuration (`config.yml`)

The configuration is designed to be consistent across all platforms.

```yaml
# The IP address to listen on. 0.0.0.0 listens on all available interfaces.
Host: 0.0.0.0

# The port to listen on (Default: 8192). 
# Can be changed dynamically via reload command.
Port: 8192

# Enable V2 Token support (HMAC-SHA256). Required for many modern voting sites.
TokenSupport: false

# Tokens used for V2 authentication.
# Format: 'serviceName: token'
Tokens:
  default: '1a2b3c4d5e6f7g8h' # Generated 16-char hex

# Automatically clear the offline waiting list every X seconds.
# Default: 7200 (2 hours). Set to 0 to disable.
AutoClearDelay: 7200

# Send received votes to other backend servers.
Forwarding:
  survival:
    Enabled: true
    Host: '127.0.0.1'
    Port: 8193
    # Use V2 Token instead of RSA for internal forwarding (Recommended)
    UseToken: true
    Token: 'internal_secret_token'
    # RSA Key (Only needed if UseToken is false)
    Key: 'MIIBIjANBgkqhki...' 
```

---

## 🔒 Security Deep Dive

### V1 Protocol (RSA)
Uses a **2048-bit RSA** key pair. The server list encrypts the vote with your **public key**, and VotifierPlus decrypts it with your **private key**. 
*   **Pros:** Standardized, widely supported.
*   **Cons:** Higher CPU overhead, fixed block size.

### V2 Protocol (Tokens)
Uses **HMAC-SHA256** with a shared secret token. 
*   **Pros:** Much faster, supports larger data payloads, highly secure against replay attacks via a unique challenge-response handshake.
*   **Token Format:** Our implementation uses a 16-character lowercase hexadecimal string (e.g., `f3e2d1c0b9a87654`).

### Dynamic Reloading
Unlike older Votifier versions, VotifierPlus can **rebind ports** during runtime. When you run `/votifierplus reload`, the plugin:
1.  Kills the existing socket.
2.  Parses the new hostname/port.
3.  Re-validates the RSA folder.
4.  Regenerates missing keys automatically.
5.  Binds to the new port immediately.

---

## 💻 Developer API

Developers can listen to the `VotifierEvent` to process votes.

**Spigot:**
```java
@EventHandler
public void onVote(com.vexsoftware.votifier.model.VotifierEvent event) {
    Vote vote = event.getVote();
    System.out.println("Received vote from " + vote.getUsername() + " via " + vote.getServiceName());
}
```

**Sponge:**
```java
@Listener
public void onVote(com.vexsoftware.votifier.SpongeVotifier.VotifierEvent event) {
    Vote vote = event.getVote();
}
```

---

## 🛠️ Compiling from Source

Requires **Maven** and **JDK 21** (for the build process, though output is Java 8 compatible).

```bash
git clone https://github.com/vanes430/VotifierPlus.git
cd VotifierPlus
mvn clean install
```
All compiled jars will be gathered in the root `target/` directory.

---

## 📜 Credits & License

*   **Original Author:** BenCodez & VexSoftware.
*   **Fork Maintainer:** [vanes430](https://github.com/vanes430)
*   **License:** Distributed under the **GNU General Public License v3.0**. See `LICENSE` for details.

---
© 2025 VotifierPlus Team. Proudly maintained by vanes430.
