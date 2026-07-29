# VotifierPlus

[![License: GPLv3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Java](https://img.shields.io/badge/Java-21%2B-orange)](https://adoptium.net/)
[![Paper](https://img.shields.io/badge/Paper-1.21.1%2B-green)](https://papermc.io/)
[![Velocity](https://img.shields.io/badge/Velocity-3.4.0%2B-blueviolet)](https://velocitypowered.com/)

**VotifierPlus** listens for Minecraft server votes and fires Bukkit/Velocity events so other plugins can react. Drop-in replacement for legacy Votifier — no code changes required for existing vote listener plugins.

Fork of [BenCodez/VotifierPlus](https://github.com/BenCodez/VotifierPlus), itself a fork of the original [Votifier](https://github.com/vexsoftware/votifier) by Vex Software LLC.

## Features

- **Dual protocol** — V1 (RSA) and V2 (HMAC token) simultaneously
- **Dual platform** — Paper 1.21.1+ and Velocity 3.4.0+
- **Folia-ready** — all scheduling via `GlobalRegionScheduler`
- **Pending votes** — offline players receive votes on next join
- **Vote throttling** — per-IP rate limiting with tunnel detection
- **Vote forwarding** — relay votes across a multi-server network
- **Backward compatible** — existing plugins work unchanged

## Quick Start

1. Drop `VotifierPlus.jar` into your server's `plugins/` folder
2. Restart the server
3. Configure `plugins/VotifierPlus/config.yml` to your needs
4. Run `/votifierplus reload` to apply changes

### First Run

On first start, VotifierPlus auto-generates `config.yml` and RSA key pair. A setup message appears in console 10 seconds after the server fully loads.

## Configuration

```yaml
# Host to bind the vote receiver. 0.0.0.0 = all interfaces.
host: 0.0.0.0

# TCP port for incoming votes
port: 8192

# V2 token-based authentication (HMAC challenge-response)
TokenSupport: false

# Debug verbosity: NONE | INFO | EXTRA | DEV
DebugLevel: NONE

# Paper: ticks to delay pending vote delivery on player join (min 20)
PendingVoteDelay: 20

# Rate limiting — protects against scanners and brute force
ConnectionThrottle:
  Enabled: false
  Window: "2m"
  Failures: 20
  ThrottleFor: "5m"
  PerClientBan:
    Enabled: true
    Failures: 6
    BanFor: "15m"

# Forward votes to downstream Votifier servers
Forwarding:
  server1:
    Enabled: false
    Host: ""
    Port: 8193
    Key: ""
    Token: ""
```

When the server starts, invalid configuration values are detected and startup is aborted with a clear error message. Fix the value and run `/votifierplus reload`.

## Commands

| Command | Permission | Description |
|---|---|---|
| `/votifierplus help` | `votifierplus.help` | Show command help |
| `/votifierplus reload` | `votifierplus.reload` | Reload configuration |
| `/votifierplus generatekeys` | `votifierplus.generatekeys` | Regenerate RSA key pair |
| `/votifierplus test <player> <service>` | `votifierplus.test` | Send a test vote |

Velocity aliases: `/vp`, `/votifierplusproxy`

## Developer API

### Paper (Bukkit)

```java
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.paper.events.VotifierEvent;  // canonical
// or: com.vexsoftware.votifier.model.VotifierEvent          // backward compat

@EventHandler
public void onVote(VotifierEvent event) {
    Vote vote = event.getVote();
    String player = vote.getUsername();
    String service = vote.getServiceName();
    // reward the player
}
```

### Velocity

```java
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.velocity.event.VotifierEvent;

@Subscribe
public void onVote(VotifierEvent event) {
    Vote vote = event.getVote();
    // reward the player
}
```

## Build from Source

```bash
git clone https://github.com/vanes430/VotifierPlus.git
cd VotifierPlus
./gradlew build
```

Output: `build/libs/VotifierPlus-*.jar`

Requirements: JDK 21, Git

## Project Structure

```
src/main/java/com/vexsoftware/votifier/
├── common/           Shared network layer
│   ├── crypto/       RSA encrypt/decrypt, key I/O, token utilities
│   ├── model/        Vote data model
│   └── net/          VoteReceiver, VoteParser, VoteForwarder,
│                     VoteThrottleService, VotePlatform adapter
├── paper/            Paper implementation
│   ├── config/       Bukkit FileConfiguration wrapper
│   ├── commands/     In-game commands (help, reload, generatekeys, test)
│   └── events/       Canonical Bukkit VotifierEvent
└── velocity/         Velocity implementation
    ├── Config.java   Configurate YAML wrapper
    ├── VotifierPlusVelocityCommand.java
    └── event/        Velocity VotifierEvent
```

## Protocol Support

### V1 (RSA)
- Legacy encrypt-with-public-key protocol
- Supported by all voting services (NuVotifier, Votifier, etc.)
- 256-byte encrypted block with opcode + fields

### V2 (Token / HMAC)
- Challenge-response handshake: `VOTIFIER 2 <challenge>`
- JSON payload signed with HMAC-SHA256
- Protocol auto-detected per connection — both V1 and V2 accepted simultaneously

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feat/my-change`)
3. Make your changes
4. Run `./gradlew spotlessApply` to format code
5. Commit (`git commit -m "feat: description"`)
6. Push and open a pull request

All Java source files include a GPLv3 license header. The project uses Spotless for code formatting and enforces consistent headers via CI.

## License

[GNU General Public License v3.0](LICENSE)

VotifierPlus is a fork of the original [Votifier](https://github.com/vexsoftware/votifier) by Vex Software LLC.
