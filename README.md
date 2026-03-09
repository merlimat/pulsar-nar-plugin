# Pulsar NAR Gradle Plugin

A Gradle plugin that packages projects as NAR (NiFi Archive) files, primarily used for Apache Pulsar IO connectors.

## Usage

Apply the plugin in your `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.merlimat.nar") version "0.1.0"
}

group = "org.apache.pulsar"
version = "1.0.0"

dependencies {
    // Bundled in the NAR (META-INF/bundled-dependencies/)
    implementation("com.google.guava:guava:33.4.0-jre")
    implementation("org.apache.pulsar:pulsar-io-common:4.2.0")

    // NOT bundled (equivalent to Maven provided scope)
    compileOnly("org.apache.pulsar:pulsar-io-core:4.2.0")
}
```

Run `gradle assemble` to produce `build/libs/<project-name>-<version>.nar`.

## Configuration

All settings are optional. Defaults are derived from project properties:

```kotlin
nar {
    narId.set("my-connector")                        // default: project.name
    narGroup.set("com.example")                      // default: project.group
    narVersion.set("2.0.0")                          // default: project.version
    cloneDuringInstanceClassLoading.set(true)         // default: false
    buildTag.set("v1.0.0-rc1")                       // default: git describe --always --dirty
}
```

## NAR Archive Structure

The produced `.nar` file is a ZIP archive with the following layout:

```
my-connector-1.0.0.nar
├── com/example/MyConnector.class          # compiled classes at root
├── META-INF/
│   ├── MANIFEST.MF                        # NAR manifest attributes
│   ├── bundled-dependencies/              # runtime dependency JARs
│   │   ├── guava-33.4.0-jre.jar
│   │   └── ...
│   └── services/
│       └── pulsar-io.yaml                 # connector descriptor (from resources)
```

### Manifest Attributes

| Attribute | Source |
|---|---|
| `Nar-Id` | `nar.narId` (default: `project.name`) |
| `Nar-Group` | `nar.narGroup` (default: `project.group`) |
| `Nar-Version` | `nar.narVersion` (default: `project.version`) |
| `Clone-During-Instance-Class-Loading` | `nar.cloneDuringInstanceClassLoading` (default: `false`) |
| `Build-Tag` | `nar.buildTag` (default: git describe) |
| `Build-Timestamp` | UTC timestamp at build time |

## Dependency Scoping

| Gradle Configuration | Bundled in NAR? | Maven Equivalent |
|---|---|---|
| `implementation` | Yes | `compile` |
| `runtimeOnly` | Yes | `runtime` |
| `compileOnly` | No | `provided` |
| `testImplementation` | No | `test` |

## Building the Plugin

```bash
./gradlew check
```
