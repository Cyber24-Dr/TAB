plugins {
    id("dev.rosewood.mojang-remapper")
}

dependencies {
    implementation(projects.bukkit.nms.adapter)
    compileOnly(libs.spigot.v117) {
        artifact {
            classifier = "remapped-mojang"
        }
    }

    mojangToObf("org.spigotmc", "minecraft-server", "1.17.1", classifier = "maps-mojang", ext = "txt")
    mojangRemapped("org.spigotmc", "spigot", "1.17.1", classifier = "remapped-mojang")
    obfToRuntime("org.spigotmc", "minecraft-server", "1.17.1", classifier = "maps-spigot", ext = "csrg")
    obfRemapped("org.spigotmc", "spigot", "1.17.1", classifier = "remapped-obf")
}

tasks.build {
    dependsOn(tasks.runtimeMappedJar)
}

tasks.compileJava {
    options.release.set(16)
}
