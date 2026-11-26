plugins {
    alias(libs.plugins.android).apply(false)
    alias(libs.plugins.ksp).apply(false)
    alias(libs.plugins.detekt).apply(false)
    alias(libs.plugins.fork.compose).apply(false) // fork: Compose compiler
}
