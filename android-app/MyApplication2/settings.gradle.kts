// /home/sreekanthsk/AndroidStudioProjects/MyApplication2/settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Repository for MPAndroidChart library
        maven { url = uri("https://jitpack.io") }
    }
}
rootProject.name = "My Application"
include(":app")
