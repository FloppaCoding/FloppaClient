import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.architectury.pack200.java.Pack200Adapter
import net.fabricmc.loom.task.RemapJarTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    kotlin("jvm") version "1.8.10"
    // This is for creating a documentation from the documentation comments. Use it with the dokkaHtml gradle task
    id("org.jetbrains.dokka") version "1.7.20"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("gg.essential.loom") version "0.10.0.+"
    id("dev.architectury.architectury-pack200") version "0.1.3"
    idea
    java
    `maven-publish`
}

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:1.7.10")
    }
}

// This variable determine the filename of the produced jar file.
version = "1.0.3-0.2"
group = "floppaclient"

repositories {
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://repo.sk1er.club/repository/maven-public")
}

val packageLib by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")

    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    compileOnly("org.spongepowered:mixin:0.8.5")

    // Essentials is still a dependency because it is used for the tweaker and to provide external libraries.
    packageLib("gg.essential:loader-launchwrapper:1.1.3")
    implementation("gg.essential:essential-1.8.9-forge:3662")

    // For scanning self registering modules packaged within the mod. -- Removed!
//    packageLib("org.reflections:reflections:0.10.2")
}

sourceSets {
    main {
        output.setResourcesDir(file("${buildDir}/classes/kotlin/main"))
    }
}

loom {
    launchConfigs {
        getByName("client") {
            property("mixin.debug", "true")
            property("asmhelper.verbose", "true")
            arg("--tweakClass", "floppaclient.tweaker.FloppaClientTweaker")
            arg("--mixin", "mixins.floppaclient.json")
        }
    }
    forge {
        pack200Provider.set(Pack200Adapter())
        mixinConfig("mixins.floppaclient.json")
    }
    mixin {
        defaultRefmapName.set("mixins.floppaclient.refmap.json")
    }
}

tasks {
    processResources {
        inputs.property("version", project.version)
        inputs.property("mcversion", "1.8.9")

        filesMatching("mcmod.info") {
            expand(mapOf("version" to project.version, "mcversion" to "1.8.9"))
        }
        dependsOn(compileJava)
    }
    named<Jar>("jar") {
        manifest.attributes(
            "FMLCorePluginContainsFMLMod" to true,
            "FMLCorePlugin" to "floppaclient.forge.FMLLoadingPlugin",
            "ForceLoadAsMod" to true,
            "MixinConfigs" to "mixins.floppaclient.json",
            "ModSide" to "CLIENT",
            "TweakClass" to "floppaclient.tweaker.FloppaClientTweaker",
            "TweakOrder" to "0"
        )
        dependsOn(shadowJar)
        enabled = false
    }
    named<RemapJarTask>("remapJar") {
        archiveBaseName.set("FloppaClient")
        input.set(shadowJar.get().archiveFile)
    }
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("FloppaClient")
        archiveClassifier.set("dev")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        configurations = listOf(packageLib)
        mergeServiceFiles()
    }
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
    // Task for custom formatted ducumentation
    register<DokkaTask>("dokkaCustomFormat") {
        pluginConfiguration<org.jetbrains.dokka.base.DokkaBase, org.jetbrains.dokka.base.DokkaBaseConfiguration> {
            // Dokka's stylesheets and assets with conflicting names will be overriden.
            // In this particular case, logo-styles.css will be overriden and Icon.png will
            // be added as an additional image asset
            // see the original assets at: https://github.com/Kotlin/dokka/tree/1.7.20/plugins/base/src/main/resources/dokka/styles
            customStyleSheets = listOf(file("documentation/dokka/logo-styles.css"))
            customAssets = listOf(file("documentation/dokka/Icon.png"))
            footerMessage = "(c) 2023 Floppa Coding"
            separateInheritedMembers = false
            // templatesDir = file("documentation/dokka/templates")
            mergeImplicitExpectActualDeclarations = false
        }
    }
    // Required by jitpack
    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = "floppacoding"
                artifactId = "floppaclient"
                version = "1.0.3-0.1"

                // A wrong components variable is overloading the correct one, so the getter is used instead.
//                from(components["java"])
                from(getComponents().getByName("java"))
            }
        }
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}