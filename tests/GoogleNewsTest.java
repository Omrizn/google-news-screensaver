// SPDX-License-Identifier: Apache-2.0
package kiosk.googlenews;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.jxl.kiosk.plugins.PluginHost;

/** Exercises the feed parser and the plugin against a small feed shaped like Google News' RSS. */
public final class GoogleNewsTest {
    static final class Host implements PluginHost {
        final List<String> documents = java.util.Collections.synchronizedList(new ArrayList<>());
        volatile String status;
        volatile boolean statusError;
        boolean removed;
        public void showWindow(String title, String message, String button) {}
        public void hideWindow() {}
        public void log(String message) {}
        public void status(String message, boolean error) { status = message; statusError = error; }
        public void publishScreensaver(String key, String title, String html) {
            assert GoogleNewsPlugin.KEY.equals(key) && GoogleNewsPlugin.TITLE.equals(title);
            assert html.getBytes(StandardCharsets.UTF_8).length <= 512 * 1024;
            documents.add(html);
        }
        public void removeScreensaver(String key) { removed = true; }
        public InputStream openAsset(String name) {
            assert GoogleNewsPlugin.TEMPLATE.equals(name);
            return new ByteArrayInputStream("<script>__GN_DATA__</script>".getBytes(StandardCharsets.UTF_8));
        }
    }

    static String item(String id, String title, String site, String source, String description) {
        return "<item><title>" + title + "</title><link>https://news.google.com/rss/articles/" + id + "?oc=5</link>"
            + "<guid isPermaLink=\"false\">" + id + "</guid><pubDate>Sun, 01 Nov 2026 21:21:46 GMT</pubDate>"
            + "<description>" + description + "</description><source url=\"" + site + "\">" + source + "</source></item>";
    }

    static String li(String headline, String outlet) {
        return "&lt;li&gt;&lt;a href=\"https://news.google.com/rss/articles/x\" target=\"_blank\"&gt;" + headline
            + "&lt;/a&gt;&amp;nbsp;&amp;nbsp;&lt;font color=\"#6f6f6f\"&gt;" + outlet + "&lt;/font&gt;&lt;/li&gt;";
    }

    static String feed() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><rss version=\"2.0\"><channel><title>Top stories</title>"
            + item("a1", "Nine killed in jet crash &amp; fire - BBC", "https://www.bbc.co.uk", "BBC",
                "&lt;ol&gt;" + li("Nine killed", "BBC") + li("All nine died", "The Guardian") + li("Jet crash", "Sky News")
                    + li("Pilot&amp;#39;s last words", "The Guardian") + "&lt;/ol&gt;")
            + item("a2", "Budget unlocks \u00a310bn | London Evening Standard - Evening Standard", "https://www.standard.co.uk", "Evening Standard",
                "&lt;a href=\"x\"&gt;Budget unlocks&lt;/a&gt;&amp;nbsp;&amp;nbsp;&lt;font color=\"#6f6f6f\"&gt;Evening Standard&lt;/font&gt;")
            + item("a1", "Duplicate - BBC", "https://www.bbc.co.uk", "BBC", "")
            + item("a3", "No outlet here", "http://insecure.example", "Example", "")
            + "</channel></rss>";
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        assert HeadlineFeed.url("United Kingdom", "Top stories", "").equals("https://news.google.com/rss?hl=en-GB&gl=GB&ceid=GB:en");
        assert HeadlineFeed.url("United States", "Sport", " ").equals("https://news.google.com/rss/headlines/section/topic/SPORTS?hl=en-US&gl=US&ceid=US:en");
        assert HeadlineFeed.url("Nowhere", "Nothing", "Arsenal & Spurs").equals("https://news.google.com/rss/search?q=Arsenal+%26+Spurs&hl=he&gl=IL&ceid=IL:he");
        assert HeadlineFeed.url("Nowhere", "Top stories", "").equals("https://news.google.com/rss?hl=he&gl=IL&ceid=IL:he");
        assert HeadlineFeed.url("\u05d9\u05e9\u05e8\u05d0\u05dc", "Sport", "").equals("https://news.google.com/rss/headlines/section/topic/SPORTS?hl=he&gl=IL&ceid=IL:he");
        assert HeadlineFeed.EDITIONS.keySet().iterator().next().equals(HeadlineFeed.DEFAULT_EDITION);
        assert new GoogleNewsPlugin.Settings(new HashMap<String, Object>()).edition.equals("\u05d9\u05e9\u05e8\u05d0\u05dc");
        assert HeadlineFeed.url("\u65e5\u672c", "Technology", null).equals("https://news.google.com/rss/headlines/section/topic/TECHNOLOGY?hl=ja&gl=JP&ceid=JP:ja");
        assert HeadlineFeed.heading("World", "").equals("World");
        Map<String, Object> grouped = new HashMap<>();
        grouped.put("storyCount", 10);
        grouped.put("perScreen", 3);
        assert new GoogleNewsPlugin.Settings(grouped).storyLimit() == 12;
        grouped.put("perScreen", 1);
        assert new GoogleNewsPlugin.Settings(grouped).storyLimit() == 10;
        assert HeadlineFeed.unescape("&amp;lt;&#39;&#x41;&bogus;").equals("&lt;'A&bogus;");

        List<Map<String, Object>> stories = HeadlineFeed.parse(feed(), 10);
        assert stories.size() == 3 : stories;
        Map<String, Object> first = stories.get(0);
        assert "a1".equals(first.get("id")) && "Nine killed in jet crash & fire".equals(first.get("title")) : first;
        assert "BBC".equals(first.get("source")) && "https://www.bbc.co.uk".equals(first.get("site"));
        assert Integer.valueOf(4).equals(first.get("sources")) : first;
        assert first.get("outlets").equals(java.util.Arrays.asList("The Guardian", "Sky News")) : first;
        assert "Sun, 01 Nov 2026 21:21:46 GMT".equals(first.get("start"));
        Map<String, Object> second = stories.get(1);
        assert "Budget unlocks \u00a310bn".equals(second.get("title")) && second.get("sources") == null;
        assert ((List<Object>) second.get("outlets")).isEmpty();
        assert stories.get(2).get("site") == null && "No outlet here".equals(stories.get(2).get("title"));
        assert HeadlineFeed.parse(feed(), 2).size() == 2;
        assert HeadlineFeed.parse("<rss></rss>", 10).isEmpty();

        // Outlet lists: show only, hide, either by name or by website, hide wins, empty means everything.
        assert !OutletFilter.NONE.active() && OutletFilter.NONE.allows("Anyone", null);
        assert OutletFilter.tokens(" BBC, www.Ynet.co.il;;bbc\nSky News ").equals(java.util.Arrays.asList("bbc", "ynet.co.il", "sky news"));
        assert OutletFilter.host("https://www.Ynet.co.il/path?x=1").equals("ynet.co.il") && OutletFilter.host(null).isEmpty();
        OutletFilter only = new OutletFilter("bbc, standard.co.uk", "");
        assert only.active() && only.allows("BBC News", null) && only.allows("Other name", "https://www.standard.co.uk");
        assert !only.allows("Example", "http://insecure.example") && !only.allows(null, null);
        OutletFilter hide = new OutletFilter("", "guardian,example");
        assert hide.allows("BBC", "https://www.bbc.co.uk") && !hide.allows("The Guardian", null) && !hide.allows("X", "http://insecure.example");
        assert new OutletFilter("bbc", "bbc").allows("BBC", null) == false;
        assert new OutletFilter("A,b", "").key.equals(new OutletFilter(" a ; B ", null).key);
        assert hide.hides("The Guardian") && !hide.hides("Sky News") && !hide.hides(null);

        List<Map<String, Object>> onlyBbc = HeadlineFeed.parse(feed(), 10, new OutletFilter("bbc", ""));
        assert onlyBbc.size() == 1 && "BBC".equals(onlyBbc.get(0).get("source")) : onlyBbc;
        List<Map<String, Object>> noBbc = HeadlineFeed.parse(feed(), 10, new OutletFilter("", "bbc"));
        assert noBbc.size() == 2 && "Budget unlocks \u00a310bn".equals(noBbc.get(0).get("title")) : noBbc;
        // The limit counts only the stories kept, so hiding the first outlet does not shrink the result.
        assert HeadlineFeed.parse(feed(), 2, new OutletFilter("", "bbc")).size() == 2;
        // Hidden outlets disappear from "also covered by" and from the count.
        Map<String, Object> filtered = HeadlineFeed.parse(feed(), 10, new OutletFilter("", "guardian")).get(0);
        assert filtered.get("outlets").equals(java.util.Arrays.asList("Sky News")) && Integer.valueOf(2).equals(filtered.get("sources")) : filtered;
        assert HeadlineFeed.parse(feed(), 10, new OutletFilter("nobody", "")).isEmpty();

        // The plugin publishes a loading document, then the stories with their logos.
        Host host = new Host();
        GoogleNewsPlugin plugin = new GoogleNewsPlugin();
        final List<String> requests = java.util.Collections.synchronizedList(new ArrayList<>());
        plugin.fetcher = new GoogleNewsPlugin.Fetcher() {
            public String feed(String url) { requests.add(url); return GoogleNewsTest.feed(); }
            public byte[] image(String url) throws IOException {
                requests.add(url);
                if (url.contains("standard")) throw new IOException("HTTP 404");
                return new byte[] {(byte) 0x89, 'P', 'N', 'G'};
            }
        };
        Map<String, Object> settings = new HashMap<>();
        settings.put("edition", "Ireland");
        settings.put("secondsPerStory", 20);
        settings.put("perScreen", 9);
        plugin.start(host, settings);
        waitFor(() -> host.documents.size() >= 2 && host.status != null);
        assert requests.get(0).equals("https://news.google.com/rss?hl=en-IE&gl=IE&ceid=IE:en") : requests;
        assert host.documents.get(0).contains("\"stories\":[]");
        String shown = host.documents.get(host.documents.size() - 1);
        assert shown.contains("Nine killed in jet crash") && shown.contains("data:image/png;base64,iVBORw==") : shown;
        assert shown.contains("\"heading\":\"Top stories\"") && shown.contains("\"edition\":\"Ireland\"") && shown.contains("\"secondsPerStory\":20");
        assert shown.contains("\"perScreen\":4") : shown;
        assert host.status.startsWith("3 stories") && !host.statusError : host.status;
        int logoRequests = requests.size() - 1;
        assert logoRequests == 2 : requests;

        // A display-only change re-renders without fetching.
        int published = host.documents.size();
        settings.put("showClock", false);
        plugin.configure(settings);
        waitFor(() -> host.documents.size() > published);
        assert host.documents.get(host.documents.size() - 1).contains("\"showClock\":false");
        assert requests.size() == logoRequests + 1;

        // A new section refetches, and known logos come from the cache.
        settings.put("section", "Business");
        plugin.configure(settings);
        waitFor(() -> requests.size() > logoRequests + 1 && host.documents.get(host.documents.size() - 1).contains("\"heading\":\"Business\""));
        assert requests.get(requests.size() - 1).contains("/topic/BUSINESS?") : requests;

        // Outlet lists refetch, and a list that matches nothing is an answer, not an error.
        int beforeFilter = requests.size();
        settings.put("hideOutlets", "bbc");
        plugin.configure(settings);
        // The status is published after the document, so wait for it: the last document may still be the unfiltered one.
        waitFor(() -> requests.size() > beforeFilter && host.status != null && host.status.startsWith("2 stories"));
        String hidden = host.documents.get(host.documents.size() - 1);
        assert !hidden.contains("Nine killed") && host.status.startsWith("2 stories") && !host.statusError : host.status;
        settings.put("onlyOutlets", "nobody");
        plugin.configure(settings);
        waitFor(() -> host.status != null && host.status.startsWith("No stories match"));
        assert !host.statusError && host.documents.get(host.documents.size() - 1).contains("\"stories\":[]");
        assert !host.documents.get(host.documents.size() - 1).contains("Couldn't reach Google News");
        settings.put("onlyOutlets", "");
        settings.put("hideOutlets", "");
        plugin.configure(settings);
        waitFor(() -> host.status != null && host.status.startsWith("3 stories"));

        // A failed refresh keeps the last stories on screen.
        plugin.fetcher = new GoogleNewsPlugin.Fetcher() {
            public String feed(String url) throws IOException { throw new IOException("offline"); }
            public byte[] image(String url) throws IOException { throw new IOException("offline"); }
        };
        plugin.refresh();
        String stale = host.documents.get(host.documents.size() - 1);
        assert stale.contains("Nine killed") && stale.contains("Couldn't reach Google News") : stale;
        assert host.statusError;

        plugin.stop();
        assert host.removed;
        System.out.println("GoogleNewsTest passed");
    }

    interface Condition { boolean met(); }

    static void waitFor(Condition condition) throws InterruptedException {
        for (int i = 0; i < 200 && !condition.met(); i++) Thread.sleep(25);
        assert condition.met() : "Timed out";
    }
}
