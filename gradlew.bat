@echo off
setlocal
set DIRNAME=%~dp0
set JAVA_EXE=java.exe
if defined JAVA_HOME set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
set CLASSPATH=%DIRNAME%gradle\wrapper\gradle-wrapper.jar
"%JAVA_EXE%" -Xmx4096m -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
endlocal
