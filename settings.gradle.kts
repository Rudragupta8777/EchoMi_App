import java.util.Properties
import java.io.FileInputStream
import java.io.File

// Read local.properties
val localProperties = Properties()
val localPropertiesFile = File(rootDir, "local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        // 1. ADD JITPACK REPOSITORY (Fixes BlurView error)
        maven { url = uri("https://jitpack.io") }

        // 2. MAPBOX REPOSITORY (Fixes Mapbox download)
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials {
                username = "mapbox"
                // This MUST be the SECRET token (starts with sk.), not the public one
                password = localProperties.getProperty("MAPBOX_SECRET_TOKEN") ?: ""
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

rootProject.name = "EchoMi"
include(":app")