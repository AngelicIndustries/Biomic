import org.gradle.api.tasks.JavaExec

tasks.withType<JavaExec>().configureEach {
    if (name.startsWith("runClient")) {
        // SDL3 otherwise installs a SIGSEGV handler ahead of HotSpot's handler on Linux.
        // JIT implicit-null checks then get re-raised by SDL and become fatal JVM crashes.
        environment("SDL_NO_SIGNAL_HANDLERS", "1")

        // Codex/IDE launchers can inherit a TTY-flavoured environment even while the user
        // has a graphical session. Without either display socket SDL falls back to KMS/DRM,
        // where its first page flip fails. Recover the current user's sockets at execution
        // time so configuration-cache entries do not capture a stale desktop session.
        doFirst("biomicRecoverDesktopEnvironment") {
            val runtimeDirectory = environment["XDG_RUNTIME_DIR"]?.toString()
            val waylandDisplay =
                runtimeDirectory
                    ?.let { java.io.File(it) }
                    ?.list()
                    ?.asSequence()
                    ?.filter { it.matches(Regex("wayland-\\d+")) }
                    ?.sorted()
                    ?.firstOrNull()

            if (environment["WAYLAND_DISPLAY"]?.toString().isNullOrBlank() && waylandDisplay != null) {
                environment("WAYLAND_DISPLAY", waylandDisplay)
                if (environment["XDG_SESSION_TYPE"]?.toString().isNullOrBlank() ||
                    environment["XDG_SESSION_TYPE"]?.toString() == "tty"
                ) {
                    environment("XDG_SESSION_TYPE", "wayland")
                }
            }

            if (environment["DISPLAY"]?.toString().isNullOrBlank()) {
                val x11Display =
                    java.io.File("/tmp/.X11-unix")
                        .list()
                        ?.asSequence()
                        ?.filter { it.matches(Regex("X\\d+")) }
                        ?.sortedBy { it.removePrefix("X").toIntOrNull() ?: Int.MAX_VALUE }
                        ?.firstOrNull()
                if (x11Display != null) {
                    environment("DISPLAY", ":${x11Display.removePrefix("X")}")
                }
            }
        }
    }

    if (name.startsWith("runServer")) {
        // WawelAuth's GUI stack is client-only. GTNH setup appends parts of the
        // runtime classpath late, so filter immediately before server launch.
        doFirst("biomicStripClientOnlyMods") {
            classpath = classpath.filter { file ->
                val name = file.name
                !name.contains("Angelica", ignoreCase = true) &&
                    !name.contains("ModularUI", ignoreCase = true) &&
                    !name.contains("Baubles-Expanded", ignoreCase = true)
            }
        }
    }
}
