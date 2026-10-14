// Top-level build file. Versiones de plugins declaradas explícitamente
// (sin version catalog) para que el build funcione sin libs.versions.toml.
plugins {
    id("com.android.application") version "8.6.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
