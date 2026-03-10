plugins {
    id("com.gradleup.nmcp.settings").version("1.4.4")
}

rootProject.name = "pulsar-nar-plugin"

nmcpSettings {
    centralPortal {
        username = providers.environmentVariable("MAVEN_CENTRAL_USERNAME")
        password = providers.environmentVariable("MAVEN_CENTRAL_PASSWORD")
    }
}
