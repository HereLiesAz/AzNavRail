plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":aznavrail-annotation"))
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
}
