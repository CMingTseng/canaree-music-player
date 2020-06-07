plugins {
    id(BuildPlugins.androidLibrary)
    id(BuildPlugins.kotlinAndroid)
    id(BuildPlugins.kotlinKapt)
    id(BuildPlugins.kotlinAndroidExtensions)
}

android {
    applyDefaults()

    defaultConfig {
        val properties = localProperties
        configField("LAST_FM_KEY" to properties.lastFmKey)
        configField("LAST_FM_SECRET" to properties.lastFmSecret)
    }

}

dependencies {
    lintChecks(project(":lint"))

    implementation(project(":core"))
    implementation(project(":domain"))

    implementation(project(":libraries:lib-image-loader"))
    implementation(project(":libraries:lib-media"))

    implementation(project(":navigation"))
    implementation(project(":features:feature-presentation-base"))

    implementation(project(":shared-android"))
    implementation(project(":shared"))

    implementation(Libraries.kotlin)
    implementation(Libraries.Coroutines.core)

    implementation(Libraries.Dagger.core)
    kapt(Libraries.Dagger.kapt)
    implementation(Libraries.Dagger.android)
    implementation(Libraries.Dagger.androidSupport)
    kapt(Libraries.Dagger.androidKapt)

    implementation(Libraries.X.core)
    implementation(Libraries.X.appcompat)
    implementation(Libraries.X.fragments)
    implementation(Libraries.X.recyclerView)
    implementation(Libraries.X.constraintLayout)
    implementation(Libraries.X.preference)
    implementation(Libraries.X.material)

    implementation(Libraries.UX.dialogs)

    implementation(Libraries.Utils.colorDesaturation)
    implementation(Libraries.Utils.scrollHelper)
    implementation(Libraries.Utils.lastFmBinding) // TODO remove this

    implementation(Libraries.Debug.timber)

    testImplementation(Libraries.Test.junit)
    testImplementation(Libraries.Test.mockito)
    testImplementation(Libraries.Test.mockitoKotlin)
    testImplementation(Libraries.Test.android)
    testImplementation(Libraries.Test.robolectric)
    testImplementation(Libraries.Coroutines.test)
}
