#!/usr/bin/env python3
"""Static dead-code report + source cut.

1. Compile src/main/java to CLASSES (any javac run with the Gradle compile classpath).
2. tools/deadcode/proguard_config.py CLASSES CLASSPATH JDK_CLASSES_DIR OUT_DIR > biomic.pro
   (JDK_CLASSES_DIR: `jimage extract --dir D $JAVA_HOME/lib/modules`; one subdirectory per module)
3. java -jar proguard.jar @biomic.pro            -> OUT_DIR/usage.txt (members unreachable from entry points)
4. javac -d out tools/deadcode/DeadCodeCutter.java
   java -cp out DeadCodeCutter OUT_DIR/usage.txt src/main/java CLASSPATH --apply
   It only removes members no remaining source references (javac-attributed fixed point), never enum constants,
   record components, side-effecting field initializers, the last constructor of a kept class, or anything in a
   read-only file. Methods that override one another go as a family, and only when ProGuard reports all of them and
   none overrides a library method. -Dwhy=FILE lists every reported element that stays, and why.
5. Repeat 1-4 until nothing is cut, then deal with what -Dwhy lists by hand (types only named in a live signature,
   interfaces nothing calls). Compile, fix the tests of removed code, run the benchmarks and a client flight.
"""
import os, sys
KEEPS = r'''-dontobfuscate
-dontoptimize
-dontpreverify
-ignorewarnings
-dontwarn **
-dontnote **
-printusage @OUT@/usage.txt
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Record
# Entry points only: the proxies FML instantiates, the mixin loaders named in the manifest. What they call is
# reachable; anything else in them is reported. Config options nothing reads are reported too.
-keep class industries.angelic.biomic.ClientProxy { <init>(); }
-keep class industries.angelic.biomic.CommonProxy { <init>(); }
-keep class industries.angelic.biomic.Tags { *; }
-keep class industries.angelic.biomic.core.** { <init>(...); }
# Mixins are applied by name; their injectors, overwrites and shadows are reached through the targets. Accessors and
# invokers are ordinary interfaces: reported when nothing calls them.
-keep class industries.angelic.biomic.mixins.** {
    <init>(...);
    @org.spongepowered.asm.mixin.injection.** *;
    @com.llamalad7.mixinextras.** *;
    @org.spongepowered.asm.mixin.Overwrite *;
    @org.spongepowered.asm.mixin.Shadow *;
}
-keep @cpw.mods.fml.common.Mod class * { <init>(); }
-keep @com.gtnewhorizon.gtnhlib.config.Config class *
-keepclassmembers class * {
    @cpw.mods.fml.common.eventhandler.SubscribeEvent *;
    @cpw.mods.fml.common.Mod$EventHandler *;
    @cpw.mods.fml.common.network.NetworkCheckHandler *;
    @cpw.mods.fml.common.Mod$Instance *;
    @cpw.mods.fml.common.SidedProxy *;
}
-keepclassmembers class * extends net.minecraft.world.WorldSavedData { <init>(java.lang.String); }
-keepclassmembers class * extends net.minecraft.tileentity.TileEntity { <init>(); }
-keepclassmembers class * extends net.minecraft.item.ItemBlock { <init>(...); }
-keepclassmembers class * extends net.minecraft.block.Block { <init>(...); }
-keepclassmembers class * extends net.minecraft.entity.Entity { <init>(net.minecraft.world.World); }
-keepclassmembers class * implements cpw.mods.fml.common.network.simpleimpl.IMessage { <init>(); }
-keepclassmembers class * implements cpw.mods.fml.common.network.simpleimpl.IMessageHandler { <init>(); }
-keepclassmembers class industries.angelic.biomic.compat.flora.ReviewedFloraRegistry$** { <fields>; <init>(...); }
-keep class industries.angelic.biomic.api.** { public *; }
-keep class industries.angelic.biomic.world.terrain.OrigenNativeFunctions { public static *; }
-keepclassmembers enum * { public static **[] values(); public static ** valueOf(java.lang.String); }
'''
classes, classpath, jdk, out = sys.argv[1:5]
lines = [f'-injars {classes}(industries/angelic/biomic/**)', f'-outjars {out}/out.jar']
lines += [f'-libraryjars {p}' for p in classpath.split(':') if p]
lines += [f'-libraryjars {jdk}/{m}(!module-info.class)' for m in sorted(os.listdir(jdk))]
print('\n'.join(lines))
print(KEEPS.replace('@OUT@', out))
