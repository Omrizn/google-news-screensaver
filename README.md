# Google News Headlines (Hebrew)

> This is a modified version of [MozKaboom/google-news-screensaver](https://github.com/MozKaboom/google-news-screensaver) (Apache-2.0). It adds an **Israel edition in Hebrew** (the default), a **right-to-left Hebrew screensaver** with translated labels, and **outlet lists** to choose which news sources appear. Install it under its own id, `google-news-headlines-he`, so it can sit next to the original plugin.

A [Kiosk Satellite](https://kiosksatellite.com) screensaver that cycles through the headlines on [Google News](https://news.google.com). Each story fills the screen with its headline, the outlet that published it and its logo, how long ago it was published and the other outlets covering the same story. Pick the edition, such as Israel, United Kingdom or United States, a section, such as World or Sport, or follow a search instead. Choose which outlets to show or hide.

This plugin is not affiliated with, endorsed by or sponsored by Google. It reads the public Google News RSS feeds.

## Screenshots

The screenshots are of the original English layout. With a Hebrew edition the same layout is mirrored right to left.

![A landscape screen showing three UK headlines from Google News side by side, each with its outlet's logo and the other outlets covering it](docs/screenshots/landscape.jpg)

![A portrait screen showing four UK headlines from Google News stacked, each with its outlet's logo and the other outlets covering it](docs/screenshots/portrait.jpg)

## Installation

Kiosk Satellite installs plugins from a GitHub repository. Put this code in your own repository, publish a release so the workflow builds the plugin, then add it by address:

1. Open **Settings > Plugin Manager** in Kiosk Satellite and turn on **Enable Plugins**.
2. Choose **Add plugin**, paste the address of your repository, for example `https://github.com/<you>/google-news-screensaver`, and choose **Preview**.
3. Review the plugin and choose **Trust and install**.

To install without GitHub, build the zip (see **Build**) and use **Plugin Manager > Developer Tools > Install from ZIP**.

## Use it

1. Enable **Google News Headlines (Hebrew)** from its entry row in Plugin Manager.
2. Open **Screensaver > Screensaver mode** and choose **Headlines (Google News Headlines (Hebrew))**. You can also pick it in a screensaver schedule entry.

Kiosk Satellite still decides when the screensaver starts and stops, its brightness, widgets and pixel shift. The plugin only draws the headlines.

The headlines load when the plugin starts and again on the interval you set. Until the first load finishes the screensaver shows **Loading headlines…**. If Google News can't be reached, it keeps showing the last headlines it loaded and says it's offline. The plugin's subpage shows when the headlines last loaded or why they didn't.

The **Refresh headlines** action loads the latest headlines right away. Run it from the plugin's subpage, a gesture, a drawer shortcut or a Home Assistant button.

## Settings

| Setting | Default | What it does |
| --- | --- | --- |
| Edition | ישראל (Israel) | The Google News edition, which sets the country and language of the headlines. Israel uses Hebrew. |
| Section | Top stories | Top stories, World, Your country, Business, Technology, Entertainment, Sport, Science or Health. Your country is the edition's national news. |
| Search instead | Empty | The latest stories about a subject, such as a team or a town, instead of a section. Leave it empty to use the section. |
| Only these outlets | Empty | Show stories only from these outlets. Separate names with commas, for example `ynet, maariv, haaretz`. A name matches part of the outlet's name or of its website address, ignoring case, so `ynet` matches both "Ynet" and `ynet.co.il`. Leave empty to show every outlet. |
| Hide these outlets | Empty | Never show stories from these outlets, and leave them out of the other coverage line. Same format as the list above. Hiding wins over showing. |
| Number of stories | 10 | How many headlines to cycle through, 3 to 20, in the order Google News lists them. With several headlines per screen it rounds up to fill the last screen. |
| Headlines per screen | 1 | How many headlines to show at once, 1 to 4. More than one stacks them on a portrait screen and puts them side by side on a landscape one. The text grows or shrinks to fill the screen. |
| Time per screen | 12 s | How long each screen of headlines stays up. |
| Refresh every | 30 min | How often to load the latest headlines. |
| Show outlet logos | On | Download each outlet's logo from Google. Turn off to show its initial instead. |
| Show other coverage | On | Name the other outlets covering each story, when Google News groups them. |
| Show clock | On | The time and date in the top right corner. |

## Hebrew and outlets

When the feed is in Hebrew (the Israel edition) the screensaver turns right to left: the layout is mirrored, the section names, "ago" times, the other coverage line and the date are in Hebrew, and Latin names such as "Nvidia" inside a Hebrew headline are kept in the right order. Other editions look as they did in the original plugin.

The outlet lists work on the stories Google News returns for the edition and section, usually thirty to a hundred. The **Number of stories** limit counts only the stories that pass the lists. A short list of outlets can leave you with fewer stories than you asked for. To follow particular outlets more closely, use **Search instead**. The text is passed to Google News search unchanged, so its search operators, such as `site:ynet.co.il OR site:maariv.co.il`, should work. This has not been tried on a device.

The setting labels in Kiosk Satellite's Plugin Manager stay in English.

## Data and privacy

On each refresh the plugin's Java code makes these requests, and no others:

- `https://news.google.com/rss` to read the feed for the edition and section, or the search.
- One request per outlet to Google's favicon service (`encrypted-tbn0.gstatic.com`) for its logo, unless **Show outlet logos** is off. The request names the outlet's website. Logos already loaded aren't downloaded again.

The requests carry no account, cookie or kiosk information beyond what any web browser sends. A search sends its words to Google.

The screensaver page itself makes no network requests. Kiosk Satellite blocks them. The headlines and logos are embedded in the page the plugin publishes.

## Build

Install Python 3, JDK 17 or newer and an Android SDK with a platform and build-tools. Set `ANDROID_HOME` and `JAVA_HOME` as needed.

```sh
python3 tools/test.py
python3 tools/build.py
```

The build creates `dist/google-news-headlines-<version>.zip`. To try it, open **Plugin Manager > Developer Tools > Install from ZIP** in Kiosk Satellite.

Releases are built by the GitHub Actions workflow in `.github/workflows/build.yml` when a release is published.

## License

[Apache-2.0](LICENSE). Modified from the original [Google News Headlines plugin](https://github.com/MozKaboom/google-news-screensaver) by MozKaboom: Israel edition and Hebrew right-to-left layout, outlet lists. The SDK interfaces in `sdk/` and the build tools in `tools/` come from the [Kiosk Satellite plugin template](https://github.com/jxlarrea/kiosk-satellite-plugins) under the same license.
