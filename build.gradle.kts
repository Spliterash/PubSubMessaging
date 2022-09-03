plugins {
    id("java-library")
    id("io.freefair.lombok") version "6.5.1"
}


version = "1.0-SNAPSHOT"

allprojects {
    apply(plugin = "java-library")
    apply(plugin = "io.freefair.lombok")

    repositories {
        mavenCentral()
    }

    dependencies{
        compileOnly("org.apache.logging.log4j:log4j-api:2.18.0")
    }
}