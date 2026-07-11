pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
        maven("https://maven.pkg.jetbrains.space/public/p/supabase/maven")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
        maven("https://jitpack.io")
        maven("https://maven.pkg.jetbrains.space/public/p/supabase/maven")
        maven("https://plugins.gradle.org/m2/")
    }
}
rootProject.name = "MonitorApp"
include(":app")