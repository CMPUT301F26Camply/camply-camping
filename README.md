# Camply

Camply is a campsite reservation application developed for CMPUT 301 at the University of Alberta.

This README is a work in progress and will be updated as the project develops. Additional project information, documentation, and setup instructions will be added as they become available.

## AI Usage Disclosure

This section documents the use of generative AI tools during the development of Camply. Usage is recorded by project part and deliverable, including the tools used, their purpose, and the extent of their contribution.


<details>
<summary><strong>Project Part 4</strong></summary>
  
**Contributor:** Nguyen Hoang Long Dinh	(`nguyenh2-debug`)   
**AI Provider:** OpenAI   
**Tool:** Codex   
**Date Used:** November 24, 2026   

#### Owner Past Reservations (US 02.07.02)

- **Purpose:** Assisted with implementing category filtering on the owner campsite overview, matching reservation counts, and preserving filters through reservation navigation.
- **Extent of Use:** Generated changes to the reservation controller, model, and Compose view, plus unit and UI tests for past reservations, empty categories, and navigation.

#### Owner Reservations and Reservation Messaging

- **Stories:** US 02.07.03, US 02.08.01, US 02.12.01, US 01.14.01, and US 03.12.01.
- **Purpose:** Used OpenAI Codex in November 2026 to support owner reservation filtering and rejection, two-way reservation messaging, and read-only admin message logs.
- **Extent of Use:** Assisted with Kotlin controllers, models, repositories, and Compose views, Firestore rules for message access, and unit and UI tests. The cancelled-reservation story reused the reservation category filters, with dedicated UI coverage.

**Contributor:** Filip Kosmajac (`fkosmajac`)  
**AI Provider:** OpenAI  
**Tool:** ChatGPT 6  
**Date Used:** November 2026

#### Part 4 Implementation Planning and Documentation

- **Initial Prompt:** "Given all of our 66 user stories and the previous Halfway Implementation Plan (these stories have been completed, in the halfway plan), I would like to create similar documentation that encompasses all 66 user stories, identifies what has already been implemented, and outlines the remaining work, dependencies, and implementation approach/order for the final project submission"
- **Follow-up Prompts:** Multiple follow-up prompts were used to further review and refine the plan, including verifying user story coverage, reviewing dependencies, adjusting the proposed implementation order, identifying potential gaps, and improving the organization and formatting of the documentation.
- **Purpose:** Used ChatGPT to assist with developing and refining the Part 4 Implementation Plan. This included reviewing the remaining user stories, checking dependencies and alignment with project requirements, and organizing the proposed implementation approach into a Markdown document.
- **Extent of Use:** Moderate assistance with reviewing, organizing, and refining the implementation plan and its documentation. ChatGPT provided feedback and suggested revisions, which were evaluated and incorporated where appropriate. Final implementation decisions, story assignments, and documentation were reviewed and approved by the team.

#### UI Redesign and Shared Component Implementation

- **Prompts and References:** Multiple prompts and follow-up discussions were used throughout the initial UI redesign and its continued development. The relevant prompts, purposes, and extent of AI assistance are documented directly in the following source files:
  - [CamplyComponents.kt](Code/app/src/main/java/com/example/camply/shared/view/CamplyComponents.kt): Redesign of shared Jetpack Compose components, including buttons, cards, text fields, and reusable selection inputs.
  - [Theme.kt](Code/app/src/main/java/com/example/camply/ui/theme/Theme.kt): Material 3 theme redesign, including four color presets and updated light/dark color schemes.
  - [CampsiteBrowseView.kt](Code/app/src/main/java/com/example/camply/campsite/view/CampsiteBrowseView.kt): Visual updates to campsite browsing, search results, filters, dropdowns, and layout spacing.
  - [CampsiteSummaryCard.kt](Code/app/src/main/java/com/example/camply/campsite/view/CampsiteSummaryCard.kt): Redesign of campsite summary cards, including promotional image previews, placeholders, and updated styling.
  - [CampsiteOwnerView.kt](Code/app/src/main/java/com/example/camply/user/view/CampsiteOwnerView.kt): Redesign of the campsite owner dashboard, management action cards, and campsite listings.
  - [CampsiteFormView.kt](Code/app/src/main/java/com/example/camply/campsite/view/CampsiteFormView.kt): Redesign of campsite registration and editing, including promotional photo management, cover-photo selection, validation feedback, rules and cancellation policies, and interactive campsite location selection.
  - [CampsiteDetailView.kt](Code/app/src/main/java/com/example/camply/campsite/view/CampsiteDetailView.kt): Redesign of the campsite details screen, including photo display, saved map location, campsite information, and owner editing controls.
  - [CampsiteMap.kt](Code/app/src/main/java/com/example/camply/campsite/view/CampsiteMap.kt): Assistance with researching mapping options and implementing a reusable MapLibre map component for campsite location selection and display.
  - [CampsitePhotoGallery.kt](Code/app/src/main/java/com/example/camply/campsite/view/CampsitePhotoGallery.kt): Implementation assistance for a reusable photo gallery with thumbnails, Firestore image loading, full-screen viewing, zoom gestures, and photo navigation.
  - [CampsiteImageService.kt](Code/app/src/main/java/com/example/camply/campsite/service/CampsiteImageService.kt): Assistance with campsite image processing, including bitmap decoding, resizing, JPEG compression, image orientation, and error handling.
- **Testing Assistance:** Additional AI assistance is documented in the following test files:
  - [CampsiteImageServiceTest.kt](Code/app/src/androidTest/java/com/example/camply/campsite/service/CampsiteImageServiceTest.kt): Assistance with Android instrumented tests for image compression, dimensions, aspect ratios, orientation, and error handling.
  - [CampsiteFormTest.kt](Code/app/src/androidTest/java/com/example/camply/CampsiteFormTest.kt): Assistance with a Compose UI test helper for locating editable text fields inside tagged shared input components.
- **Purpose:** Used ChatGPT to assist with continuing the Camply UI redesign, improving visual consistency across existing screens, and implementing selected campsite-related functionality. This included refining reusable Jetpack Compose components, updating Material 3 styling, improving campsite registration and detail screens, integrating campsite photo management and galleries, and supporting interactive map location selection.
- **Extent of Use:** Substantive assistance with UI design, Kotlin/Jetpack Compose implementation, campsite image processing, map integration, and code refinement. Additional assistance was used for specific test implementations and troubleshooting, as documented in the corresponding test files. Suggested implementations were reviewed, adjusted, and manually tested before incorporation into the project. The overall redesign direction was discussed and agreed upon by the team.

#### Profile Image Management (US 01.08.02)

- **Prompts and References:** AI assistance was used for specific portions of the profile image implementation. The relevant prompts and usage declarations are documented directly in the following source files:
  - [ProfileImageService.kt](Code/app/src/main/java/com/example/camply/user/service/ProfileImageService.kt): Assistance with reading selected images, bitmap decoding, resizing while preserving aspect ratio, JPEG compression, and error handling.
  - [UserProfileView.kt](Code/app/src/main/java/com/example/camply/user/view/UserProfileView.kt): Assistance with setting up Android's photo picker in Jetpack Compose and handling the selected image URI.
- **Purpose:** Used ChatGPT to assist with image processing and integrating Android's photo picker into the profile screen.
- **Extent of Use:** Substantive assistance with implementing the image processing service, including resizing, compression, and error handling. Additional assistance was used specifically for the photo picker launcher and selected image handling in `UserProfileView.kt`. The suggested code was reviewed and adjusted before incorporation into the project.

</details>

<details>
<summary><strong>Project Part 3</strong></summary>

**Contributor:** Ayesha Junaid (`junaidayesha92-code`)  
**AI Provider:** Anthropic   
**Tool:** Claude  
**Date Used:** November 2026

#### Campsite UI

- **Prompt:**  Using the description and user story images I've provided, give me a step by step guide to implement the UI portion of the user stories. follow the colour schemes ive provided
- **Purpose:** Used Claude to get help implementing UI screens for US 01.01.01 - 01.01.05
- **Extent of Use:** Moderate assistance with following a step by step guide to implement the UI

</details>

<details>
<summary><strong>Project Part 2</strong></summary>   
   
**Contributor:** Filip Kosmajac (`fkosmajac`)  
**AI Provider:** OpenAI  
**Tool:** ChatGPT  
**Date Used:** October 2026

#### Product Backlog

- **Purpose:** Used ChatGPT to assist with reviewing and improving user story interpretations, acceptance criteria, and relationships between campsite owner and administrator requirements.
- **Extent of Use:** Moderate assistance with reviewing and revising written backlog content. Resulting content was reviewed and adjusted before being included in GitHub Issues.

#### Halfway Checkpoint Planning and Documentation

- **Purpose:** Used ChatGPT to review an initial halfway checkpoint implementation plan, including the proposed user story selection and development process. ChatGPT assisted with cross referencing story dependencies, checking alignment with project requirements, identifying potential gaps or inconsistencies, and suggesting refinements to the proposed scope and implementation order. It also assisted with organizing, editing, and formatting the Halfway Implementation Plan in Markdown.
- **Extent of Use:** Moderate assistance with reviewing and refining the initial plan and preparing its documentation. ChatGPT provided feedback and suggested improvements, which were evaluated and incorporated where appropriate. The final story selection, implementation approach, and documentation were reviewed and approved by the team.

**Contributor:** Laviza Nofal (`no-viza`)  
**AI Provider:** Figma  
**Tool:** Figma's AI assistant   
**Date Used:** October 2026

#### UI Mockups

- **Purpose:** Used Figma's AI bot to assist with minor formatting issues during the UI design.
- **Extent of Use:** Minimal assistance to help with formatting/ alignment of a few icons during the design process.
- **Prompts**:
- 1- "For the selected icon, change its border to white and make the background black"
- 2- "Replace the selected icon with the "inbox icon" that I have on the inbox screen"


**Contributor:** Khoi Nguyen (`khuivagorou`)  
**AI Provider:** OpenAI
**Tool:** ChatGPT   
**Date Used:** October 2026

#### CRC card descriptions 

- **Purpose:** Used ChatGPT to refine the wording of some CRC card descriptions for better cohesion and readability.
- **Extent of Use:** Minimal assistance with rephrasing.

</details>

Additional AI usage will be documented under the relevant project part and deliverables as the project progresses.

## References and Attributions

The following external libraries, services, and documentation were referenced or used during Camply's development.

### Maps and Location

- [MapLibre Native](https://github.com/maplibre/maplibre-native): Open source mapping library used to display interactive maps, manage campsite pins, and support map interactions. Licensed under the BSD 2-Clause License.
- [MapLibre Android SDK Documentation](https://maplibre.org/maplibre-native/android/examples/): Reference for integrating MapLibre maps into Android applications.
- [OpenFreeMap](https://openfreemap.org/): Provides the hosted map tiles and Liberty map style used by Camply.
- [OpenFreeMap Terms of Service](https://openfreemap.org/tos/): Terms governing the use of the hosted map service.
- [OpenStreetMap](https://www.openstreetmap.org/copyright): Provides the underlying geographic data, licensed under the Open Database License (ODbL). Map data is credited to OpenStreetMap contributors.
- [OpenMapTiles](https://openmaptiles.org/): Map tile schema and attribution used by the OpenFreeMap service.
- [Android Geocoder](https://developer.android.com/reference/kotlin/android/location/Geocoder): Android API used to retrieve location information from geographic coordinates.

### Image Handling and Storage

- [Android Photo Picker](https://developer.android.com/training/data-storage/shared/photopicker): Reference for selecting images from the device.
- [AndroidX ExifInterface](https://developer.android.com/reference/androidx/exifinterface/media/ExifInterface): Used to read image orientation metadata and correct image rotation during processing.
- [Firestore Blob Reference](https://firebase.google.com/docs/reference/kotlin/com/google/firebase/firestore/Blob): Reference for storing image data as binary values in Firestore documents.
