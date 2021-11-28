enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("VERSION_CATALOGS")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://repo.pl3x.net/")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven("https://repo.viaversion.com/")
        maven("https://repo.aikar.co/content/groups/aikar/")
        maven("https://ci.lucko.me/plugin/repository/everything/")
        maven("https://repo.essentialsx.net/releases/")
        maven("https://repo.codemc.org/repository/maven-public")
        maven("https://repo.kryptonmc.org/releases")
        maven("https://repo.kryptonmc.org/snapshots")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://nexus.velocitypowered.com/repository/maven-public/")
        maven("https://repo.opencollab.dev/maven-snapshots/")
        maven("https://repo.rosewooddev.io/repository/public/")
    }
}

pluginManagement {
    includeBuild("build-logic")
    repositories {
        maven("https://papermc.io/repo/repository/maven-public/")
    }
    plugins {
        id("io.papermc.paperweight.userdev") version "1.2.0"
    }
}

rootProject.name = "TAB"

include(":api")
include(":shared")
//include(":krypton")
include(":velocity")
include(":bukkit")
include(":bukkit:nms:adapter")
include(":bukkit:nms:v1_8_R1")
include(":bukkit:nms:v1_8_R2")
include(":bukkit:nms:v1_8_R3")
include(":bukkit:nms:v1_9_R1")
include(":bukkit:nms:v1_9_R2")
include(":bukkit:nms:v1_10")
include(":bukkit:nms:v1_11")
include(":bukkit:nms:v1_12")
include(":bukkit:nms:v1_13_R1")
include(":bukkit:nms:v1_13_R2")
include(":bukkit:nms:v1_14")
include(":bukkit:nms:v1_15")
include(":bukkit:nms:v1_16_R1")
include(":bukkit:nms:v1_16_R2")
include(":bukkit:nms:v1_16_R3")
include(":bukkit:nms:v1_17")
include(":bungeecord")
include(":jar")
