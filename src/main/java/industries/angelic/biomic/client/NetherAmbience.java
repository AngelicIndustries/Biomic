package industries.angelic.biomic.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent17;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.lib.Reference;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.biome.BiomicBiomes;
import industries.angelic.biomic.biome.TartarusBiomes;
import industries.angelic.biomic.biome.TartarusBiomes.AmbientParticle;
import industries.angelic.biomic.biome.TartarusBiomes.Effects;
import industries.angelic.biomic.client.NetherAmbienceProfiles.Profile;
import industries.angelic.biomic.client.particle.NetherAmbientParticle;
import industries.angelic.biomic.config.AmbienceConfig;

/**
 * The Tartarus Nether's modern biome ambience, on the client only: each biome's loop and random additions, its mood
 * in place of the cave sound, its music in place of C418's, its ambient particles and its fog, as
 * {@link NetherAmbienceProfiles} gives them. The sounds are Et Futurum's runtime download from Mojang, played by name;
 * a channel whose sounds are missing stays as it was.
 */
@SideOnly(Side.CLIENT)
public final class NetherAmbience {

    public static final NetherAmbience INSTANCE = new NetherAmbience();

    /** Ticks a new biome must hold before the loop and music follow it, so borders don't flicker. */
    private static final int SETTLE_TICKS = 15;
    /** Modern loops fade in and out over two seconds. */
    private static final int FADE_TICKS = 40;
    /** A new biome's music must hold this long before the track gives way to it, fading out over MUSIC_FADE. */
    private static final int MUSIC_SETTLE_TICKS = 100, MUSIC_FADE_TICKS = 60, MUSIC_START_TICKS = 40;
    /** C418's Nether tracks (warped biomes keep them), and silence (Abyssal Shadowland). */
    private static final ResourceLocation C418 = new ResourceLocation("minecraft", "music.game.nether");

    /**
     * What a biome's music can be besides one of its own (a Biomic sound event, a String) or a modern set (Effects):
     * not Strings, so an own-music event can never be taken for one.
     */
    private enum Fixed {
        KEEP_C418,
        SILENCE
    }

    private static final Object KEEP_C418 = Fixed.KEEP_C418, SILENCE = Fixed.SILENCE;
    /** Modern additions: a chance each tick, about one every four and a half seconds. */
    private static final float ADDITION_CHANCE = .0111F;
    /** Until a Tartarus biome shows up, columns probed each tick within this many blocks. */
    private static final int PROBES = 8, PROBE_RANGE = 128;
    private static final int FOG_RADIUS = 2, FOG_STEP = 5;

    private static final ResourceLocation[] LOOPS = locations("ambient.", ".loop"),
        ADDITIONS = locations("ambient.", ".additions"), MOODS = locations("ambient.", ".mood"),
        MUSIC = locations("music.nether.", "");
    private static final Set<String> LOGGED = new HashSet<>();

    private final Random rand = new Random();
    private final NetherAmbienceProfiles.Settled<Profile> settled = new NetherAmbienceProfiles.Settled<>(SETTLE_TICKS);
    private WorldClient world;
    private Profile[] profiles = new Profile[0];
    /** Whether this world is the Tartarus Nether: one of its biomes has been seen. */
    private boolean tartarus;
    private boolean loopsFound, moodsFound, musicFound;
    private Ambient loop;
    private int loopAge;
    /** The track playing now, and the music the biome asks for once it has held. */
    private Track music;
    private final NetherAmbienceProfiles.Settled<Object> musicWanted = new NetherAmbienceProfiles.Settled<>(
        MUSIC_SETTLE_TICKS);
    /** Other mods' biome loops started before Biomic took over, stopped when it does. */
    private final Set<ISound> foreignLoops = Collections.newSetFromMap(new WeakHashMap<>());

    private NetherAmbience() {}

    /** {@code -Dbiomic.traceNetherMusic=true}: logs the music's decisions, and where the player is every 5 s. */
    private static final boolean TRACE = Boolean.getBoolean("biomic.traceNetherMusic");
    private int traceTicks;
    private String tracedBiome;
    private Object tracedWanted;

    private static void trace(String format, Object... args) {
        if (TRACE) Biomic.LOG.info("[nether music] " + format, args);
    }

    private static String describe(Track track) {
        return track == null ? "none"
            : track.key + " ("
                + track.getPositionedSoundLocation()
                + ", age "
                + track.age
                + (track.stopping ? ", fading" : "")
                + ")";
    }

    /** Whether Biomic plays the biome loops here, so other mods' Nether loops keep quiet. */
    public static boolean ownsLoops() {
        return INSTANCE.here() && AmbienceConfig.netherSounds && INSTANCE.loopsFound;
    }

    /** Whether Biomic replaces the cave sound here. */
    public static boolean ownsMood() {
        return INSTANCE.here() && AmbienceConfig.netherMood && INSTANCE.moodsFound;
    }

    /** Whether Biomic picks the Nether music here. */
    public static boolean ownsMusic() {
        return INSTANCE.here() && AmbienceConfig.netherMusic && INSTANCE.musicFound;
    }

    public static boolean ownsAny() {
        return ownsLoops() || ownsMood() || ownsMusic();
    }

    private boolean here() {
        return tartarus && world != null && world == Minecraft.getMinecraft().theWorld;
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        bind(mc.theWorld);
        EntityLivingBase camera = mc.renderViewEntity;
        if (world == null || world.provider.dimensionId != -1 || camera == null || mc.isGamePaused()) return;
        int x = MathHelper.floor_double(camera.posX), y = MathHelper.floor_double(camera.posY);
        int z = MathHelper.floor_double(camera.posZ);
        if (world.getChunkFromBlockCoords(x, z)
            .isEmpty()) return;
        if (!tartarus) probe(x, z);
        if (!tartarus) return;
        Profile here = settled.update(profileAt(x, z));
        if (TRACE) traceTick(mc.getSoundHandler(), x, y, z, here);
        sounds(mc.getSoundHandler(), here);
        followMusic(mc.getSoundHandler(), here);
        if (AmbienceConfig.netherParticles) particles(x, y, z, mc.gameSettings.particleSetting);
    }

    /** Binds to a new client world: the dimension change or world join already stopped every sound. */
    private void bind(WorldClient current) {
        if (current == world) return;
        world = current;
        tartarus = false;
        settled.reset();
        musicWanted.reset();
        loop = null;
        music = null;
        foreignLoops.clear();
        if (current == null || current.provider.dimensionId != -1) return;
        profiles = NetherAmbienceProfiles.byBiomeId(BiomeGenBase.getBiomeGenArray(), BiomicBiomes::getBiome);
        SoundHandler sounds = Minecraft.getMinecraft()
            .getSoundHandler();
        List<ResourceLocation> loops = new ArrayList<>(List.of(LOOPS)), tracks = new ArrayList<>();
        loops.addAll(List.of(ADDITIONS));
        for (Effects set : Effects.values()) if (NetherAmbienceProfiles.hasMusic(set)) tracks.add(MUSIC[set.ordinal()]);
        loopsFound = found(sounds, "biome loops", loops);
        moodsFound = found(sounds, "moods", List.of(MOODS));
        musicFound = found(sounds, "music", tracks);
        trace(
            "entered the Nether: Biomic's music sounds {}, music setting {}",
            musicFound ? "present" : "missing",
            AmbienceConfig.netherMusic);
    }

    private void traceTick(SoundHandler sounds, int x, int y, int z, Profile here) {
        BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
        String name = biome == null ? "?" : biome.biomeName;
        if (!name.equals(tracedBiome)) {
            trace("biome {} at {} {} {} asks for {}", name, x, y, z, musicKey(profileAt(x, z)));
            tracedBiome = name;
        }
        if (++traceTicks % 100 == 0) trace(
            "at {} {} {} in {}: settled biome asks for {}, held music {}, track {}, playing {}, Biomic owns it {}",
            x,
            y,
            z,
            name,
            here == null ? "?" : musicKey(here),
            musicWanted.value(),
            describe(music),
            music != null && sounds.isSoundPlaying(music),
            ownsMusic());
    }

    /** Every music sound that is about to play in the Nether, after all mods had their say. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void traceSound(PlaySoundEvent17 event) {
        if (!TRACE || world == null || world.provider.dimensionId != -1 || event.sound == null) return;
        if (event.category != net.minecraft.client.audio.SoundCategory.MUSIC && !event.name.contains("music")) return;
        trace(
            "music sound {} -> {}",
            event.sound.getPositionedSoundLocation(),
            event.result == null ? "cancelled"
                : event.result instanceof Track track ? "Biomic track " + describe(track)
                    : event.result.getPositionedSoundLocation());
    }

    private static boolean found(SoundHandler sounds, String channel, List<ResourceLocation> names) {
        List<String> missing = new ArrayList<>();
        for (ResourceLocation name : names) if (sounds.getSound(name) == null) missing.add(name.toString());
        if (missing.isEmpty()) return true;
        if (LOGGED.add(channel)) Biomic.LOG.warn(
            "Nether ambience: Et Futurum's {} are missing ({}), so they stay as "
                + "they were. Et Futurum downloads them from Mojang at start.",
            channel,
            missing);
        return false;
    }

    private void probe(int x, int z) {
        see(world.getBiomeGenForCoords(x, z));
        for (int i = 0; i < PROBES && !tartarus; i++) {
            int px = x + rand.nextInt(2 * PROBE_RANGE + 1) - PROBE_RANGE;
            int pz = z + rand.nextInt(2 * PROBE_RANGE + 1) - PROBE_RANGE;
            if (!world.getChunkFromBlockCoords(px, pz)
                .isEmpty()) see(world.getBiomeGenForCoords(px, pz));
        }
    }

    /** Tartarus biomes are only ever placed in the Tartarus Nether. */
    private void see(BiomeGenBase biome) {
        if (biome != null && TartarusBiomes.fromBiomeId(biome.biomeID) != null) tartarus = true;
    }

    private Profile profileOf(BiomeGenBase biome) {
        int id = biome == null ? -1 : biome.biomeID;
        return id >= 0 && id < profiles.length ? profiles[id] : null;
    }

    private Profile profileAt(int x, int z) {
        Profile profile = profileOf(world.getBiomeGenForCoords(x, z));
        return profile != null ? profile : NetherAmbienceProfiles.DEFAULT;
    }

    private void sounds(SoundHandler sounds, Profile here) {
        boolean owns = ownsLoops();
        if (owns && !foreignLoops.isEmpty()) {
            for (ISound foreign : foreignLoops) sounds.stopSound(foreign);
            foreignLoops.clear();
        }
        Effects wanted = owns ? here.loop() : null;
        if (loop != null && loop.set != wanted) {
            loop.fadeOut();
            loop = null;
        }
        // Started again if something stopped it, or it never started (the ambient volume was off).
        if (loop != null && ++loopAge > FADE_TICKS && !sounds.isSoundPlaying(loop)) loop = null;
        if (loop == null && wanted != null) {
            loop = new Ambient(wanted, true);
            loopAge = 0;
            sounds.playSound(loop);
        }
        if (wanted != null && rand.nextFloat() < ADDITION_CHANCE) sounds.playSound(new Ambient(wanted, false));
    }

    private void particles(int x, int y, int z, int particleSetting) {
        float scale = AmbienceConfig.netherParticleScale;
        if (particleSetting >= 2 || scale <= 0) return;
        int height = world.getHeight();
        for (int i = 0; i < NetherAmbienceProfiles.SAMPLES; i++) {
            // Spread like vanilla's: denser near the camera.
            int px = x + rand.nextInt(16) - rand.nextInt(16), py = y + rand.nextInt(16) - rand.nextInt(16);
            int pz = z + rand.nextInt(16) - rand.nextInt(16);
            if (py < 0 || py >= height) continue;
            Profile profile = profileOf(world.getBiomeGenForCoords(px, pz));
            if (profile == null || profile.particle() == null) continue;
            AmbientParticle particle = profile.particle();
            float chance = NetherAmbienceProfiles.spotChance(
                profile.particleChance(),
                particleSetting,
                particle == AmbientParticle.WHITE_ASH ? scale * AmbienceConfig.netherWhiteAshScale : scale);
            if (rand.nextFloat() >= chance || !world.isAirBlock(px, py, pz)) continue;
            NetherAmbientParticle
                .spawn(world, particle, px + rand.nextDouble(), py + rand.nextDouble(), pz + rand.nextDouble());
        }
    }

    /** Runs after Netherlicious' and Et Futurum's handlers, so its choice is the one that plays. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void sound(PlaySoundEvent17 event) {
        Minecraft mc = Minecraft.getMinecraft();
        bind(mc.theWorld);
        if (event.sound == null || world == null
            || world.provider.dimensionId != -1
            || event.sound instanceof Ambient
            || event.sound instanceof Track) return;
        if (foreignLoop(event.sound, event.name)) {
            // One biome loop at a time: another mod's loop that started before Biomic knew the world is stopped later.
            if (ownsLoops()) event.result = null;
            else if (event.result != null) foreignLoops.add(event.result);
            return;
        }
        if (!"minecraft".equals(
            event.sound.getPositionedSoundLocation()
                .getResourceDomain()))
            return;
        if (event.name.equals("ambient.cave.cave") && ownsMood()) mood(event);
        else if (event.name.equals("music.game.nether") && ownsMusic()) music(event, mc);
    }

    /** A biome loop: an ambient sound that repeats and has no position. */
    private static boolean foreignLoop(ISound sound, String name) {
        return sound.canRepeat() && sound.getAttenuationType() == ISound.AttenuationType.NONE
            && name.startsWith("ambient.")
            && name.endsWith(".loop");
    }

    /** The biome at the dark spot gives the mood, which plays there as modern moods do, at full volume and pitch. */
    private void mood(PlaySoundEvent17 event) {
        ISound cave = event.sound;
        Profile profile = profileAt(MathHelper.floor_float(cave.getXPosF()), MathHelper.floor_float(cave.getZPosF()));
        if (profile.mood() == null) event.result = cave;
        else event.result = new PositionedSoundRecord(
            MOODS[profile.mood()
                .ordinal()],
            1F,
            1F,
            cave.getXPosF(),
            cave.getYPosF(),
            cave.getZPosF());
    }

    /**
     * 1.7.10 tries a Nether track every one to three minutes; while Biomic's still plays, that try is skipped. The
     * track is the biome's (warped forests keep C418's), played as a {@link Track} so it can fade when the player
     * moves on (see {@link #followMusic}).
     */
    private void music(PlaySoundEvent17 event, Minecraft mc) {
        if (music != null && mc.getSoundHandler()
            .isSoundPlaying(music)) {
            trace("the music ticker's try is skipped: {} still plays", describe(music));
            event.result = null;
            return;
        }
        music = null;
        Profile here = settled.value();
        if (here == null) {
            EntityLivingBase camera = mc.renderViewEntity;
            if (camera == null) return;
            here = profileAt(MathHelper.floor_double(camera.posX), MathHelper.floor_double(camera.posZ));
        }
        event.result = music = track(musicKey(here));
        trace("the music ticker starts {}", music == null ? "nothing (silent biome)" : describe(music));
    }

    /**
     * The music follows the player (review, 2026-10-24: "the music doesn't seem to change depending on biomes"): once
     * a biome whose music differs has held for MUSIC_SETTLE_TICKS, the track fades out and the biome's own starts (or
     * none, in a silent biome). Otherwise 1.7.10's music ticker picks when the next track starts, as before.
     */
    private void followMusic(SoundHandler sounds, Profile here) {
        if (!ownsMusic()) return;
        Object wanted = musicWanted.update(musicKey(here));
        if (TRACE && !java.util.Objects.equals(wanted, tracedWanted)) {
            trace("the biome's music has held: now {}", wanted);
            tracedWanted = wanted;
        }
        if (music == null) return;
        // A track the sound system never ticked (its sound missing, or not started) counts as ended after a while.
        boolean neverStarted = music.age == 0 && ++music.unstarted > MUSIC_START_TICKS;
        if (!sounds.isSoundPlaying(music) && (music.age > MUSIC_FADE_TICKS || neverStarted)) {
            // Faded out for a new biome: its music starts at once; a track that ended leaves the next to the ticker.
            boolean switching = music.stopping;
            if (neverStarted) trace(
                "{} never started; {}",
                describe(music),
                switching ? "starting " + wanted : "the music ticker starts the next");
            else if (switching) trace("faded out {}; starting {}", describe(music), wanted);
            else trace("{} ended; the music ticker starts the next", describe(music));
            music = null;
            if (switching && wanted != SILENCE) sounds.playSound(music = track(wanted));
            return;
        }
        if (!music.stopping && !java.util.Objects.equals(wanted, music.key)) {
            trace("fading out {} for {}", describe(music), wanted);
            music.fadeOut();
        }
    }

    /** The music a biome plays: its own, its modern set, C418's tracks, or silence. */
    private static Object musicKey(Profile profile) {
        if (profile.ownMusic() != null) return profile.ownMusic();
        return profile.silent() ? SILENCE : profile.music() == null ? KEEP_C418 : profile.music();
    }

    private static Track track(Object key) {
        if (key == SILENCE) return null;
        if (key == KEEP_C418) return new Track(C418, key);
        if (key instanceof String event) return new Track(new ResourceLocation(event), key);
        return new Track(MUSIC[((Effects) key).ordinal()], key);
    }

    /**
     * Each biome's fog, averaged over the surrounding 20 blocks so borders fade. Runs after BOP's and Netherlicious'
     * fog, whose colour stays wherever the table leaves the fog alone.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void fog(EntityViewRenderEvent.FogColors event) {
        if (!(event.entity.worldObj instanceof WorldClient current) || current.provider.dimensionId != -1) return;
        if (event.block != null && event.block.getMaterial() != Material.air) return;
        bind(current);
        int x = MathHelper.floor_double(event.entity.posX), z = MathHelper.floor_double(event.entity.posZ);
        float red = 0, green = 0, blue = 0;
        int samples = 0;
        boolean coloured = false;
        for (int dx = -FOG_RADIUS; dx <= FOG_RADIUS; dx++) for (int dz = -FOG_RADIUS; dz <= FOG_RADIUS; dz++) {
            samples++;
            BiomeGenBase biome = current.getBiomeGenForCoords(x + dx * FOG_STEP, z + dz * FOG_STEP);
            see(biome);
            Profile profile = profileOf(biome);
            int fog = profile == null ? NetherAmbienceProfiles.NO_FOG : profile.fog();
            if (fog == NetherAmbienceProfiles.NO_FOG) {
                red += event.red;
                green += event.green;
                blue += event.blue;
                continue;
            }
            coloured = true;
            red += (fog >> 16 & 255) / 255F;
            green += (fog >> 8 & 255) / 255F;
            blue += (fog & 255) / 255F;
        }
        if (!coloured || !tartarus) return;
        event.red = red / samples;
        event.green = green / samples;
        event.blue = blue / samples;
    }

    private static ResourceLocation[] locations(String prefix, String suffix) {
        Effects[] sets = Effects.values();
        ResourceLocation[] sounds = new ResourceLocation[sets.length];
        // Et Futurum's AssetDirector namespace for the modern sounds it downloads.
        for (int i = 0; i < sets.length; i++)
            sounds[i] = new ResourceLocation(Reference.MCAssetVer, prefix + sets[i].key() + suffix);
        return sounds;
    }

    /** A music track that can fade out when the player's biome asks for other music. */
    private static final class Track extends MovingSound {

        final Object key;
        boolean stopping;
        /** Ticks the sound system played this track; ticks it waited to start without being played. */
        int age, unstarted;

        Track(ResourceLocation location, Object key) {
            super(location);
            this.key = key;
            repeat = false;
            field_147665_h = 0;
            field_147666_i = ISound.AttenuationType.NONE;
            volume = 1F;
        }

        void fadeOut() {
            stopping = true;
        }

        @Override
        public void update() {
            age++;
            if (!stopping) return;
            volume = Math.max(0F, volume - 1F / MUSIC_FADE_TICKS);
            if (volume <= 0F) donePlaying = true;
        }
    }

    /**
     * A biome loop, fading in and out as modern loops do, or one of its additions. Both follow the player, so they
     * sound the same in both ears.
     */
    private static final class Ambient extends MovingSound {

        final Effects set;
        private final boolean loop;
        private int fade;
        private boolean stopping;

        Ambient(Effects set, boolean loop) {
            super((loop ? LOOPS : ADDITIONS)[set.ordinal()]);
            this.set = set;
            this.loop = loop;
            repeat = loop;
            field_147665_h = 0;
            field_147666_i = ISound.AttenuationType.NONE;
            // A sound that starts silent never starts at all.
            volume = loop ? 1F / FADE_TICKS : 1F;
            follow();
        }

        void fadeOut() {
            stopping = true;
        }

        @Override
        public void update() {
            follow();
            if (!loop) return;
            fade = Math.max(0, Math.min(FADE_TICKS, fade + (stopping ? -1 : 1)));
            volume = Math.max(1, fade) / (float) FADE_TICKS;
            if (stopping && fade == 0) donePlaying = true;
        }

        private void follow() {
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            if (player == null) return;
            xPosF = (float) player.posX;
            yPosF = (float) player.posY;
            zPosF = (float) player.posZ;
        }
    }
}
