package industries.angelic.biomic.browser;

import java.util.List;

/**
 * The server's answers as the browser screen picks them up: written on the network thread, read on the client
 * thread when the screen updates.
 */
public final class BrowserReplies {

    /** Set once an answer arrives; the screen takes it. */
    public record Catalogue(List<BrowserCatalogue.Entry> entries) {

        public boolean allowed() {
            return entries != null;
        }
    }

    /** A search's latest line. */
    public record Status(String text, BrowserMessages.Progress progress) {}

    /** How far the latest survey has got: measured, of how many. */
    public record SurveyProgress(int done, int total) {}

    private static volatile Catalogue catalogue;
    private static final java.util.concurrent.ConcurrentLinkedQueue<BrowserMessages.Distance> DISTANCES = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private static volatile SurveyProgress survey;
    private static volatile Status status;

    private BrowserReplies() {}

    static void catalogue(List<BrowserCatalogue.Entry> entries) {
        catalogue = new Catalogue(entries);
    }

    static void distances(List<BrowserMessages.Distance> distances, int done, int total) {
        DISTANCES.addAll(distances);
        survey = new SurveyProgress(done, total);
    }

    /** The distances that arrived since last asked. */
    public static List<BrowserMessages.Distance> takeDistances() {
        List<BrowserMessages.Distance> taken = new java.util.ArrayList<>();
        for (BrowserMessages.Distance distance; (distance = DISTANCES.poll()) != null;) taken.add(distance);
        return taken;
    }

    /** A new survey begins: the last one's progress and unread distances go. */
    public static void surveyStarted() {
        DISTANCES.clear();
        survey = new SurveyProgress(0, 1);
    }

    /** How far the latest survey has got, or null before any. */
    public static SurveyProgress surveyProgress() {
        return survey;
    }

    static void status(String text, BrowserMessages.Progress progress) {
        status = new Status(text, progress);
    }

    /** The latest search status, once: null until one arrives. */
    public static Status takeStatus() {
        Status taken = status;
        status = null;
        return taken;
    }

    /** The latest catalogue answer, once: null until one arrives. */
    public static Catalogue takeCatalogue() {
        Catalogue taken = catalogue;
        catalogue = null;
        return taken;
    }
}
