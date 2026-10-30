package industries.angelic.biomic.world.terrain;

import java.util.Arrays;

import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;

/** Primitive pages keep snapshot copying bounded to array copies, not one NBT object graph per block. */
final class JournalIntPages {

    static final int PAGE_INTS = 16384;
    private final NBTTagList pages = new NBTTagList();
    private int[] page = new int[PAGE_INTS];
    private int used;

    void add(int value) {
        if (used == page.length) {
            pages.appendTag(new NBTTagIntArray(page));
            page = new int[PAGE_INTS];
            used = 0;
        }
        page[used++] = value;
    }

    NBTTagList finish() {
        if (used != 0) pages.appendTag(new NBTTagIntArray(Arrays.copyOf(page, used)));
        return pages;
    }

    static final class Reader {

        private final NBTTagList pages;
        private int pageIndex, offset;
        private int[] page = new int[0];
        private long remaining;

        Reader(NBTTagList pages) {
            this.pages = pages;
            for (int i = 0; i < pages.tagCount(); i++) {
                int length = pages.func_150306_c(i).length;
                if (length == 0 || length > PAGE_INTS) throw malformed();
                remaining += length;
            }
        }

        long remaining() {
            return remaining;
        }

        int next() {
            if (remaining == 0) throw malformed();
            if (offset == page.length) {
                page = pages.func_150306_c(pageIndex++);
                offset = 0;
            }
            remaining--;
            return page[offset++];
        }
    }

    static IllegalStateException malformed() {
        return new IllegalStateException("Malformed compact generation journal");
    }
}
