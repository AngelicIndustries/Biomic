
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.api.tasks.testing.Test
import xyz.wagyourtail.jvmdg.gradle.task.DowngradeJar
import com.gtnewhorizons.retrofuturagradle.minecraft.RunMinecraftTask
import java.util.UUID

plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

apply(from = "https://raw.githubusercontent.com/AngelicIndustries/67minecraft-gradle-publish/${property("publish67ScriptTag")}/67minecraft-publish.gradle.kts")

// Mixin's Java8 compatibility level cannot accept native nestmates from a multi-release overlay.
// Keep every mixin and its generated helpers on the downgraded base path, while ordinary code
// retains native multi-release implementations (including Math.fma on modern JVMs).
tasks.withType<DowngradeJar>().configureEach {
    val versionedMixins = "META-INF/versions/*/industries/angelic/biomic/mixins/**"
    // DowngradeJar builds its CopySpec during its task action, so explicitly track the packaging rule.
    inputs.property("biomicJava8MixinPackage", versionedMixins)
    exclude(versionedMixins)
}

dependencies {
    // GTNHLib provides FastUtil in-game; the convention test classpath intentionally omits mod runtime deps.
    testRuntimeOnly("it.unimi.dsi:fastutil:8.5.18")
}

// Several parity tests intentionally retain complete immutable Origen graphs. Recycle the test worker before those
// independent fixtures accumulate enough static cache state to starve TerrainPreviewTest's large raster assertion.
tasks.withType<Test>().configureEach {
    forkEvery = 8
}

// A live development JVM must not lazily read a JAR/directory that another Gradle invocation rewrites.
// Opt-in for long-running preview servers/isolated clients, leaving ordinary development tasks unchanged.
val snapshotPreviewRuntime = providers.gradleProperty("biomicSnapshotRuntime").map(String::toBoolean).orElse(false)
val previewBuildRoot = layout.buildDirectory.get().asFile.toPath().toAbsolutePath().normalize()
tasks.withType<RunMinecraftTask>().configureEach {
    if (snapshotPreviewRuntime.get()) {
        doFirst("biomicSnapshotRuntimeClasspath") {
            val snapshot = workingDir.resolve("runtime-snapshots/${UUID.randomUUID()}")
            val paths = classpath.files.mapIndexed { index, source ->
                if (!source.toPath().toAbsolutePath().normalize().startsWith(previewBuildRoot)) source
                else {
                    check(source.exists()) { "Missing preview classpath entry: $source" }
                    val target = snapshot.resolve("$index-${source.name}")
                    target.parentFile.mkdirs()
                    if (source.isDirectory) source.copyRecursively(target, overwrite = false)
                    else source.copyTo(target, overwrite = false)
                    target
                }
            }
            classpath = files(paths)
            logger.lifecycle("Isolated live project classpath in $snapshot")
        }
    }
}

tasks.named<ProcessResources>("processResources").configure {
    from(layout.projectDirectory.file("THIRD_PARTY_NOTICES.md"))
    from(layout.projectDirectory.file("LICENSE"))
    from(layout.projectDirectory.file("LICENSE-MIT"))
}

val worldgenBenchmarkMode = providers.gradleProperty("biomicWorldgenBenchmark")
    .map(String::toBoolean)
    .orElse(false)
val worldgenBenchmarkCenterChunkX = providers.gradleProperty("biomicBenchmarkCenterChunkX").orElse("256")
val worldgenBenchmarkCenterChunkZ = providers.gradleProperty("biomicBenchmarkCenterChunkZ").orElse("-256")
val worldgenBenchmarkRadius = providers.gradleProperty("biomicBenchmarkRadius").orElse("12")
val worldgenBenchmarkRoot = layout.buildDirectory.dir("worldgen-benchmark")
val worldgenBenchmarkResult = worldgenBenchmarkRoot.map { it.file("result.txt") }
val worldgenBenchmarkJfr = providers.gradleProperty("biomicBenchmarkJfr")
val profileExpressions = providers.gradleProperty("biomicProfileExpressions")
    .map(String::toBoolean)
    .orElse(false)
val profileTerraScripts = providers.gradleProperty("biomicProfileTerraScripts")
    .map(String::toBoolean)
    .orElse(false)
val profileFeatures = providers.gradleProperty("biomicProfileFeatures")
    .map(String::toBoolean)
    .orElse(false)
val normalServerRoot = providers.gradleProperty("runServerWorkingDirectory")
    .map(layout.projectDirectory::dir)
    .orElse(layout.projectDirectory.dir("run/server"))
val auditViewDistance = providers.gradleProperty("biomicAuditViewDistance").orElse("20").get().toInt()
require(auditViewDistance in 3..32) { "Audit view distance must be in 3..32" }

tasks.named<RunMinecraftTask>("runServer25").configure {
    // The GTNH run task already appends the literal `nogui` server argument. Keep
    // AWT headless too, so a future launcher/plugin regression cannot open Swing.
    extraJvmArgs.add("-Djava.awt.headless=true")
    if (profileExpressions.get()) {
        extraJvmArgs.add("-Dbiomic.profileExpressions=true")
    }
    if (profileTerraScripts.get()) {
        extraJvmArgs.add("-Dbiomic.profileTerraScripts=true")
    }
    if (profileFeatures.get()) {
        extraJvmArgs.add("-Dbiomic.profileFeatures=true")
    }
    if (worldgenBenchmarkMode.get()) {
        val root = worldgenBenchmarkRoot.get().asFile
        workingDir(root)
        extraJvmArgs.add("-Dbiomic.worldgenBenchmark=true")
        extraJvmArgs.add("-Dbiomic.worldgenBenchmarkResult=${worldgenBenchmarkResult.get().asFile.absolutePath}")
        extraJvmArgs.add("-Dbiomic.worldgenBenchmarkCenterChunkX=${worldgenBenchmarkCenterChunkX.get()}")
        extraJvmArgs.add("-Dbiomic.worldgenBenchmarkCenterChunkZ=${worldgenBenchmarkCenterChunkZ.get()}")
        extraJvmArgs.add("-Dbiomic.worldgenBenchmarkRadius=${worldgenBenchmarkRadius.get()}")
        if (worldgenBenchmarkJfr.isPresent) {
            extraJvmArgs.add(
                "-XX:StartFlightRecording=filename=${worldgenBenchmarkJfr.get()},settings=profile,dumponexit=true",
            )
        }
        extraArgs.addAll("--universe", root.absolutePath, "--world", "world", "--port", "25576")
        doFirst {
            root.mkdirs()
            root.resolve("world").deleteRecursively()
            root.resolve("config").deleteRecursively()
            root.resolve("logs").deleteRecursively()
            root.resolve("crash-reports").deleteRecursively()
            root.resolve("result.txt").delete()
            root.resolve("eula.txt").writeText("eula=true\n")
            root.resolve("server.properties").writeText(
                """
                allow-flight=true
                difficulty=0
                gamemode=1
                generate-structures=true
                level-name=world
                level-seed=42
                level-type=biomic
                max-players=1
                online-mode=false
                server-ip=127.0.0.1
                server-port=25576
                spawn-animals=false
                spawn-monsters=false
                spawn-npcs=false
                view-distance=12
                """.trimIndent() + "\n",
            )
            root.resolve("config/etfuturum").mkdirs()
            root.resolve("config/etfuturum/experiments.cfg").writeText(
                """
                "experimental features -- tread carefully" {
                    B:enableDripstone=true
                    B:enableMangroveBlocks=true
                    B:enableMossAzalea=true
                }
                """.trimIndent() + "\n",
            )
            root.resolve("config/wawelauth").mkdirs()
            root.resolve("config/wawelauth/server.json").writeText("{\"wawelAuthEnabled\":false}\n")
        }
    } else {
        doFirst("biomicConfigureAuditViewDistance") {
            val propertiesFile = normalServerRoot.get().file("server.properties").asFile
            propertiesFile.parentFile.mkdirs()
            val current = if (propertiesFile.isFile) propertiesFile.readText() else ""
            val viewDistanceLine = Regex("(?m)^\\s*view-distance\\s*=.*$")
            val updated = if (viewDistanceLine.containsMatchIn(current)) {
                viewDistanceLine.replace(current, "view-distance=$auditViewDistance")
            } else {
                current + (if (current.isEmpty() || current.endsWith("\n")) "" else "\n") +
                    "view-distance=$auditViewDistance\n"
            }
            if (updated != current) {
                propertiesFile.writeText(updated)
                logger.lifecycle("Configured Biomic audit server view distance to $auditViewDistance")
            }
        }
    }
}

val worldgenBenchmark = tasks.register<Exec>("worldgenBenchmark") {
    group = "verification"
    description = "Generates the fixed seed-42 vanilla spawn window and records cold Biomic worldgen timings"
    workingDir(project.projectDir)
    val report = worldgenBenchmarkResult.get().asFile
    val benchmarkCommand = mutableListOf(
        layout.projectDirectory.file("gradlew").asFile.absolutePath,
        "runServer25",
        "-PbiomicWorldgenBenchmark=true",
        "--no-configuration-cache",
        "--no-daemon",
    )
    if (worldgenBenchmarkJfr.isPresent) {
        benchmarkCommand.add("-PbiomicBenchmarkJfr=${worldgenBenchmarkJfr.get()}")
    }
    benchmarkCommand.add("-PbiomicBenchmarkCenterChunkX=${worldgenBenchmarkCenterChunkX.get()}")
    benchmarkCommand.add("-PbiomicBenchmarkCenterChunkZ=${worldgenBenchmarkCenterChunkZ.get()}")
    benchmarkCommand.add("-PbiomicBenchmarkRadius=${worldgenBenchmarkRadius.get()}")
    if (profileExpressions.get()) {
        benchmarkCommand.add("-PbiomicProfileExpressions=true")
    }
    if (profileTerraScripts.get()) {
        benchmarkCommand.add("-PbiomicProfileTerraScripts=true")
    }
    if (profileFeatures.get()) {
        benchmarkCommand.add("-PbiomicProfileFeatures=true")
    }
    commandLine(benchmarkCommand)
    doLast {
        check(report.isFile && report.readText().startsWith("PASS\n")) {
            "Worldgen benchmark did not produce a passing report at $report"
        }
        logger.lifecycle(report.readText().trimEnd())
    }
}
