import com.android.build.api.dsl.ApplicationExtension
import com.google.firebase.appdistribution.gradle.AppDistributionExtension
import mega.privacy.android.build.buildTypeMatches
import mega.privacy.android.build.getAppGitHash
import mega.privacy.android.build.getChatGitHash
import mega.privacy.android.build.getKarmaPluginPort
import mega.privacy.android.build.getNocturnTimeout
import mega.privacy.android.build.getSdkGitHash
import mega.privacy.android.build.isCiBuild
import mega.privacy.android.build.isServerBuild
import mega.privacy.android.build.nativeLibsDir
import mega.privacy.android.build.preBuiltSdkDependency
import mega.privacy.android.build.readReleaseNotes
import mega.privacy.android.build.readTesterGroupList
import mega.privacy.android.build.readTesters
import mega.privacy.android.build.readVersionCode
import mega.privacy.android.build.readVersionNameChannel
import mega.privacy.android.build.readVersionNameTag
import mega.privacy.android.build.shouldActivateGreeter
import mega.privacy.android.build.shouldActivateNocturn
import mega.privacy.android.build.shouldActivateTestLite
import mega.privacy.android.build.shouldApplyDefaultConfiguration
import mega.privacy.android.build.shouldCombineLintReports
import mega.privacy.android.build.shouldUsePrebuiltSdk


plugins {
    alias(convention.plugins.mega.android.app)
    alias(convention.plugins.mega.android.test)
    id("kotlin-parcelize")
    id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.firebase.appdistribution")
    id("jacoco")
    id("com.google.gms.google-services")
    id("de.mannodermaus.android-junit5")
    id("com.google.firebase.crashlytics")
    id("com.google.firebase.firebase-perf")
    id("androidx.baselineprofile")
}

configurations {
    jacocoAnt
}

jacoco {
    toolVersion = "0.8.11"
}

android {
    defaultConfig {
        applicationId = "mega.privacy.android.app"

        val appVersion: String by rootProject.extra
        versionName = appVersion

        if (useStaticVersion()) {
            println("Create DEBUG build using static versionCode")
            versionCode = 9999
            versionNameSuffix = "(9999_debug)"
        } else {
            println("Create NORMAL build using dynamic versionCode")
            versionCode = readVersionCode()
            versionNameSuffix =
                "${readVersionNameChannel()}(${readVersionCode()}${readVersionNameTag()})(" +
                        "${getAppGitHash(project)})"
        }

        buildConfigField("String", "USER_AGENT", "\"MEGAAndroid/${versionName}_${versionCode}\"")
        buildConfigField("boolean", "ACTIVATE_GREETER", "${shouldActivateGreeter(project)}")
        buildConfigField("boolean", "ACTIVATE_NOCTURN", "${shouldActivateNocturn(project)}")
        buildConfigField("long", "NOCTURN_TIMEOUT", "${getNocturnTimeout(project)}")
        buildConfigField("int", "KARMA_PLUGIN_PORT", "${getKarmaPluginPort(project)}")
        resValue("string", "app_version", "\"${versionName}${versionNameSuffix}\"")

        val megaSdkVersion: String by rootProject.extra
        resValue("string", "sdk_version", "\"${getSdkGitHash(megaSdkVersion, project)}\"")
        resValue("string", "karere_version", "\"${getChatGitHash(megaSdkVersion, project)}\"")

        testInstrumentationRunner = "test.mega.privacy.android.app.HiltTestRunner"

        withGroovyBuilder {
            "firebaseCrashlytics" {
                // Enable processing and uploading of native symbols to Crashlytics servers.
                // This flag must be enabled to see properly-symbolicated native
                // stack traces in the Crashlytics dashboard.
                "nativeSymbolUploadEnabled"(true)
                "unstrippedNativeLibsDir"(nativeLibsDir(project))
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            extra["enableCrashlytics"] = false
            extra["alwaysUpdateBuildId"] = false
            buildConfigField("String", "ENVIRONMENT", "\"MEGAEnv/Dev\"")
        }
        release {
            firebaseAppDistribution {
                releaseNotes = readReleaseNotes()
                groups = readTesterGroupList()
                testers = readTesters()
            }
            buildConfigField("String", "ENVIRONMENT", "\"\"")
        }

        register("qa") {
            initWith(getByName("debug"))
            isDebuggable = true
            matchingFallbacks += listOf("debug", "release")
            applicationIdSuffix = ".qa"
            buildConfigField("String", "ENVIRONMENT", "\"MEGAEnv/QA\"")
            firebaseAppDistribution {
                releaseNotes = readReleaseNotes()
                groups = readTesterGroupList()
                testers = readTesters()
            }
        }
    }

    flavorDimensions += "service"
    productFlavors {
        create("gms") {
            dimension = "service"
        }
    }

    configurations {
        implementation {
            exclude(module = "protolite-well-known-types")
            exclude(module = "protobuf-javalite")
        }
    }
    lint {
        checkReleaseBuilds = false
        if (shouldCombineLintReports()) {
            checkDependencies = true
            htmlReport = true
            htmlOutput = file("build/reports/combined.html")
        }
        abortOnError = false
        xmlOutput = file("build/reports/lint-results.xml")
    }
    namespace = "mega.privacy.android.app"
    packaging {
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }
}

project.extensions.configure<ApplicationExtension> {
    buildTypes.configureEach {
        configure<AppDistributionExtension> {
            if (name == "release") {
                appId = "1:268821755439:android:9b611c50c9f7a503"
            } else if (name == "qa") {
                appId = "1:268821755439:android:1e977881202351664e78da"
            }
        }
    }
}

applyTestLiteForTasks()

dependencies {
    // Modules
    implementation(project(":core:formatter"))
    implementation(project(":domain"))
    implementation(project(":shared:original-core-ui"))
    implementation(project(":shared:theme"))
    implementation(project(":legacy-core-ui"))
    implementation(project(":data"))
    implementation(project(":navigation"))
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")
    implementation(project(":shared:sync"))
    "baselineProfile"(project(":baselineprofile"))
    implementation(project(":liveeventbus-x"))
    implementation(project(":analytics"))
    implementation(project(":icon-pack"))
    implementation(project(":feature:sync"))
    implementation(project(":feature:devicecenter"))
    implementation(project(":shared:resources"))
    preBuiltSdkDependency(rootProject.extra)

    //Test Modules
    testImplementation(project(":core-test"))
    testImplementation(project(":core-ui-test"))

    // Jetbrains
    implementation(lib.anko.commons)
    implementation(lib.coroutines.android)
    implementation(lib.coroutines.core)
    implementation(lib.kotlin.ktx)
    implementation(lib.kotlin.stdlib)
    implementation(lib.kotlin.stdlib.jdk7)

    // Android X
    implementation(androidx.bundles.lifecycle)
    implementation(androidx.bundles.navigation)
    implementation(androidx.appcompat)
    implementation(androidx.biometric)
    implementation(androidx.camera.camera2)
    implementation(androidx.camera.view)
    implementation(androidx.camera.lifecycle)
    implementation(androidx.cardview)
    implementation(androidx.constraintlayout)
    implementation(androidx.constraintlayout.compose)
    implementation(androidx.datastore.preferences)
    implementation(androidx.emoji2)
    implementation(androidx.emojiPicker)
    implementation(androidx.exifinterface)
    implementation(androidx.fragment)
    implementation(androidx.legacy.support)
    implementation(androidx.multidex)
    implementation(androidx.palette)
    implementation(androidx.preferences)
    implementation(androidx.recyclerview)
    implementation(androidx.recyclerview.selection)
    implementation(androidx.viewpager2)
    implementation(androidx.work.ktx)
    implementation(androidx.paging)
    implementation(androidx.sqlite.ktx)

    // Compose
    implementation(platform(androidx.compose.bom))
    implementation(androidx.bundles.compose.bom)
    implementation(androidx.compose.activity)
    implementation(androidx.compose.viewmodel)
    implementation(lib.coil)
    implementation(lib.coil.gif)
    implementation(lib.coil.svg)
    implementation(lib.coil.video)
    implementation(lib.coil.compose)
    implementation(androidx.paging.compose)

    // Google
    implementation(google.gson)
    implementation(google.material)
    implementation(google.media3.exoplayer)
    implementation(google.media3.ui)
    implementation(google.flexbox)
    implementation(google.zxing)
    implementation(google.accompanist.pager)
    implementation(google.accompanist.flowlayout)
    implementation(google.accompanist.placeholder)
    implementation(google.accompanist.permissions)
    implementation(google.accompanist.navigationmaterial)
    implementation(google.accompanist.navigationanimation)
    implementation(google.accompanist.systemui)

    // Google GMS
    implementation(lib.billing.client.ktx)
    implementation(google.services.location)
    implementation(google.services.maps)
    implementation(google.maps.utils)
    implementation(google.code.scanner)
    implementation(google.install.referrer)

    // Firebase
    implementation(platform(google.firebase.bom))
    implementation(google.bundles.firebase.bom)
    implementation(google.firebase.analytics)

    // Play Core
    implementation(google.play.core)
    implementation(google.play.core.ktx)

    // protobuf-java for tombstone debug
    implementation(google.protobuff)

    // Hilt
    implementation(google.hilt.android)
    implementation(androidx.hilt.work)
    implementation(androidx.hilt.navigation)

    if (shouldApplyDefaultConfiguration(project)) {
        apply(plugin = "dagger.hilt.android.plugin")

        kapt(google.hilt.android.compiler)
        kapt(androidx.hilt.compiler)
        kaptTest(google.hilt.android.compiler)
    }

    // RX
    implementation(lib.bundles.rx)

    // Fresco
    implementation(lib.bundles.fresco)
    implementation(lib.facebook.inferannotation)
    implementation(files("src/main/libs/fresco-zoomable.aar"))

    // Retrofit
    implementation(lib.retrofit)
    implementation(lib.retrofit.gson)

    // Logging
    implementation(lib.bundles.logging)

    // Other libs
    implementation(lib.bannerviewpager)
    implementation(lib.parallaxscroll)
    implementation(lib.vdurmont.emoji)
    implementation(lib.code.scanner)
    implementation(lib.stickyheader)
    implementation(lib.shimmerlayout)
    implementation(lib.collapsingtoolbar)
    implementation(lib.namedregexp)
    implementation(lib.blurry)
    implementation(lib.documentscanner)
    implementation(lib.simplestorage)
    implementation(lib.compose.state.events)
    implementation(testlib.hamcrest)
    implementation(lib.mega.analytics)
    implementation(lib.kotlin.serialisation)

    // Debug
    debugImplementation(lib.nocturn)
    debugImplementation(lib.xray)

    if (!shouldUsePrebuiltSdk()) {
        implementation(files("../sdk/src/main/jni/megachat/webrtc/libwebrtc.jar"))
    }

    // Kotlin + coroutines
    // Java Code Coverage
    jacocoAnt("org.jacoco:org.jacoco.ant:0.8.11:nodeps")

    // Testing dependencies
    testImplementation(testlib.bundles.unit.test)
    testImplementation(lib.bundles.unit.test)
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.truth.ext)
    testImplementation(testlib.arch.core.test)
    testImplementation(testlib.test.core.ktx)
    testImplementation(testlib.junit.test.ktx)
    testImplementation(testlib.espresso.contrib) {
        exclude(group = "org.checkerframework", module = "checker")
        exclude(module = "protobuf-lite")
    }
    testImplementation(google.hilt.android.test)
    testImplementation(androidx.work.test)
    testImplementation(testlib.compose.junit)
    testImplementation(androidx.navigation.testing)

    //jUnit 5
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)

    androidTestImplementation(platform(androidx.compose.bom))
    androidTestImplementation(testlib.junit.test.ktx)
    androidTestImplementation(testlib.truth)
    androidTestImplementation(testlib.espresso)
    androidTestImplementation(google.hilt.android.test)
    androidTestImplementation(testlib.mockito)
    androidTestImplementation(testlib.mockito.kotlin)
    androidTestImplementation(testlib.mockito.android)
    androidTestImplementation(testlib.espresso.contrib) {
        exclude(group = "org.checkerframework", module = "checker")
        exclude(module = "protobuf-lite")
    }
    androidTestImplementation(testlib.espresso.intents)
    androidTestImplementation(testlib.compose.junit)

    kaptAndroidTest(google.hilt.android.compiler)
    debugImplementation(androidx.fragment.test)
    debugImplementation(testlib.compose.manifest)
    debugImplementation(testlib.test.monitor)

    // Live Data testing
    testImplementation(testlib.jraska.livedata.test)
    testImplementation(testlib.coil.test)

    //QA
    "qaImplementation"(google.firebase.app.distribution)
    "qaImplementation"(testlib.compose.manifest)

    lintChecks(project(":lint"))
}

tasks.register("instrumentClasses") {
    dependsOn("compileGmsDebugSources")
    val outputDir = "${buildDir.path}/intermediates/classes-instrumented/gms/debug/"
    doLast {
        println("Instrumenting classes")

        val fileFilter = listOf(
            // data binding
            "android/databinding/**/*.class",
            "**/android/databinding/*Binding.class",
            "**/android/databinding/*",
            "**/androidx/databinding/*",
            "**/BR.*",
            // android
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*",
            "android/**/*.*",
            // dagger
            "**/*_MembersInjector.class",
            "**/Dagger*Component.class",
            "**/Dagger*Component\$Builder.class",
            "**/Dagger*Subcomponent*.class",
            "**/*Subcomponent\$Builder.class",
            "**/*Module_*Factory.class",
            "**/di/module/*",
            "**/*_Factory*.*",
            "**/*Module*.*",
            "**/*Dagger*.*",
            "**/*Hilt*.*",
            // kotlin
            "**/*MapperImpl*.*",
            "**/*\$ViewInjector*.*",
            "**/*\$ViewBinder*.*",
            "**/BuildConfig.*",
            "**/*Component*.*",
            "**/*BR*.*",
            "**/Manifest*.*",
            "**/*\$Lambda$*.*",
//                "**/*Companion*.*",
            "**/*Module*.*",
            "**/*Dagger*.*",
            "**/*Hilt*.*",
            "**/*MembersInjector*.*",
            "**/*_MembersInjector.class",
            "**/*_Factory*.*",
            "**/*_Provide*Factory*.*",
//                "**/*Extensions*.*",
            // sealed and data classes
            "**/*$Result.*",
            "**/*$Result$*.*",
            // adapters generated by moshi
            "**/*JsonAdapter.*",
            //entity in domain layer
            "**/domain/entity/*",
            // model in data layer
            "**/data/model/*",
        )
        val excludesPattern = fileFilter.joinToString()
        val jacocoAntConfig by configurations.jacocoAnt
        ant.withGroovyBuilder {
            "taskdef"(
                "name" to "instrument",
                "classname" to "org.jacoco.ant.InstrumentTask",
                "classpath" to jacocoAntConfig.asPath
            )
            "instrument"("destdir" to outputDir) {
                "fileset"(
                    "dir" to "${buildDir.path}/intermediates/javac/gmsDebug/compileGmsDebugJavaWithJavac/classes",
                    "excludes" to excludesPattern
                )
                "fileset"(
                    "dir" to "${buildDir.path}/tmp/kotlin-classes/gmsDebug",
                    "excludes" to excludesPattern
                )
            }
        }
        /* Add the instrumented classes to the beginning of classpath */
        tasks.named("testGmsDebugUnitTest") {
            if (hasProperty("classpath")) {
                setProperty("classpath", files(outputDir) + property("classpath") as FileCollection)
            }
        }
    }
}

tasks.register("createUnitTestCoverageReport") {
    dependsOn("instrumentClasses", "testGmsDebugUnitTest")
    val jacocoAntConfig by configurations.jacocoAnt
    doLast {
        ant.withGroovyBuilder {
            "taskdef"(
                "name" to "report",
                "classname" to "org.jacoco.ant.ReportTask",
                "classpath" to jacocoAntConfig.asPath
            )
            "report"() {
                "executiondata" {
                    ant.withGroovyBuilder {
                        "file"("file" to "${buildDir.path}/jacoco/testGmsDebugUnitTest.exec")
                    }
                }
                "structure"("name" to "Coverage") {
                    "classfiles" {
                        "fileset"("dir" to "${buildDir.path}/intermediates/javac/gmsDebug/compileGmsDebugJavaWithJavac/classes")
                        "fileset"("dir" to "${buildDir.path}/tmp/kotlin-classes/gmsDebug")
                    }
                    "sourcefiles" {
                        "fileset"("dir" to "src/main/java")
                        "fileset"("dir" to "src/test/java")
                    }
                }
                "html"("destdir" to "${buildDir.path}/coverage-report/html")
                "csv"("destfile" to "${buildDir.path}/coverage-report/coverage.csv")
                "xml"("destfile" to "${buildDir.path}/coverage-report/coverage.xml")
            }
        }
    }
}

/**
 * Gradle task for getting the app git hash
 * Run ./gradlew -q printAppGitHash
 */
tasks.register("printAppGitHash") {
    doLast {
        println(getAppGitHash(project))
    }
}

/**
 * Gradle task for getting the app version name
 * Run ./gradlew -q printAppVersionName
 */
tasks.register("printAppVersionName") {
    doLast {
        println(android.defaultConfig.versionName)
    }
}

/**
 * Gradle task for getting the pre-build SDK version
 * Run ./gradlew -q printPrebuildSdkVersion
 */
tasks.register("printPrebuildSdkVersion") {
    doLast {
        val megaSdkVersion: String by rootProject.extra
        println(megaSdkVersion)
    }
}

/**
 * Gradle task for getting the app version name channel
 * Run ./gradlew -q printAppVersionNameChannel
 */
tasks.register("printAppVersionNameChannel") {
    doLast {
        println(readVersionNameChannel())
    }
}

/**
 * Decide whether to use static version code
 */
fun useStaticVersion(): Boolean {
    val taskNames = gradle.startParameter.taskNames
    return buildTypeMatches("debug", taskNames) ||
            buildTypeMatches("lint", taskNames) ||
            buildTypeMatches("test", taskNames) ||
            (buildTypeMatches("qa", taskNames) && !isServerBuild()) ||
            (buildTypeMatches("qa", taskNames) && isCiBuild())
}

/**
 * Apply unit test lite mode if constraints met
 */
fun applyTestLiteForTasks() {
    val excludedTasks = listOf<(Task) -> Boolean>(
        { it.name.startsWith("injectCrashlytics") },
        { it.name.startsWith("kapt") && it.name.endsWith("TestKotlin") },
    )

    gradle.taskGraph.whenReady {
        for (task in allTasks) {
            if (task.name.lowercase().startsWith("test")) {
                tasks.matching { activeTask ->
                    excludedTasks.any { it(activeTask) } && shouldActivateTestLite()
                }.configureEach {
                    enabled = false
                }
            }
        }
    }
}