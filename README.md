# Camply

Camply is a campsite reservation application developed for CMPUT 301 at the University of Alberta.

This README is a work in progress and will be updated as the project develops. Additional project information, documentation, and setup instructions will be added as they become available.

## AI Usage Disclosure

This section documents the use of generative AI tools during the development of Camply. Usage is recorded by project part and deliverable, including the tools used, their purpose, and the extent of their contribution.

<details>
<summary><strong>Project Part 4</strong></summary>

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

- **Prompts and References:** Multiple prompts and follow-up discussions were used throughout the UI redesign process. The relevant prompts, purpose of AI assistance, and implementation changes are documented directly in the following source files:
  - [CamplyComponents.kt](Code/app/src/main/java/com/example/camply/shared/view/CamplyComponents.kt): Redesign of shared UI components, buttons, cards, and reusable inputs.
  - [Theme.kt](Code/app/src/main/java/com/example/camply/ui/theme/Theme.kt): Material 3 theme redesign and support for multiple color presets.
  - [CampsiteBrowseView.kt](Code/app/src/main/java/com/example/camply/campsite/view/CampsiteBrowseView.kt): Updates to campsite browsing, search results, filters, and layout styling.
  - [CampsiteSummaryCard.kt](Code/app/src/main/java/com/example/camply/campsite/view/CampsiteSummaryCard.kt):  Redesign of campsite cards, including image previews and updated styling.
  - [CampsiteOwnerView.kt](Code/app/src/main/java/com/example/camply/user/view/CampsiteOwnerView.kt): Redesign of the campsite owner dashboard and management components.
- **Purpose:** Used ChatGPT to assist with redesigning portions of the Camply UI and improving the existing reusable Jetpack Compose components. This included updating layouts, improving visual consistency, implementing additional shared components, and applying the updated Material 3 theme across existing screens.
- **Extent of Use:** Moderate assistance with UI design, Kotlin implementation, and code refinement. ChatGPT provided suggestions and implementation guidance, which were reviewed, adjusted, and tested before being incorporated into the project. The overall redesign direction was discussed and agreed upon by the team.

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
