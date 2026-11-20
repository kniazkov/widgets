# Changelog

Versions follow the policy in [RELEASING.ru.md](docs/maintainers/RELEASING.ru.md).
A heading describes a release candidate; publication is confirmed only by its immutable tag
and published artifacts. Entries never imply an artifact is already on Maven Central.

## [0.2.1]

- Local camera QR scanner with explicit permission and application acknowledgement.
- Repeated-frame protection, inverted QR decoding and camera lifecycle cleanup.

## [0.2.0]

First versioned baseline after the historical `0.1` development artifact.

- Public HTML and per-page metadata (`PublicPage`).
- Explicit single-owner deployment, native WSS transport and bounded HTTP fallback.
- Forms, keyboard actions, accessibility, navigation, overlays, data tables and upload manager.
- Lifecycle, protocol limits, acknowledged events and sequenced updates.
- Java/browser build identity; version-pinned resource URLs and explicit stale-client refusal.
- Clean release artifact verification and an independently compiled Maven consumer.

### Compatibility and migration

- Java 21 required. HTTP envelope 1, ordered updates 2, bundle negotiation 1.
- **Breaking from 0.1:** browser clients without the exact loaded bundle identity cannot
  create a UI session. Deploy Java and browser resources together and reload open tabs.
- API changes accumulated under 0.1 are not claimed binary-compatible. Recompile the consumer
  and run its tests before upgrading. See the review checklist and individual contract docs.
- Runtime UI sessions are ephemeral. Rollback requires the old application artifact and a reload;
  business-data migrations and their rollback belong to the consumer application.

## [0.1]

Historical unversioned-development baseline. Different commits used the same Maven coordinate;
retain the exact source SHA and artifact when reproducing an installation.
