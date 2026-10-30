package industries.angelic.biomic.browser;

import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** The biome browser's messages: requests from the browser, the server's answers. */
public final class BrowserMessages {

    private BrowserMessages() {}

    /** What the browser asks for. */
    public enum Action {
        CATALOGUE
    }

    public static final class Request implements IMessage {

        Action action = Action.CATALOGUE;

        public Request() {}

        public Request(Action action) {
            this.action = action;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            action = Action.values()[buf.readByte()];
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeByte(action.ordinal());
        }

        public static final class Handler implements IMessageHandler<Request, IMessage> {

            @Override
            public IMessage onMessage(Request message, MessageContext context) {
                // Network thread: the server answers on its own tick.
                BrowserServer.enqueue(context.getServerHandler().playerEntity, message.action);
                return null;
            }
        }
    }

    /** Go to the nearest spot of a biome, or (next) further out than the spots this search already visited. */
    public static final class Go implements IMessage {

        String key;
        BrowserCatalogue.Place place;
        boolean next;

        public Go() {}

        public Go(String key, BrowserCatalogue.Place place, boolean next) {
            this.key = key;
            this.place = place;
            this.next = next;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            key = ByteBufUtils.readUTF8String(buf);
            place = BrowserCatalogue.Place.values()[buf.readByte()];
            next = buf.readBoolean();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            ByteBufUtils.writeUTF8String(buf, key);
            buf.writeByte(place.ordinal());
            buf.writeBoolean(next);
        }

        public static final class Handler implements IMessageHandler<Go, IMessage> {

            @Override
            public IMessage onMessage(Go message, MessageContext context) {
                BrowserServer
                    .enqueueGo(context.getServerHandler().playerEntity, message.key, message.place, message.next);
                return null;
            }
        }
    }

    /** How a search goes: still looking, done without landing, or landed. */
    public enum Progress {
        LOOKING,
        DONE,
        LANDED
    }

    /** Back to a spot: an issue marked there. */
    public static final class GoTo implements IMessage {

        int dimension, x, y, z;

        public GoTo() {}

        public GoTo(int dimension, int x, int y, int z) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            dimension = buf.readInt();
            x = buf.readInt();
            y = buf.readInt();
            z = buf.readInt();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(dimension);
            buf.writeInt(x);
            buf.writeInt(y);
            buf.writeInt(z);
        }

        public static final class Handler implements IMessageHandler<GoTo, IMessage> {

            @Override
            public IMessage onMessage(GoTo message, MessageContext context) {
                BrowserServer.enqueueGoTo(
                    context.getServerHandler().playerEntity,
                    message.dimension,
                    message.x,
                    message.y,
                    message.z);
                return null;
            }
        }
    }

    /** Measure every biome's distance from here: a survey (BiomeLocator#survey). */
    public static final class Survey implements IMessage {

        public Survey() {}

        @Override
        public void fromBytes(ByteBuf buf) {}

        @Override
        public void toBytes(ByteBuf buf) {}

        public static final class Handler implements IMessageHandler<Survey, IMessage> {

            @Override
            public IMessage onMessage(Survey message, MessageContext context) {
                BrowserServer.enqueueSurvey(context.getServerHandler().playerEntity);
                return null;
            }
        }
    }

    /** A biome's nearest find in a place, in blocks from where the survey began; -1 when none is within reach. */
    public record Distance(String key, BrowserCatalogue.Place place, int distance) {}

    /** A survey's latest distances, and how far it has got. */
    public static final class Distances implements IMessage {

        List<Distance> distances;
        int done, total;

        public Distances() {}

        Distances(List<Distance> distances, int done, int total) {
            this.distances = distances;
            this.done = done;
            this.total = total;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            done = buf.readInt();
            total = buf.readInt();
            int count = buf.readShort();
            distances = new java.util.ArrayList<>(count);
            for (int i = 0; i < count; i++) distances.add(
                new Distance(
                    ByteBufUtils.readUTF8String(buf),
                    BrowserCatalogue.Place.values()[buf.readByte()],
                    buf.readInt()));
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(done);
            buf.writeInt(total);
            buf.writeShort(distances.size());
            for (Distance distance : distances) {
                ByteBufUtils.writeUTF8String(buf, distance.key());
                buf.writeByte(
                    distance.place()
                        .ordinal());
                buf.writeInt(distance.distance());
            }
        }

        public static final class Handler implements IMessageHandler<Distances, IMessage> {

            @Override
            public IMessage onMessage(Distances message, MessageContext context) {
                BrowserReplies.distances(message.distances, message.done, message.total);
                return null;
            }
        }
    }

    /** A line on how a search goes, for the browser (and the chat, once it is over). */
    public static final class Status implements IMessage {

        String text;
        Progress progress;

        public Status() {}

        Status(String text, Progress progress) {
            this.text = text;
            this.progress = progress;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            text = ByteBufUtils.readUTF8String(buf);
            progress = Progress.values()[buf.readByte()];
        }

        @Override
        public void toBytes(ByteBuf buf) {
            ByteBufUtils.writeUTF8String(buf, text);
            buf.writeByte(progress.ordinal());
        }

        public static final class Handler implements IMessageHandler<Status, IMessage> {

            @Override
            public IMessage onMessage(Status message, MessageContext context) {
                BrowserReplies.status(message.text, message.progress);
                return null;
            }
        }
    }

    public static final class Catalogue implements IMessage {

        /** Null when the player may not use the browser. */
        List<BrowserCatalogue.Entry> entries;

        public Catalogue() {}

        Catalogue(List<BrowserCatalogue.Entry> entries) {
            this.entries = entries;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            NBTTagCompound root = ByteBufUtils.readTag(buf);
            entries = root.getBoolean("allowed")
                ? BrowserCatalogue.read(root.getTagList("entries", Constants.NBT.TAG_COMPOUND))
                : null;
        }

        @Override
        public void toBytes(ByteBuf buf) {
            NBTTagCompound root = new NBTTagCompound();
            root.setBoolean("allowed", entries != null);
            if (entries != null) root.setTag("entries", BrowserCatalogue.write(entries));
            ByteBufUtils.writeTag(buf, root);
        }

        public static final class Handler implements IMessageHandler<Catalogue, IMessage> {

            @Override
            public IMessage onMessage(Catalogue message, MessageContext context) {
                BrowserReplies.catalogue(message.entries);
                return null;
            }
        }
    }
}
