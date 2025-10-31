# Build Instructions for Restricted Environments

This document describes how to build the ABB Android application APK file in environments where access to Google's Maven repository may be restricted.

## Standard Build Process

### Prerequisites
- JDK 17 or higher
- Android SDK API 34
- Gradle 8.2
- Internet access to maven.google.com and dl.google.com

### Building Debug APK

```bash
./gradlew assembleDebug
```

The debug APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

### Building Release APK

```bash
./gradlew assembleRelease
```

The release APK will be generated at: `app/build/outputs/apk/release/app-release.apk`

Note: Release builds require signing configuration. See BUILDING.md for details on setting up signing.

## Building via GitHub Actions

If you encounter network restrictions preventing direct builds, you can use GitHub Actions to build the APK:

1. Push your changes to the repository
2. Go to the "Actions" tab in GitHub
3. Select the "Build APK" workflow
4. Click "Run workflow"
5. Wait for the workflow to complete
6. Download the generated APK from the workflow artifacts

The workflow configuration is located at `.github/workflows/build-apk.yml`

## Troubleshooting Network Issues

### Problem: Cannot access dl.google.com

**Symptom:**
```
Could not GET 'https://dl.google.com/dl/android/maven2/...'
> dl.google.com: No address associated with hostname
```

**Cause:**
The Android Gradle Plugin and related dependencies are hosted on Google's Maven repository at dl.google.com. Even when configuring maven.google.com, Gradle follows HTTP redirects to dl.google.com for actual file downloads.

**Solutions:**

1. **Enable access to dl.google.com** in your network/firewall configuration

2. **Use GitHub Actions** (recommended for CI/CD):
   - The `.github/workflows/build-apk.yml` workflow builds the APK in GitHub's infrastructure
   - Download the APK artifact after the workflow completes

3. **Use a pre-configured build environment**:
   - Build in an environment with full internet access
   - Use a Docker container with pre-cached dependencies
   
4. **Manual dependency caching** (advanced):
   ```bash
   # On a machine with internet access:
   ./gradlew assembleDebug --refresh-dependencies
   
   # Copy the Gradle cache to the restricted environment:
   tar -czf gradle-cache.tar.gz ~/.gradle/caches/
   # Transfer gradle-cache.tar.gz to target machine
   # Extract to ~/.gradle/caches/
   ```

## Required Network Access

The following domains must be accessible for building:

- `maven.google.com` - Google's Maven repository index
- `dl.google.com` - Google's Maven repository file server (required)
- `repo1.maven.org` - Maven Central repository
- `services.gradle.org` - Gradle distribution downloads

## Alternative Build Methods

### Using Android Studio

Android Studio typically has better network configuration options:

1. Open the project in Android Studio
2. Configure proxy settings if needed: File → Settings → Appearance & Behavior → System Settings → HTTP Proxy
3. Sync Gradle
4. Build → Build Bundle(s) / APK(s) → Build APK(s)

### Using Pre-built Docker Image

Create a Dockerfile with pre-cached dependencies:

```dockerfile
FROM openjdk:17-slim
RUN apt-get update && apt-get install -y wget unzip
# Download and setup Android SDK
# Download and cache Gradle dependencies
# ...
```

## APK Output Locations

After a successful build:

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release-unsigned.apk` (or signed version if configured)

## Verifying the Build

To verify the APK was built correctly:

```bash
# Check if APK exists
ls -lh app/build/outputs/apk/debug/app-debug.apk

# Get APK information
/path/to/android/sdk/build-tools/34.0.0/aapt dump badging app/build/outputs/apk/debug/app-debug.apk
```

## Support

For build issues, please check:
- [BUILDING.md](BUILDING.md) - Detailed build instructions
- [GitHub Issues](https://github.com/OMOCV/Android/issues) - Report problems or search for solutions
