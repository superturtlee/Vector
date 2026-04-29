import java.time.Instant

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktfmt)
}

ktfmt { kotlinLangStyle() }

val defaultManagerPackageName: String by rootProject.extra

android {
    namespace = defaultManagerPackageName

    defaultConfig { buildConfigField("long", "BUILD_TIME", Instant.now().epochSecond.toString()) }
    sourceSets { getByName("main") { res.srcDir("../daemon/src/main/res") } }
}

dependencies {
    implementation(libs.gson)
    implementation(libs.okhttp)

    implementation(projects.services.managerService)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    // Compose BOM (Bill of Materials) ensures all compose libraries use compatible versions
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    // Tooling dependencies (only loaded in debug mode for UI previews)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
