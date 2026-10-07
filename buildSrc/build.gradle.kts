plugins {
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // LibbyTask writes libby.json with nanojson. Upstream declares it as a runtime
    // dependency of the plugin, so it has to be on the build script classpath too.
    implementation("com.grack:nanojson:1.7")
}

gradlePlugin {
    plugins {
        create("libby") {
            id = "xyz.kyngs.libby.plugin"
            implementationClass = "xyz.kyngs.libby.plugin.LibbyGradlePlugin"
        }
    }
}
