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
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        maven {
            url = uri("https://raw.githubusercontent.com/guardianproject/gpmaven/master")
            content {
                includeGroup("info.guardianproject")
            }
        }
        maven {
            url = uri("https://jitpack.io")
            content {
                includeGroup("com.github.mhiew")
            }
        }
    }
}

rootProject.name = "UtxoPocket"
include(":app")
