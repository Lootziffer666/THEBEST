# Gradle Wrapper binary

`gradle-wrapper.jar` is intentionally not committed because this environment does not support binary files in patches.

If you want to use the checked-in wrapper scripts locally, generate/download the wrapper binary yourself before committing it:

```bash
gradle wrapper --gradle-version 8.14.4 --distribution-type bin
```

Download URL for the Gradle distribution used by this project:

https://services.gradle.org/distributions/gradle-8.14.4-bin.zip

CI does not depend on the wrapper binary; it installs Gradle 8.14.4 via `gradle/actions/setup-gradle` and runs the same verification tasks with `gradle`.
