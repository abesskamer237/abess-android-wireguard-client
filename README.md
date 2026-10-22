# ABESS Android WireGuard Client

Android/Kotlin + Jetpack Compose client scaffold using the official embeddable WireGuard Android tunnel library.

## Implemented in this revision

- Imports and exports standard WireGuard `.conf` profiles.
- Parses configurations using the WireGuard library's parser.
- Requests Android VPN permission before starting a tunnel.
- Starts/stops the userspace WireGuard backend through `GoBackend`.
- Adds CI build verification.

## Still incomplete

- The enrollment button calls `POST /v1/enroll` over HTTPS using a one-time activation code issued by the server admin. The server only issues such codes for an active, paid, unexpired subscription with available device slots. The app displays the returned plan, expiry and device allowance. See the server repository's `docs/enrollment.md`.
- Customer checkout, provider payment UI, account login and lease heartbeat are not integrated into this reference client. The server enforces subscription status, expiry and device limits; provider-specific payment verification must be configured on the server.
- A successful APK build does not prove that a server handshake, DNS resolution, or Internet routing works. Test with a valid profile and reachable WireGuard server on a real Android device.
- This is standard WireGuard over UDP; it does not implement Geny's proprietary WSS/camouflage/SlowDNS transports.

## Build

Requires JDK 17 and Gradle 8.9+ (Android Gradle Plugin 8.7.3).

    gradle assembleDebug

GitHub Actions builds the debug APK on pushes and pull requests. The WireGuard tunnel library is published on Maven Central; see the official [WireGuard embedding documentation](https://www.wireguard.com/embedding/).

## Security

Client configuration contains a private key. Do not share profile files or paste them into public issues. Never put the server's administrative token in the Android app.
