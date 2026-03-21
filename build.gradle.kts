plugins {
    id("com.android.application") version "8.3.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("org.jetbrains.kotlin.jvm") version "1.9.22" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
}

val desktopDir = file("${System.getProperty("user.home")}/Desktop/NotaSpese_v1.3.2")
tasks.register("buildAndCopyAll") {
    dependsOn(":app:assembleRelease", ":desktop:packageUberJarForCurrentOS")
    doLast {
        desktopDir.mkdirs()
        copy {
            from(project(":app").file("build/outputs/apk/release/app-release.apk"))
            into(desktopDir)
            rename { "NotaSpese_1.3.2.apk" }
        }
        copy {
            from(project(":desktop").file("build/compose/jars/NotaSpese-windows-x64-1.3.2.jar"))
            into(desktopDir)
        }
        val exeDir = project(":desktop").file("build/compose/binaries/main/app/NotaSpese")
        if (exeDir.exists()) {
            copy { from(exeDir); into(desktopDir) }
        }
        project.file("$desktopDir/Avvia NotaSpese.bat").writeText("""
            @echo off
            cd /d "%~dp0"
            if exist "NotaSpese.exe" ( start "" "NotaSpese.exe" )
            else ( java -jar "NotaSpese-windows-x64-1.3.2.jar" )
        """.trimIndent())
        project.file("$desktopDir/Avvia NotaSpese (JAR).bat").writeText("""
            @echo off
            cd /d "%~dp0"
            java -jar "NotaSpese-windows-x64-1.3.2.jar"
            if errorlevel 1 ( echo Richiesto Java 17+ | pause )
        """.trimIndent())
    }
}
