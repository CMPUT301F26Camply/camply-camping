# Users

### User (abstract)

*User stories: 01.09.01, 01.10.01*

| Responsibilities | Collaborators |
| --- | --- |
| • Knows its device ID, role(s), and Profile<br>• Knows whether notifications are enabled<br>• Provides shared account identity and information<br>• Can be deleted or removed | • Profile<br>• DeviceIdentityService<br>• Repository |

### Profile

*User stories: 01.07.03, 01.08.01*

| Responsibilities | Collaborators |
| --- | --- |
| • Knows name, email, phone, and profile Image<br>• Can update its personal information<br>• Supports deletion of the user's profile | • User<br>• Image |

### Entrant (interface)

*User stories: 01.02.01, 01.02.02, 01.04.03, 01.07.01, 01.13.01*

| Responsibilities | Collaborators |
| --- | --- |
| • Defines access to active and past reservations and joined lotteries<br>• Defines joining and leaving a waiting list<br>• Defines accepting or declining a lottery win<br>• Defines cancelling a reservation and rating a campsite | • Lottery<br>• WaitingList<br>• Reservation<br>• Rating<br>• SearchFilter<br>• Notification |

### CampsiteOwner (interface)

*User stories: 02.01.01, 02.03.01, 02.08.01*

| Responsibilities | Collaborators |
| --- | --- |
| • Defines access to owned campsites and the ability to send notifications<br>• Defines registering and updating campsites<br>• Defines creating and managing lottery listings<br>• Defines rejecting reservations<br>• Defines messaging entrants who have reservations | • Campsite<br>• Lottery<br>• Reservation<br>• NotificationService<br>• Message |

### Admin (extends User; implements Entrant, CampsiteOwner)

*User stories: 03.01.01 to 03.08.01, 03.11.01, 03.12.01*

| Responsibilities | Collaborators |
| --- | --- |
| • Can browse and remove campsites, profiles, images, and violating owners<br>• Can review notification and message logs<br>• Can access entrant, owner, and admin functionality through the same account | • ModerationService<br>• NotificationLog<br>• MessageLog<br>• Campsite<br>• Profile<br>• Image<br>• Lottery<br>• WaitingList<br>• Reservation |

### EntrantUser (extends User; implements Entrant)

*User stories: US 01 (entrant stories)*

| Responsibilities | Collaborators |
| --- | --- |
| • Maintains the user's entrant-related information<br>• Implements entrant operations defined by Entrant | • Lottery<br>• WaitingList<br>• Reservation<br>• Rating |

### CampsiteOwnerUser (extends User; implements CampsiteOwner)

*User stories: US 02 (owner stories)*

| Responsibilities | Collaborators |
| --- | --- |
| • Maintains the user's owned campsite information<br>• Implements campsite management operations defined by CampsiteOwner | • Campsite<br>• Lottery<br>• Reservation |

### DeviceIdentityService

*User stories: 01.09.01*

| Responsibilities | Collaborators |
| --- | --- |
| • Identifies a user by device ID (no username or password)<br>• Creates a new user record on first launch and resolves the existing one afterward | • User<br>• Repository |

# Campsite Management

### Campsite

*User stories: 02.01.02, 02.01.03, 02.01.04, 01.01.04, 01.01.05*

| Responsibilities | Collaborators |
| --- | --- |
| • Stores campsite name, description, and amenities<br>• Stores capacity, price, tags, and owner<br>• Maintains image, location, rating, and QR link<br>• Maintains availability schedule and reservation policy<br>• Supports owner updates and admin removal | • CampsiteOwner<br>• AvailabilitySchedule<br>• GeoLocation<br>• Image<br>• Rating<br>• ReservationPolicy |

### AvailabilitySchedule

*User stories: 01.04.01, 02.06.01*

| Responsibilities | Collaborators |
| --- | --- |
| • Knows available, booked, and blocked date ranges for one campsite<br>• Checks whether a range is free<br>• Marks dates as booked<br>• Releases dates on cancellation or rejection | • Campsite<br>• DateRange<br>• Reservation |

### DateRange

| Responsibilities | Collaborators |
| --- | --- |
| • Knows start and end dates<br>• Checks overlap and containment<br>• Calculates number of nights | • AvailabilitySchedule<br>• Lottery<br>• Reservation<br>• SearchFilter |

### GeoLocation

*User stories: 02.01.02*

| Responsibilities | Collaborators |
| --- | --- |
| • Knows latitude, longitude, and a display address | • Campsite<br>• MapService |

### MapService

| Responsibilities | Collaborators |
| --- | --- |
| • Displays a campsite's location on a map for entrants | • GeoLocation<br>• Campsite |

### Rating

*User stories: 01.13.01*

| Responsibilities | Collaborators |
| --- | --- |
| • Knows the entrant, campsite, and score<br>• Can be added or updated<br>• Contributes to the campsite's average rating | • Entrant<br>• Campsite |

### Image

*User stories: 02.01.02, 03.03.01, 03.06.01*

| Responsibilities | Collaborators |
| --- | --- |
| • Knows its owner, type (promo or profile), and storage URL<br>• Can be uploaded and removed | • ImageStorage<br>• Campsite<br>• Profile<br>• ModerationService |

# Lottery System

### Lottery

*User stories: 02.04.01 to 02.04.07, 01.01.06*

| Responsibilities | Collaborators |
| --- | --- |
| • Stores campsite(s) and date ranges<br>• Manages registration and booking periods<br>• Tracks number of winners and waitlist capacity<br>• Tracks lottery status (open, drawn, booking, closed)<br>• Supports owner creation and updates<br>• Accepts or rejects entrants based on lottery rules | • Campsite<br>• CampsiteOwner<br>• WaitingList<br>• LotteryDrawer<br>• DateRange |

### WaitingList

*User stories: 01.02.01, 01.02.02, 02.09.01*

| Responsibilities | Collaborators |
| --- | --- |
| • Knows its entries (entrant and status: waiting, won, lost, declined, reserved)<br>• Adds and removes entrants<br>• Enforces waitlist capacity<br>• Lists entrants for the owner | • Lottery<br>• Entrant<br>• LotteryDrawer |

### LotteryDrawer

*User stories: 02.05.01, 01.03.01, 01.03.02*

| Responsibilities | Collaborators |
| --- | --- |
| • Draws N random winners from a WaitingList when registration ends<br>• Marks the remaining entrants as losers<br>• Opens the booking period for winners<br>• Triggers win and lose notifications | • Lottery<br>• WaitingList<br>• NotificationService<br>• Scheduler |

### Scheduler

*User stories: 02.05.01*

| Responsibilities | Collaborators |
| --- | --- |
| • Runs time-based triggers (registration end, booking period end) | • LotteryDrawer<br>• Lottery |

# Booking

### Reservation

*User stories: 01.06.01, 01.06.02, 01.07.01, 02.07.01 to 02.07.03, 02.08.01*

| Responsibilities | Collaborators |
| --- | --- |
| • Stores entrant, campsite, dates, and guest count<br>• Tracks price and reservation status<br>• Records acknowledgement of campsite rules<br>• Supports entrant cancellation<br>• Supports owner rejection and related processing | • Entrant<br>• Campsite<br>• CampsiteOwner<br>• DateRange<br>• AvailabilitySchedule<br>• NotificationService |

### BookingService

*User stories: 01.04.01 to 01.04.03, 01.05.01*

| Responsibilities | Collaborators |
| --- | --- |
| • Shows a winner the available dates (and which campsite, for multi-campsite lotteries)<br>• Creates a Reservation if the dates are still free<br>• Handles decline and blocks booking if dates were taken | • Lottery<br>• WaitingList<br>• AvailabilitySchedule<br>• Reservation<br>• ReservationPolicy<br>• NotificationService |

### ReservationPolicy

*User stories: 02.11.01*

| Responsibilities | Collaborators |
| --- | --- |
| • Knows a campsite's rules and cancellation policy<br>• Records that an entrant confirmed reading them before booking | • Campsite<br>• Reservation<br>• Entrant |

### AppPolicy

*User stories: 01.11.01*

| Responsibilities | Collaborators |
| --- | --- |
| • Holds the app's policy and guidelines text for display |  |

# Communication

### Notification

*User stories: 01.03.01, 01.03.02, 01.05.01, 01.07.02*

| Responsibilities | Collaborators |
| --- | --- |
| • Knows sender, recipient, type (win, lose, confirmation, cancellation, owner message), content, and timestamp | • NotificationService<br>• User |

### NotificationService

*User stories: 01.10.01, 02.10.01*

| Responsibilities | Collaborators |
| --- | --- |
| • Creates and sends notifications, skipping users who opted out<br>• Logs every notification sent<br>• Lets owners broadcast to their waitlist | • Notification<br>• PushService<br>• NotificationLog<br>• User<br>• WaitingList |

### NotificationLog

*User stories: 02.10.02, 03.08.01*

| Responsibilities | Collaborators |
| --- | --- |
| • Stores all notifications sent<br>• Filters by owner (for the owner's history)<br>• Shows everything (for admins) | • Notification<br>• Repository<br>• CampsiteOwner<br>• Admin |

### PushService (interface; implemented by FirebasePushService)

| Responsibilities | Collaborators |
| --- | --- |
| • Delivers a notification to a device | • NotificationService |

### Message

*User stories: 02.12.01*

| Responsibilities | Collaborators |
| --- | --- |
| • Knows sender (owner), recipient (entrant), reservation, content, and timestamp | • CampsiteOwner<br>• Entrant<br>• Reservation<br>• MessageLog |

### MessageLog

*User stories: 03.12.01*

| Responsibilities | Collaborators |
| --- | --- |
| • Records all owner-to-entrant messages<br>• Lets admins review them | • Message<br>• Repository<br>• Admin |

# Search

### SearchFilter

*User stories: 01.01.02*

| Responsibilities | Collaborators |
| --- | --- |
| • Knows location, DateRange, and number of guests<br>• Tests whether a campsite matches | • Campsite<br>• DateRange<br>• GeoLocation |

### CampsiteSearchService

*User stories: 01.01.01, 01.01.03, 01.01.04*

| Responsibilities | Collaborators |
| --- | --- |
| • Searches by text<br>• Applies a SearchFilter<br>• Returns matching campsites with summary data<br>• Indicates which results have an active lottery | • SearchFilter<br>• Campsite<br>• Lottery<br>• Repository |

# Utilities

### QRCodeGenerator

*User stories: 02.02.01*

| Responsibilities | Collaborators |
| --- | --- |
| • Generates a unique QR code that links to a campsite's details | • Campsite |

### QRCodeScanner

*User stories: 01.12.01, 01.01.06*

| Responsibilities | Collaborators |
| --- | --- |
| • Scans a QR code and resolves it to a campsite ID<br>• Opens the campsite details screen where the entrant can join an active lottery | • Campsite<br>• CampsiteSearchService<br>• Lottery |

### CSVExporter

*User stories: 01.05.02, 01.07.02*

| Responsibilities | Collaborators |
| --- | --- |
| • Produces a downloadable CSV of a reservation confirmation or cancellation | • Reservation |

# Administration

### ModerationService

*User stories: 03.01.01 to 03.07.01*

| Responsibilities | Collaborators |
| --- | --- |
| • Removes campsites, profiles, images, and violating owners<br>• Cascades cleanup (removes an owner's campsites and lotteries, releases affected reservations) | • Admin<br>• Campsite<br>• Profile<br>• Image<br>• CampsiteOwner<br>• Repository |

# Persistence

### Repository (interface)

| Responsibilities | Collaborators |
| --- | --- |
| • Defines create, read, update, delete, and query operations for all model classes<br>• Supports real-time status listeners | • All model and service classes (as clients) |

### FirebaseRepository

| Responsibilities | Collaborators |
| --- | --- |
| • Implements repository operations using Firestore or Realtime Database<br>• Supports backend replacement through the Repository interface | • Repository |

### ImageStorage (interface; implemented by FirebaseImageStorage)

| Responsibilities | Collaborators |
| --- | --- |
| • Uploads, retrieves, and deletes image files | • Image |


