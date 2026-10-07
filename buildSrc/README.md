# buildSrc

## Why is a Gradle plugin source tree living here?

`:Plugin` uses the `xyz.kyngs.libby.plugin` Gradle plugin to generate `libby.json` (the
list of libraries that Libby downloads at runtime, together with their checksums and the
Shadow relocations).

That plugin is **not installable from any public repository**:

* `repo.kyngs.xyz/gradle-plugins` only hosts the *plugin marker* for
  `xyz.kyngs.libby.plugin:1.2.1`.
* The marker points at `libby-gradle-plugin:plugin:1.2.1`, which returns **404** on
  `repo.kyngs.xyz`, Maven Central and the Gradle Plugin Portal.

The plugin therefore used to be built by hand and installed into the local
`~/.m2/repository`, which is why a clean clone failed with:

```
Could not find libby-gradle-plugin:plugin:1.2.1.
```

To make the build reproducible on any machine, the plugin sources are vendored here
verbatim and compiled by Gradle itself. `buildSrc` classes are on the classpath of every
build script, so `Plugin/build.gradle.kts` can keep using the `libby { }` extension and the
`libby(...)` dependency configuration exactly as before.

## Changes compared to upstream 1.2.1

Upstream read the relocation list straight out of the `shadowJar` task
(`ShadowPluginIntegration`). That required the Shadow classes to be visible from the same
class loader as this plugin and broke as soon as the plugin moved into `buildSrc` (or was
built against a newer Shadow): it also relied on `SimpleRelocator#getPattern()`, which
Shadow 9 removed.

The relocation patterns are now declared once by the build
(`Plugin/build.gradle.kts`, `libraryRelocations`) and handed to both Shadow and Libby, so
`libby.json` can never disagree with the shaded jar.

## License

These files are part of Libby/GradlePlugin and keep their original Mozilla Public License
2.0 headers (the same license as the rest of this project).
