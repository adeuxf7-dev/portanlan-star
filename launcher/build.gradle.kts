plugins { application }
repositories { mavenCentral() }

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.formdev:flatlaf:3.5.4")
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
application { mainClass.set("dev.portolan.launcher.Main") }