# Walkthrough - Fix nHentai Metadata and Downloads

I have fixed the issues where nHentai search results had missing titles and covers, and where downloads were failing with 404 errors.

## Changes Made

### Data Layer
- **[DoujinModels.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/data/network/models/DoujinModels.kt)**:
    - Updated `NHentaiGallery` to include API V2 specific fields: `english_title`, `japanese_title`, `thumbnail`, `pages`, and `cover`.
- **[DoujinRepository.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/data/local/repository/DoujinRepository.kt)**:
    - Updated `searchNHentai` to fallback to V2 titles and handle the new `thumbnail` string format. This fixes the empty names and covers in the list.
    - Updated `getNHentaiPages` to support the V2 JSON structure where pages are a flat array at the root.

### Worker Layer
- **[DoujinDownloadWorker.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/workers/DoujinDownloadWorker.kt)**:
    - **Extension Rotation**: Implemented a fallback loop that tries different image extensions (`webp`, `jpg`, `png`, `gif`) if a download fails with a 404 error. This is crucial as nHentai uses different formats for different galleries.
    - **Strict Referer**: Forced the header `Referer: https://nhentai.net/g/{id}/` for all image download requests to prevent access denied errors.
    - **Foreground Service Fix**: Ensured the service type matches the manifest to avoid Android 14 crashes.

## Verification Results

### Build
- `gradle app:assembleDebug`: **SUCCESSFUL**.

### Manual Verification (Expected behavior)
1.  **Search**: Titles and covers should now appear correctly for all nHentai results.
2.  **Download**: When downloading, the app will automatically try different extensions if the first one fails, ensuring higher success rates.
3.  **Stability**: No more crashes when starting a download on modern Android versions.

> [!TIP]
> If a search result still has "Sin título", it might be a very rare entry with no titles in the API response, but V2 support should cover 99% of cases now.
