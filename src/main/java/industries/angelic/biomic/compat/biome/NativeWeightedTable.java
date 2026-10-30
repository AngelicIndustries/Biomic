package industries.angelic.biomic.compat.biome;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Stable identities, not HashMap iteration or Math.random(), own native feature selection. */
public final class NativeWeightedTable<T> {

    public record Entry<T> (String key, double weight, T value) {}

    private final List<Entry<T>> entries;
    private final double total;

    public NativeWeightedTable(Collection<Entry<T>> input) {
        if (input.size() > 256) throw new IllegalArgumentException("Native weight table exceeds budget");
        var sorted = new ArrayList<>(input);
        for (var entry : sorted)
            if (entry == null || entry.key() == null) throw new IllegalArgumentException("Null native weighted entry");
        sorted.sort(Comparator.comparing(Entry::key));
        String previous = null;
        double sum = 0;
        for (var entry : sorted) {
            if (entry.key() == null || entry.key()
                .isEmpty()
                || entry.key()
                    .equals(previous)
                || entry.value() == null
                || !Double.isFinite(entry.weight())
                || entry.weight() <= 0
                || entry.weight() > 1_000_000)
                throw new IllegalArgumentException("Invalid or duplicate native weighted entry");
            sum += entry.weight();
            previous = entry.key();
        }
        entries = List.copyOf(sorted);
        total = sum;
    }

    public List<Entry<T>> entries() {
        return entries;
    }

    public T choose(Random random) {
        if (entries.isEmpty()) throw new IllegalStateException("No native weighted generators");
        double roll = random.nextDouble() * total;
        for (var entry : entries) {
            roll -= entry.weight();
            if (roll < 0) return entry.value();
        }
        return entries.get(entries.size() - 1)
            .value();
    }
}
