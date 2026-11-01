pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name = "msc"
include(":app")

if (providers.gradleProperty("msc.embedClash").getOrElse("false").toBoolean()) include(":embedded-clash")
