# gamesense-essentials
[![Downloads](https://img.shields.io/github/downloads/mtricht/gamesense-essentials/total.svg)](https://github.com/mtricht/gamesense-essentials/releases)

**Modern OLED integrations for SteelSeries GG with Windows 11 style, multi-language, and advanced customization.**

---

## Features

- **Modern Windows 11 UI**: Rounded corners, accent colors, and smooth design
- **Multi-language support**: 10+ languages (English, Türkçe, Deutsch, Français, 中文, 日本語, Русский, Español, Português, Italiano)
- **Customizable themes**: Light, dark, blue, green, and fully custom color themes
- **OLED screen widgets**:
  - Clock
  - Volume slider
  - Now playing artist and song for:
    - Spotify
    - iTunes
    - Qobuz
    - [TIDAL](https://tidal.com/)
    - [MusicBee](https://getmusicbee.com/)
    - [AIMP](https://www.aimp.ru/)
    - [th-ch's Youtube Music Desktop App](https://th-ch.github.io/youtube-music/)  
      (API server plugin must be enabled, running on port 26538, no authentication)
    - [YouTube Music Desktop App](https://ytmdesktop.app/)
- **System tray integration**: Quick access to all settings
- **Start with Windows**: One-click autostart option
- **Easy installer**: Windows .msi/.exe setup

---

## Screenshots

![Modern UI Screenshot](https://user-images.githubusercontent.com/7511094/122837368-3e0fad00-d2f4-11eb-868e-980b2b29e1c1.mp4)

---

## Download

- [Latest Release (Windows)](https://github.com/umutsevimcann/gamesense-essentials/releases)

---

## Getting Started

1. Download and install the latest release.
2. Run the app. The system tray icon will appear.
3. Right-click the tray icon to access all features and settings.
4. For music integrations, make sure the relevant player is running and (if needed) API plugins are enabled.

---

## Run on Windows Startup
To run after boot, enable "Start with Windows" in the app settings or create a shortcut to gamesense-essentials inside the "Startup" folder. See this [tutorial](https://www.howtogeek.com/208224/how-to-add-a-program-to-startup-in-windows/) if you need help.

---

## Credits & License
- Forked and based on [mtricht/gamesense-essentials](https://github.com/mtricht/gamesense-essentials) by [@mtricht](https://github.com/mtricht)
- MIT License

---

## Contributing
Pull requests, translations, and feature suggestions are welcome!

---

## Contact
- [GitHub Issues](https://github.com/umutsevimcann/gamesense-essentials/issues)
- [Original Project](https://github.com/mtricht/gamesense-essentials)

---

## What's New (v1.16.0)

- **Multi-language support improved:** All clock/date options and tray menu are now fully translated in 10+ languages.
- **Date display option:** You can now choose to show only the date or only the time (mutually exclusive).
- **12/24 hour format:** Easily switch between 12-hour and 24-hour clock formats from the tray menu.
- **Show seconds:** Option to display seconds in the clock, with smooth scrolling animation if enabled.
- **Tray menu improvements:** Clock/date options are grouped and exclusive, with clear translations for all supported languages.
- **Cleaner code:** Unused separator and redundant code removed for better performance and maintainability.
