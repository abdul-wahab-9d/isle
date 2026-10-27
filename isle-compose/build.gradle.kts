import org.gradle.api.publish.maven.MavenPublication
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

android {
    namespace = "io.github.abdulwahab9d.isle"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    api(project(":isle-core"))
    implementation(project(":isle-geometry"))
    implementation(project(":isle-anchor"))
    implementation(project(":isle-runtime"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
}

val isleModules = listOf(":isle-core", ":isle-geometry", ":isle-anchor", ":isle-runtime")

gradle.projectsEvaluated {
    val jars = isleModules.map { path ->
        val found = project(path).tasks.findByName("jar") as? org.gradle.jvm.tasks.Jar
        found ?: error(
            "No jar task in $path. Tasks: ${project(path).tasks.names.sorted().joinToString()}",
        )
    }
    tasks.named("bundleReleaseAar").configure {
        dependsOn(jars)
        inputs.files(jars.map { it.archiveFile })
        doLast {
            val aar = outputs.files.files.single { it.isFile && it.extension == "aar" }
            embedIsleModules(aar, jars.map { it.archiveFile.get().asFile })
        }
    }
    tasks.named("publishReleasePublicationToMavenLocal").configure {
        doLast {
            val version = project.property("VERSION_NAME").toString()
            val groupPath = project.property("GROUP_ID").toString().replace('.', '/')
            val published = file("${System.getProperty("user.home")}/.m2/repository/$groupPath/isle/$version")
            published.listFiles()?.filter { it.name.endsWith(".module") }?.forEach { it.delete() }
        }
    }
}

tasks.withType<org.gradle.api.publish.tasks.GenerateModuleMetadata>().configureEach {
    enabled = false
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(project.components["release"])
                groupId = project.property("GROUP_ID").toString()
                artifactId = "isle"
                version = project.property("VERSION_NAME").toString()
                pom.withXml {
                    val dependencies = asNode().children()
                        .filterIsInstance<groovy.util.Node>()
                        .firstOrNull { it.name().toString().endsWith("dependencies") }
                        ?: return@withXml
                    val internal = setOf("isle-core", "isle-geometry", "isle-anchor", "isle-runtime")
                    dependencies.children()
                        .filterIsInstance<groovy.util.Node>()
                        .filter { node ->
                            node.children().filterIsInstance<groovy.util.Node>().any { child ->
                                child.name().toString().endsWith("artifactId") && child.text() in internal
                            }
                        }
                        .toList()
                        .forEach { dependencies.remove(it) }
                }
            }
        }
    }
}

fun embedIsleModules(aar: File, jars: List<File>) {
    val temp = File(aar.parentFile, "${aar.name}.embedding")
    ZipFile(aar).use { source ->
        ZipOutputStream(BufferedOutputStream(FileOutputStream(temp))).use { dest ->
            val written = HashSet<String>()
            val entries = source.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.isDirectory || !written.add(entry.name)) continue
                dest.putNextEntry(ZipEntry(entry.name))
                if (entry.name == "classes.jar") {
                    dest.write(mergeIsleClasses(source.getInputStream(entry).readBytes(), jars))
                } else {
                    source.getInputStream(entry).use { it.copyTo(dest) }
                }
                dest.closeEntry()
            }
        }
    }
    if (aar.exists() && !aar.delete()) {
        temp.delete()
        error("Could not replace $aar")
    }
    if (!temp.renameTo(aar)) {
        temp.copyTo(aar, overwrite = true)
        temp.delete()
    }
}

fun mergeIsleClasses(primary: ByteArray, jars: List<File>): ByteArray {
    val merged = ByteArrayOutputStream()
    val seen = HashSet<String>()
    ZipOutputStream(merged).use { dest ->
        fun writeJar(bytes: ByteArray) {
            val tmp = Files.createTempFile("isle-classes", ".jar")
            try {
                Files.write(tmp, bytes)
                ZipFile(tmp.toFile()).use { zip ->
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (entry.isDirectory) continue
                        val name = entry.name
                        if (name.endsWith(".SF") || name.endsWith(".RSA") || name.endsWith(".DSA")) continue
                        if (!seen.add(name)) continue
                        dest.putNextEntry(ZipEntry(name))
                        zip.getInputStream(entry).use { it.copyTo(dest) }
                        dest.closeEntry()
                    }
                }
            } finally {
                Files.deleteIfExists(tmp)
            }
        }
        writeJar(primary)
        jars.forEach { writeJar(it.readBytes()) }
    }
    val required = listOf(
        "io/github/abdulwahab9d/isle/core/Tone.class",
        "io/github/abdulwahab9d/isle/geometry/LayoutIsleKt.class",
        "io/github/abdulwahab9d/isle/anchor/ResolveAnchorKt.class",
        "io/github/abdulwahab9d/isle/runtime/ReduceKt.class",
    )
    val missing = required.filterNot(seen::contains)
    if (missing.isNotEmpty()) error("Release AAR is missing ${missing.joinToString()}")
    return merged.toByteArray()
}
