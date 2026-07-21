// Ce fichier déclare les plugins utilisés par les sous-modules sans les appliquer
// (voir app/build.gradle.kts pour l'application effective), conformément aux
// conventions recommandées par le catalogue de versions Gradle.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.hilt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}
