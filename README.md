# VotifierPlus

Fork of the original Votifier — listens for Minecraft server votes and fires Bukkit/Velocity events so other plugins can react. Supports Paper 1.21.1+ and Velocity 3.4.0+.

## Architecture

```
VotifierPlus.jar
├── common/           ← shared network layer (18 files)
│   ├── crypto/        RSA encrypt/decrypt, key I/O, token utils
│   ├── model/         Vote data model
│   └── net/           VoteReceiver, VoteParser, VoteForwarder,
│                      VoteThrottleService, VotePlatform adapter
├── paper/            ← Paper (Bukkit) implementation (5 files)
│   ├── VotifierPlus.java    main plugin class
│   ├── config/Config.java   config via Bukkit FileConfiguration
│   ├── commands/            help, reload, generatekeys, test
│   └── events/VotifierEvent canonical Bukkit event
└── velocity/         ← Velocity implementation (4 files)
    ├── VotifierPlusVelocity.java  main plugin class
    ├── Config.java               config via Configurate
    ├── VotifierPlusVelocityCommand.java
    └── event/VotifierEvent       Velocity event
```

Both platforms share the same network layer via `VotePlatform` — a bridge interface that adapts logging, events, and config access.

## Build

```
./gradlew build
```

Output: `build/libs/VotifierPlus-1.4.4-SNAPSHOT.jar`

Dependencies (all `compileOnly` — nothing shaded):
- Paper API 1.21.1
- Velocity API 3.4.0-SNAPSHOT (ships Configurate at runtime)

## Protocols

### V1 (RSA)
- Legacy Votifier protocol, encrypts vote with RSA public key
- Supported by all voting services (NuVotifier, Votifier)

### V2 (Token / HMAC)
- Challenge-response handshake (`VOTIFIER 2 <challenge>`)
- JSON payload signed with HMAC-SHA256
- Protocol auto-detected per connection — both V1 and V2 accepted simultaneously

## Commands

| Command | Permission | Description |
|---|---|---|
| `/votifierplus help` | `votifierplus.help` | Show help |
| `/votifierplus reload` | `votifierplus.reload` | Reload plugin |
| `/votifierplus generatekeys` | `votifierplus.generatekeys` | Regenerate RSA keys |
| `/votifierplus test <player> <service>` | `votifierplus.test` | Send test vote (alias: `vote`) |

Velocity aliases: `/vp`, `/votifierplusproxy`

## Events

### Paper (Bukkit)
```java
import com.vexsoftware.votifier.model.VotifierEvent;  // backward compat
import com.vexsoftware.votifier.paper.events.VotifierEvent;  // canonical

@EventHandler
public void onVote(VotifierEvent event) {
    Vote vote = event.getVote();
}
```

### Velocity
```java
import com.vexsoftware.votifier.velocity.event.VotifierEvent;

@Subscribe
public void onVote(VotifierEvent event) {
    Vote vote = event.getVote();
}
```

Old plugins importing `com.vexsoftware.votifier.model.VotifierEvent` and `com.vexsoftware.votifier.model.Vote` continue to work — both kept at original packages.

## Configuration

Single `config.yml` shared between Paper and Velocity:

```yaml
host: 0.0.0.0
port: 8192
TokenSupport: false
DebugLevel: NONE

ConnectionThrottle:
  Enabled: false
  Window: "2m"
  Failures: 20
  ThrottleFor: "5m"
  PerClientBan:
    Enabled: true
    Failures: 6
    BanFor: "15m"

Forwarding:
  server1:
    Enabled: false
    Host: ""
    Port: 8193
    Key: ""
    Token: ""
```

## Folia

`folia-supported: true` in plugin.yml. All scheduling uses `GlobalRegionScheduler` / `AsyncScheduler` — zero Bukkit scheduler calls.

## Project Stats

| Metric | Value |
|---|---|
| Java files | 27 |
| Total LOC | ~2.650 |
| JAR size | 72 KB |
| Dependencies shaded | 0 |
| Java version | 21 |

## License

GNU General Public License v3.0
