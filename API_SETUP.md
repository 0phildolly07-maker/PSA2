# API Key Setup Guide

## Google Maps API Key Configuration

To use Google Maps features in this app, you need to add your Google Maps API key to the `local.properties` file.

### Steps:

1. **Get your Google Maps API Key:**
   - Go to [Google Cloud Console](https://console.cloud.google.com/)
   - Create a new project or select an existing one
   - Enable the "Maps SDK for Android" API
   - Go to "Credentials" and create an API key
   - Copy your API key

2. **Add the API key to local.properties:**
   - Open the `local.properties` file in the root directory of the project
   - Add the following line at the end of the file:
     ```
     MAPS_API_KEY=YOUR_ACTUAL_API_KEY_HERE
     ```
   - Replace `YOUR_ACTUAL_API_KEY_HERE` with your actual API key

3. **Example:**
   ```properties
   sdk.dir=C\:\\Users\\phild\\AppData\\Local\\Android\\Sdk
   MAPS_API_KEY=AIzaSyB1234567890abcdefghijklmnopqrstuv
   ```

### Security Notes:

- ✅ The `local.properties` file is already in `.gitignore` and will NOT be committed to version control
- ✅ Your API key is now secure and won't be exposed in your repository
- ✅ Each developer can use their own API key
- ⚠️ Never commit your actual API key to version control
- ⚠️ If you accidentally commit an API key, revoke it immediately in Google Cloud Console

### Troubleshooting:

**Issue:** Build fails or app doesn't show maps
- **Solution:** Make sure you've added the `MAPS_API_KEY` line to `local.properties`
- **Solution:** Sync Gradle files (File → Sync Project with Gradle Files)
- **Solution:** Clean and rebuild the project (Build → Clean Project, then Build → Rebuild Project)

**Issue:** "YOUR_API_KEY" appears in the app
- **Solution:** You haven't added the API key to `local.properties` yet
- **Solution:** Check that the property name is exactly `MAPS_API_KEY` (case-sensitive)

### For Team Members:

When cloning this repository, each team member needs to:
1. Create their own Google Maps API key
2. Add it to their local `local.properties` file
3. Never share their API key or commit `local.properties` to git

### What Changed:

Previously, the API key was hardcoded in `AndroidManifest.xml` as plaintext. This was insecure because:
- Anyone with access to the repository could see the API key
- The key would be exposed in version control history
- It's a security risk if the repository is public

Now, the API key is:
- Stored in `local.properties` (gitignored)
- Injected at build time via Gradle
- Secure and not exposed in the codebase

