plugins {
    id("java")
    id("java-library")
}

allprojects {
    group = "com.glance.codex"
    version = "1.0.0"

    apply(plugin = "java")
    apply(plugin = "java-library")

    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.codemc.io/repository/maven-releases/")
        maven("https://repo.triumphteam.dev/snapshots")
        maven("https://jitpack.io")
        maven("https://repo.extendedclip.com/releases/")
    }

    dependencies {
        val catalog = rootProject.the<VersionCatalogsExtension>().named("libs")
        compileOnly(catalog.findLibrary("jetbrains-annotations").get())
        compileOnly(catalog.findLibrary("lombok").get())
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
}