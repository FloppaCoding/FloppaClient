import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.architectury.pack200.java.Pack200Adapter
import net.fabricmc.loom.task.RemapJarTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("gg.essential.loom") version "0.10.0.+"
    id("dev.architectury.architectury-pack200") version "0.1.3"
    idea
    java
}

version = "0.1.7"
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

    packageLib("gg.essential:loader-launchwrapper:1.1.3")
    implementation("gg.essential:essential-1.8.9-forge:3662")
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
            arg("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
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
            "TweakClass" to "gg.essential.loader.stage0.EssentialSetupTweaker",
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
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}