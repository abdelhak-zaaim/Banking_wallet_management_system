plugins {
    id("java")
}

group = "com.wallet"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("io.github.cdimascio:dotenv-java:3.0.0")
    implementation("com.oracle.database.jdbc:ojdbc11:23.4.0.24.05")
}

tasks.test {
    useJUnitPlatform()
}