@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  etterlatte-fordeler startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and ETTERLATTE_FORDELER_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto execute

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\app.jar;%APP_HOME%\lib\ktorclient-auth-clientcredentials.jar;%APP_HOME%\lib\ktor-client-okhttp-jvm-1.6.1.jar;%APP_HOME%\lib\ktor-client-logging-jvm-1.6.1.jar;%APP_HOME%\lib\ktor-client-jackson-jvm-1.6.1.jar;%APP_HOME%\lib\common.jar;%APP_HOME%\lib\rapids-and-rivers-20210617121814-3e67e4d.jar;%APP_HOME%\lib\ktor-server-netty-jvm-1.5.0.jar;%APP_HOME%\lib\ktor-metrics-micrometer-jvm-1.5.0.jar;%APP_HOME%\lib\ktor-client-auth-jvm-1.6.1.jar;%APP_HOME%\lib\ktor-client-json-jvm-1.6.1.jar;%APP_HOME%\lib\ktor-client-core-jvm-1.6.1.jar;%APP_HOME%\lib\ktor-server-host-common-jvm-1.5.0.jar;%APP_HOME%\lib\ktor-server-core-jvm-1.5.0.jar;%APP_HOME%\lib\kotlinx-coroutines-jdk8-1.4.2-native-mt.jar;%APP_HOME%\lib\ktor-http-cio-jvm-1.6.1.jar;%APP_HOME%\lib\ktor-http-jvm-1.6.1.jar;%APP_HOME%\lib\ktor-network-jvm-1.6.1.jar;%APP_HOME%\lib\ktor-utils-jvm-1.6.1.jar;%APP_HOME%\lib\ktor-io-jvm-1.6.1.jar;%APP_HOME%\lib\kotlinx-coroutines-core-jvm-1.5.0-native-mt.jar;%APP_HOME%\lib\kotlin-stdlib-jdk8-1.5.10.jar;%APP_HOME%\lib\kotlin-stdlib-jdk7-1.5.10.jar;%APP_HOME%\lib\okhttp-4.6.0.jar;%APP_HOME%\lib\jackson-datatype-jsr310-2.12.1.jar;%APP_HOME%\lib\jackson-datatype-jdk8-2.12.1.jar;%APP_HOME%\lib\logstash-logback-encoder-6.6.jar;%APP_HOME%\lib\jackson-databind-2.12.1.jar;%APP_HOME%\lib\jackson-annotations-2.12.1.jar;%APP_HOME%\lib\jackson-core-2.12.1.jar;%APP_HOME%\lib\jackson-module-kotlin-2.12.1.jar;%APP_HOME%\lib\kotlin-reflect-1.5.21.jar;%APP_HOME%\lib\okio-jvm-2.6.0.jar;%APP_HOME%\lib\kotlin-stdlib-1.5.21.jar;%APP_HOME%\lib\logback-classic-1.2.3.jar;%APP_HOME%\lib\kafka-clients-2.4.0.jar;%APP_HOME%\lib\token-client-core-1.3.3.jar;%APP_HOME%\lib\slf4j-api-1.7.30.jar;%APP_HOME%\lib\annotations-13.0.jar;%APP_HOME%\lib\kotlin-stdlib-common-1.5.21.jar;%APP_HOME%\lib\logback-core-1.2.3.jar;%APP_HOME%\lib\micrometer-registry-prometheus-1.6.2.jar;%APP_HOME%\lib\netty-codec-http2-4.1.59.Final.jar;%APP_HOME%\lib\netty-transport-native-epoll-4.1.59.Final.jar;%APP_HOME%\lib\netty-transport-native-kqueue-4.1.59.Final.jar;%APP_HOME%\lib\alpn-api-1.1.3.v20160715.jar;%APP_HOME%\lib\zstd-jni-1.4.3-1.jar;%APP_HOME%\lib\lz4-java-1.6.0.jar;%APP_HOME%\lib\snappy-java-1.1.7.3.jar;%APP_HOME%\lib\micrometer-core-1.6.2.jar;%APP_HOME%\lib\simpleclient_common-0.9.0.jar;%APP_HOME%\lib\lombok-1.18.16.jar;%APP_HOME%\lib\validation-api-2.0.1.Final.jar;%APP_HOME%\lib\caffeine-2.8.8.jar;%APP_HOME%\lib\oauth2-oidc-sdk-8.32.1.jar;%APP_HOME%\lib\nimbus-jose-jwt-8.20.1.jar;%APP_HOME%\lib\netty-codec-http-4.1.59.Final.jar;%APP_HOME%\lib\netty-handler-4.1.59.Final.jar;%APP_HOME%\lib\netty-codec-4.1.59.Final.jar;%APP_HOME%\lib\netty-transport-native-unix-common-4.1.59.Final.jar;%APP_HOME%\lib\netty-transport-4.1.59.Final.jar;%APP_HOME%\lib\netty-buffer-4.1.59.Final.jar;%APP_HOME%\lib\netty-resolver-4.1.59.Final.jar;%APP_HOME%\lib\netty-common-4.1.59.Final.jar;%APP_HOME%\lib\HdrHistogram-2.1.12.jar;%APP_HOME%\lib\LatencyUtils-2.0.3.jar;%APP_HOME%\lib\simpleclient-0.9.0.jar;%APP_HOME%\lib\checker-qual-3.8.0.jar;%APP_HOME%\lib\error_prone_annotations-2.4.0.jar;%APP_HOME%\lib\jcip-annotations-1.0-1.jar;%APP_HOME%\lib\content-type-2.1.jar;%APP_HOME%\lib\json-smart-2.3.jar;%APP_HOME%\lib\lang-tag-1.4.4.jar;%APP_HOME%\lib\config-1.3.1.jar;%APP_HOME%\lib\accessors-smart-1.2.jar;%APP_HOME%\lib\asm-5.0.4.jar


@rem Execute etterlatte-fordeler
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %ETTERLATTE_FORDELER_OPTS%  -classpath "%CLASSPATH%"  %*

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable ETTERLATTE_FORDELER_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%ETTERLATTE_FORDELER_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
