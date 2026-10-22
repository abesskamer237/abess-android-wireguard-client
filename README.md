# ABESS Android WireGuard Client

Android/Kotlin + Jetpack Compose reference scaffold for standard WireGuard profiles.

## Current state

- Profile import/export and basic syntax validation are implemented.
- The VpnService is intentionally a lifecycle stub and stops without creating a TUN.
- The API button is a placeholder; it does not make a network request.
- The WireGuard userspace engine is not integrated, so this repository does not yet provide a working VPN connection.
- It does not implement or claim compatibility with Geny's proprietary WSS, camouflage, or SlowDNS transports.

## Build

Requires JDK 17 and Gradle 8.9 (Android Gradle Plugin 8.7.3).

    gradle assembleDebug

GitHub Actions runs the same debug build on pushes and pull requests. A successful build verifies compilation only; it does not prove a real tunnel works.

## Next steps for a functional VPN

1. Integrate and audit the official WireGuard Android tunnel library/engine.
2. Implement the Android VPN permission flow and tunnel lifecycle, with explicit error reporting.
3. Replace the placeholder API button with authenticated HTTPS enrollment using short-lived, single-use tokens. Never put the server's admin token in the app.
4. Test handshake, DNS, IPv4/IPv6 routing, reconnect, revocation, and expiry on a real Android device.
