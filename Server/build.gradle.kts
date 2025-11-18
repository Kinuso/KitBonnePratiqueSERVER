plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}
Voc
dependencies {
    implementation("commons-io:commons-io:2.21.0")
    implementation("org.apache.commons:commons-lang3:3.20.0")
    implementation("com.google.guava:guava:33.4.8-jre")
    implementation("org.json:json:20230227") // exemple, à adapter selon la version désirée
    implementation("org.apache.logging.log4j:log4j-core:2.20.0") // exemple, version à vérifier

    testImplementation(platform("org.junit:junit-bom:5.7.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}