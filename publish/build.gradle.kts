plugins {
    `maven-publish`
    `java-platform`
}
val javaProjects: Collection<Project> by rootProject.extra


rootProject.allprojects {
    publishing {
        publications {
            repositories {
                maven {
                    name = "nexus"
                    url = uri("https://repo.spliterash.ru/" + rootProject.name)
                    credentials {
                        username = findProperty("SPLITERASH_NEXUS_USR")?.toString()
                        password = findProperty("SPLITERASH_NEXUS_PSW")?.toString()
                    }
                }
                maven {
                    name = "folder"
                    url = uri(layout.buildDirectory.get().dir("repo"))
                }
            }
        }
    }
}

val currentProject = project;

configure(javaProjects) {
    if (!project.file("src").isDirectory)
        return@configure

    val mavenArtifactId = rootProject.name + project.path
        .replace(":", "-")

    currentProject.dependencies.constraints.api(project)

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])

                artifactId = mavenArtifactId
                groupId = "ru.spliterash"
            }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("bom") {
            from(components["javaPlatform"])

            artifactId = rootProject.name + "-bom"
            groupId = "ru.spliterash"
        }
    }
}