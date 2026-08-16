# Changelog

## 3.27 (2026-08-16)

Changes since 3.26:

- Added CSV history export and improved FIT generation verification.
- Added an ANT scale-information view for manufacturer, model, hardware/software revision, serial
  number, operating time, voltage, and battery status when the scale broadcasts those fields.
- Migrated Garmin authentication to DI OAuth2 and strengthened token renewal, MFA notification,
  security-provider recovery, cookie isolation, foreground-operation, and history-import behavior.
- Made multi-file data changes atomic and hardened backup/restore, repository write failures,
  selected-profile handling, history expansion, editor state, and process recreation.
- Fixed muscle-only segment display, unavailable goal progress, USB device eligibility, and several
  accessibility labels.
- Enlarged and vertically centered the Garmin Connect and email indicators in user rows.
- Expanded local and Android smoke-test coverage and added continuous integration checks.

The persisted `users`, `history`, and `goals` formats remain compatible with earlier releases.
