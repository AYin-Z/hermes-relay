# Hermes-Relay Android v1.17.0

**Release Date:** September 13, 2026

## Download

> Installing on your phone? Download `hermes-relay-1.17.0-sideload-release.apk` and tap it for the full feature set, or install from [Google Play](https://play.google.com/store/apps/details?id=com.axiomlabs.hermesrelay).

The `.aab` file is a Play Console upload bundle and cannot be installed by tapping it on a phone.

Verify the download against `SHA256SUMS.txt`. See the [sideload guide](https://hermes-relay.dev/docs/guide/sideload) for installation help.

## Summary

Google Play gains optional voice controls over other apps. This release also makes Clarify batches, profile identity, and chat context easier to follow while preserving confirmed answers and saved conversations.

## Added

- Start Voice Overlay from Voice Focus after granting microphone, notification, and display-over-other-apps access. Permission grants require a separate Start action. Stop voice from the overlay or persistent notification; screen lock, task removal, and permission loss end the session.

## Changed

- Standalone response cards use one surface, assistant bubbles are subtler, and timestamps share a row with delivery status.

## Fixed

- Answer upstream Clarify batches one question at a time, with independent choices, custom answers, and confirmed progress across reconnects. (#474)
- Context previews show that Gateway chats cannot send phone status or general turn context. Automatic phone-status sharing remains supported for API-only chats. (#556)
- Profiles display their Hermes names and group the resolved server default under its agent identity, preserving explicit selection and saved conversations.

## Install / Verify

- App version: **1.17.0** (versionCode **57**).
- Standard Chat, sessions, profiles, Manage, voice, and ordinary media use current upstream Hermes. Speech-to-text still requires a configured provider on the host.
- Hermes-Relay Plugin **1.11.3** is the optional release for Hermes-Relay tools and current Dashboard WebSocket compatibility.
- Explicit Direct API/API-only connections remain supported and are not used as silent failover for Dashboard-owned chats.
- Voice Overlay is available in Google Play and sideload builds. Device Control remains sideload-only.
- Gateway phone-status delivery and automatic Android identification remain unavailable pending upstream support.
- Physical Android 14-16 and OEM voice-overlay testing was not performed for this release. Code, rendered UI, existing emulator evidence, CI, and signed-package preflight provide the recorded verification.
