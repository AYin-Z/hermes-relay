# Hermes-Relay Plugin v__VERSION__

**Release Date:** September 13, 2026

## Summary

Dashboard WebSocket connections work again with current Hermes authentication helpers, while older Hermes hosts remain supported.

## Fixed

- Resolve WebSocket guards from their current upstream module and retain the older-host fallback. Single-use tickets, Host/Origin/IP checks, and independent Hermes-Relay session authentication remain enforced. Missing or incomplete helper contracts deny admission.

## Install / update

    # Native upstream plugin path:
    hermes plugins install Codename-11/hermes-relay/plugin --enable

    # Classic install / update on a systemd host:
    curl -fsSL https://raw.githubusercontent.com/Codename-11/hermes-relay/server-v__VERSION__/install.sh | bash
    # or, if already installed:
    hermes-relay-update

Restart or reload the Hermes Dashboard and Relay after updating so the new manifest and prompt context are active.

## Verify

    hermes relay doctor
    python scripts/check-plugin-version-sync.py --expect __VERSION__

---

Tag prefixes: Android releases use android-v*, Plugin releases use server-v*, and CLI+UI releases use desktop-v*.
