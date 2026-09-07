plugins {
    kotlin("jvm") version "2.4.0"
    id("com.gradleup.shadow") version "9.6.1"
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = "com.mohammadfaizan"
version = "0.6.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.121-stable")
    implementation(kotlin("stdlib"))
}

val targetJavaVersion = 25
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks {
    runServer {
        minecraftVersion("26.2")
    }

    build {
        dependsOn(shadowJar)
    }

    jar {
        archiveClassifier.set("dev")
    }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveClassifier.set("")
        mergeServiceFiles()
        dependencies {
            include(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
        }
    }
}
