plugins {
    `java-library`
    `maven-publish`
    id("io.freefair.lombok") version "6.5.1" apply false
}
val javaProjects by extra { allprojects - project(":publish") }

allprojects {
    version = "1.0.0-SNAPSHOT"
}


configure(javaProjects) {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    repositories {
        mavenCentral()
    }
    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    dependencies {
        implementation("org.apache.logging.log4j:log4j-api:2.18.0")

        testImplementation(platform("org.junit:junit-bom:5.9.0"))
        testImplementation("org.junit.jupiter:junit-jupiter")
    }
    tasks.test {
        useJUnitPlatform()
    }

    if (!file("src").isDirectory) {
        tasks.jar {
            enabled = false;
        }
        return@configure;
    }
    apply(plugin = "io.freefair.lombok")

    java {
        withSourcesJar()
    }
}

configure(javaProjects - project(":core")) {
    dependencies {
        api(project(":core"))
    }
}