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
    }
}

rootProject.name = "isle"

include(
    ":isle-core",
    ":isle-geometry",
    ":isle-anchor",
    ":isle-runtime",
    ":isle-compose",
    ":sample",
)
