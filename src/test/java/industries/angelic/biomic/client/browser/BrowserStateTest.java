package industries.angelic.biomic.client.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class BrowserStateTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void notesAndIssuesSurviveAndAreReviewed() throws Exception {
        File directory = folder.newFolder("browser");
        BrowserState state = new BrowserState(directory);
        state.addNote("biomic:redwoods", "Redwoods", "Regions Unexplored", "grass too bright");
        state.addIssue(
            "biomic:redwoods",
            "Redwoods",
            "Regions Unexplored",
            "localhost:25611",
            0,
            10,
            70,
            -20,
            "floating leaves");
        state.addIssue(
            "biomic:infernal_holt",
            "Infernal Holt",
            "Regions Unexplored",
            "localhost:25611",
            -1,
            1,
            2,
            3,
            "");
        state.toggleFavorite("biomic:bayou");

        BrowserState read = new BrowserState(directory);
        read.load();
        assertEquals(state.notes("biomic:redwoods"), read.notes("biomic:redwoods"));
        assertEquals(state.issues("biomic:redwoods"), read.issues("biomic:redwoods"));
        assertTrue(read.favorite("biomic:bayou"));
        assertTrue(read.annotated("biomic:infernal_holt"));

        String review = new String(
            Files.readAllBytes(new File(directory, "review.md").toPath()),
            StandardCharsets.UTF_8);
        assertTrue(review.indexOf("## Infernal Holt") < review.indexOf("## Redwoods"));
        assertTrue(review.contains("note: grass too bright"));
        assertTrue(
            review.contains("issue at 10 70 -20 in the Overworld of localhost:25611: floating leaves `/tp 10 70 -20`"));
        assertTrue(review.contains("issue at 1 2 3 in the Nether of localhost:25611: (no text)"));
        assertTrue(review.contains("## Favourites without notes\n\n- `biomic:bayou`"));

        read.removeNote(
            "biomic:redwoods",
            read.notes("biomic:redwoods")
                .get(0));
        read.removeIssue(
            read.issues("biomic:redwoods")
                .get(0));
        assertFalse(read.annotated("biomic:redwoods"));
        String after = new String(
            Files.readAllBytes(new File(directory, "review.md").toPath()),
            StandardCharsets.UTF_8);
        assertFalse(after.contains("## Redwoods"));
    }
}
