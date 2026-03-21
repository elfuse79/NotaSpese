plugins {
    kotlin("jvm")
    id("org.jetbrains.compose") version "1.6.0"
}

sourceSets {
    main {
        kotlin.srcDirs("src/jvmMain/kotlin")
        resources.srcDirs("src/jvmMain/resources")
    }
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.xerial:sqlite-jdbc:3.44.1.0")
    implementation("org.apache.pdfbox:pdfbox:2.0.29")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

compose.desktop {
    application {
        mainClass = "com.notaspese.desktop.MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi)
            packageName = "NotaSpese"
            packageVersion = "1.3.2"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

tasks.register<Copy>("copyToDesktop") {
    dependsOn("createDistributable")
    from(layout.buildDirectory.dir("compose/binaries/main/app/NotaSpese"))
    into(file("${System.getProperty("user.home")}/Desktop/NotaSpese_v1.3.2"))
    doLast {
        project.file("${destinationDir}/Avvia NotaSpese.bat").writeText("""
            @echo off
            cd /d "%~dp0"
            if exist "NotaSpese.exe" (
                start "" "NotaSpese.exe"
            ) else (
                echo Cartella errata. Eseguire: gradlew :desktop:copyToDesktop
                pause
            )
        """.trimIndent())
    }
}

tasks.register<Copy>("copyJarToDesktop") {
    dependsOn("packageUberJarForCurrentOS")
    from(layout.buildDirectory.file("compose/jars/NotaSpese-windows-x64-1.3.2.jar"))
    into(file("${System.getProperty("user.home")}/Desktop/NotaSpese_v1.3.2"))
    doLast {
        project.file("${destinationDir}/Avvia NotaSpese (JAR).bat").writeText("""
            @echo off
            cd /d "%~dp0"
            java -jar "NotaSpese-windows-x64-1.3.2.jar"
            if errorlevel 1 (
                echo.
                echo Richiesto Java 17 o superiore. Scaricalo da https://adoptium.net/
                pause
            )
        """.trimIndent())
    }
}
