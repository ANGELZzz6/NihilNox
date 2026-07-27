# Implementation Plan - Fix nHentai Metadata and Downloads

The issue is that nHentai is returning a different JSON structure than what our models expect, especially for the "API V2" endpoints being used. Additionally, the downloader fails on 404 errors because it doesn't handle different image extensions like the reader does.

## Proposed Changes

### [Data Layer]

#### [MODIFY] [DoujinModels.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/data/network/models/DoujinModels.kt)
- Update `NHentaiGallery` to include fields for nHentai API V2:
    - `english_title`: String? (for Search V2).
    - `japanese_title`: String? (for Search V2).
    - `thumbnail_path`: String? (mapped from `thumbnail` in Search V2).
    - `pages_v2`: List<NHentaiPageV2>? (mapped from `pages` in Details V2).
    - `cover_v2`: NHentaiImageV2? (mapped from `cover` in Details V2).
- Update `NHentaiTitle` to keep compatibility with V1.

#### [MODIFY] [DoujinRepository.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/data/local/repository/DoujinRepository.kt)
- Update `searchNHentai`:
    - Use `english_title` or `japanese_title` if the `title` object is null.
    - Handle `thumbnail` string to build a valid cover URL.
- Update `getNHentaiPages`:
    - Handle the `pages` array directly from the V2 response.
    - Correctly extract the image extension from the `path` field provided by V2.

### [Worker Layer]

#### [MODIFY] [DoujinDownloadWorker.kt](file:///C:/Users/elang/Documents/NihilNox/app/src/main/java/com/example/colorblend/workers/DoujinDownloadWorker.kt)
- **Extension Rotation**: If a download fails with 404, try other extensions (webp, jpg, png) similar to how the reader works.
- **Specific Referer**: Set `Referer: https://nhentai.net/g/{id}/` for all image requests.
- **Foreground Service Fix**: Ensure the `foregroundServiceType` is correctly applied to avoid the Android 14 crash.

## Verification Plan

### Automated Tests
- Build the app with `gradle app:assembleDebug`.

### Manual Verification
- Search on nHentai and verify titles and covers now appear in the list.
- Download a nHentai doujin and verify the progress bar moves and completion notification appears.
- Test reading a nHentai doujin offline.
