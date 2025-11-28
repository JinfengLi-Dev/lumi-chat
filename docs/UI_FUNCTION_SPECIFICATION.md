# Lumi-Chat UI/Function Specification

This document provides a complete specification of all UI components and functions based on the RainbowChat-Web reference screenshots. Every feature must be implemented according to these specifications.

Reference Images Location: [docs/ui-reference/](ui-reference/)

---

## Table of Contents

1. [Main Interface Layout](#1-main-interface-layout)
2. [Message Types](#2-message-types)
3. [Authentication System](#3-authentication-system)
4. [User Profile & Settings](#4-user-profile--settings)
5. [Friend System](#5-friend-system)
6. [Chat Functions](#6-chat-functions)
7. [Group System](#7-group-system)
8. [Message Actions](#8-message-actions)
9. [Special Message Types](#9-special-message-types)
10. [V5 Features](#10-v5-features)

---

## 1. Main Interface Layout

Reference: [01-main-interface-overview.jpg](ui-reference/01-main-interface-overview.jpg), [02-main-interface-overview-2.jpg](ui-reference/02-main-interface-overview-2.jpg)

### 1.1 Three-Column Layout

```
+------------------+----------------------+---------------------------+
|    LEFT SIDEBAR  |  CONVERSATION LIST   |      CHAT AREA            |
|    (User Panel)  |                      |                           |
+------------------+----------------------+---------------------------+
```

### 1.2 Left Sidebar (User Panel)

| Element | Description | Status |
|---------|-------------|--------|
| User Avatar | Circular, clickable to view/edit profile | Required |
| User Nickname | Display name with status icon | Required |
| User Signature | Personal status message (editable) | Required |
| Settings Gear Icon | Opens settings dropdown menu | Required |
| Tab Navigation | 3 tabs: Messages, Contacts, Groups | Required |
| Conversation Count | Shows "X conversations / Y unread" | Required |
| Add Button (+) | Dropdown: "Add Friend", "Create Group" | Required |
| Connection Status | Green dot = "Connected" at bottom | Required |

### 1.3 Tab Navigation Icons

| Tab | Icon | Function |
|-----|------|----------|
| Messages | Chat bubble | List of recent conversations |
| Contacts | Person icon | Friends list |
| Groups | People icon | Groups list |

### 1.4 Conversation List Item

| Element | Description |
|---------|-------------|
| Avatar | User/Group avatar (circular) |
| Name | Contact name or group name |
| Last Message Preview | Truncated last message text |
| Timestamp | Relative time (Today: HH:mm, else: MM/DD) |
| Unread Badge | Red circle with count (max 99+) |
| Pinned Indicator | Pin icon for pinned conversations |
| Muted Indicator | Mute icon for muted conversations |
| @ Mention Badge | Shows "[Someone @me]" indicator |
| Draft Indicator | "[Draft]" prefix if draft exists |

### 1.5 Chat Area Header

| Element | Description |
|---------|-------------|
| Contact/Group Name | Name with online status or member count |
| Sound Toggle | Speaker icon to toggle notification sounds |
| Fullscreen Toggle | Expand/collapse chat area |
| Close Button | X to close current chat |
| Info Panel Toggle | Opens right sidebar with details |

### 1.6 Right Info Panel (Context Panel)

For 1-on-1 chat:
- User info (avatar, name, status)
- Basic info (ID, email, registration date, last login, IP)
- Description/Bio
- Tabs: Info, Photos, Voice Messages

For Group chat:
- Group info (avatar, name, ID)
- Basic info (owner, nickname, creator, creation date)
- Group announcement (editable by owner)
- Member info section
- Buttons: Manage Members, Invite to Group
- Footer: Transfer Group, Dissolve Group (owner only)

---

## 2. Message Types

Reference: [03-message-types-supported.jpg](ui-reference/03-message-types-supported.jpg)

### 2.1 Supported Message Types

| Type | Icon | Description | Implementation |
|------|------|-------------|----------------|
| Text | - | Plain text with emoji support | Required |
| Image | Picture icon | Single/multiple images with preview | Required |
| File | Folder icon | Any file with download option | Required |
| Voice | Microphone | Audio recording with duration | Required |
| Video | Video icon | Video with thumbnail preview | Required |
| Location | Pin icon | Map location with address | Required |
| Personal Card | @ icon | Share a user's contact card | Required |
| Group Card | @ icon | Share a group's info card | Required |
| Emoji/Sticker | Smiley | Emoji picker with categories | Required |

### 2.2 Message Input Toolbar

From left to right:
1. Emoji picker (smiley face)
2. File attachment (folder)
3. Image upload (picture)
4. Voice message (microphone)
5. Video (camera - optional)
6. Location share (pin)
7. Personal card (@)
8. Clear input (eraser)

### 2.3 Message Display

| Element | Description |
|---------|-------------|
| Avatar | Sender's avatar (left for others, right for self) |
| Bubble | Message content container |
| Timestamp | Time below message |
| Read Status | Checkmarks for sent/delivered/read |
| Quick Reply | Preset quick reply buttons |
| Send Button | Blue button with dropdown for send options |

---

## 3. Authentication System

Reference: [04-login-register-forgot-password.jpg](ui-reference/04-login-register-forgot-password.jpg)

### 3.1 Login Page

| Field | Type | Validation |
|-------|------|------------|
| UID or Email | Text input | Required, valid format |
| Password | Password input | Required, min 6 chars |
| Remember Me | Checkbox | Optional |
| Login Button | Primary button | Blue, full width |
| Register Link | Text link | "No account? Register" |
| Forgot Password | Text link | "Forgot password?" |

### 3.2 Registration Page

| Field | Type | Validation |
|-------|------|------------|
| Nickname | Text input | Required, 2-20 chars |
| Email | Email input | Required, valid email |
| Password | Password input | Required, min 6 chars |
| Confirm Password | Password input | Must match password |
| Gender | Radio buttons | Male/Female |
| Terms Checkbox | Checkbox | Required to accept |
| Register Button | Primary button | Blue, full width |
| Login Link | Text link | "Already have account?" |

### 3.3 Forgot Password Page

| Field | Type | Description |
|-------|------|-------------|
| Email | Email input | Registered email address |
| Send Code Button | Button | Send verification code |
| Verification Code | Text input | 6-digit code |
| New Password | Password input | New password |
| Confirm Password | Password input | Confirm new password |
| Reset Button | Primary button | Submit reset |

---

## 4. User Profile & Settings

Reference: [05-my-profile-center.jpg](ui-reference/05-my-profile-center.jpg), [06-settings-functions.jpg](ui-reference/06-settings-functions.jpg)

### 4.1 My Profile Dialog

| Section | Fields |
|---------|--------|
| Header | Avatar (clickable to change), Nickname, Signature |
| Basic Info | ID (read-only), Email, Registration Date, Last Login, Last IP |
| Description | Bio/description text (editable) |
| Actions | Cancel, Edit Info button |

### 4.2 Edit Profile Dialog

| Field | Type | Limit |
|-------|------|-------|
| Nickname | Text input | Max 20 chars |
| Gender | Radio (Male/Female) | Required |
| Signature | Textarea | Max 60 chars |
| Description | Textarea | Max 250 chars |
| Avatar | File picker | Image files only |

### 4.3 Settings Menu (Gear Icon Dropdown)

| Option | Function |
|--------|----------|
| Personal Info | Open profile dialog |
| Change Password | Open password change dialog |
| Logout | Confirm and logout |
| About Us | App info and version |
| Help Center | Documentation/FAQ |

### 4.4 Change Password Dialog

| Field | Type | Validation |
|-------|------|------------|
| Current Password | Password | Required |
| New Password | Password | Min 6 chars, special chars allowed |
| Confirm New Password | Password | Must match |

### 4.5 About Us Dialog

| Info | Description |
|------|-------------|
| App Name | "Lumi-Chat Web" |
| Version | e.g., "7.0_b240620_pro" |
| Browser Info | Detected browser version |
| Cookie Support | true/false |
| Platform | OS detection |
| Screen Resolution | Width x Height |
| Color Depth | Bit depth |
| Copyright | Year and company |

---

## 5. Friend System

Reference: [07-add-friend-send-request.jpg](ui-reference/07-add-friend-send-request.jpg), [08-handle-friend-requests-delete.jpg](ui-reference/08-handle-friend-requests-delete.jpg)

### 5.1 Add Friend Flow

**Step 1: Search User Dialog**
| Element | Description |
|---------|-------------|
| Search Input | "UID or Email" placeholder |
| Helper Text | "Enter user ID or registered email" |
| Cancel Button | Close dialog |
| Search Button | Blue "Start Search" button |

**Step 2: User Info Dialog**
| Element | Description |
|---------|-------------|
| Avatar | User's profile picture |
| Nickname | With "Stranger" badge if not friend |
| Signature | User's status message |
| Basic Info | ID, Email, Registration Date, Last Login, Last IP |
| Description | User's bio |
| Actions | "Try Temporary Chat", "Add as Friend" buttons |

**Step 3: Add Friend Dialog**
| Element | Description |
|---------|-------------|
| Message Input | "Say something to [Name]..." placeholder |
| Character Limit | Max 100 characters |
| Cancel Button | Close dialog |
| Send Request Button | Blue button |

**Step 4: Success Confirmation**
- Checkmark icon with "Request Sent Successfully"
- Auto-close after 2 seconds

### 5.2 Friend Requests List

| Element | Description |
|---------|-------------|
| Request Item | Avatar, Name, "invites you to be friends", Timestamp |
| Accept Button | Green "Accept" button |
| Reject Button | Red "Reject" button |
| Result | Shows "Accepted" or "Rejected" confirmation |

### 5.3 Delete Friend Flow

**Step 1: Friend Info Dialog**
- Same as User Info but with "Delete Friend" option

**Step 2: Confirmation Dialog**
| Element | Description |
|---------|-------------|
| Warning Icon | Clock/warning icon |
| Message | "This will also delete all chat history with this friend. Confirm delete [Name]?" |
| Cancel Button | Close dialog |
| Confirm Delete Button | Red button |

**Step 3: Success**
- Checkmark with "Deleted Successfully"

### 5.4 Friend List View

| Element | Description |
|---------|-------------|
| Header | "Total Friends: X" |
| List Item | Avatar, Name with signature, Online indicator (green dot) |
| Click Action | Opens chat with friend |

---

## 6. Chat Functions

Reference: [09-stranger-chat.jpg](ui-reference/09-stranger-chat.jpg), [10-friend-chat.jpg](ui-reference/10-friend-chat.jpg)

### 6.1 Stranger Chat vs Friend Chat

| Feature | Stranger | Friend |
|---------|----------|--------|
| Chat Header Badge | "Stranger" label | None |
| Info Panel Title | "User Info" | "Friend Info" |
| Delete Chat | "Delete Chat" only | "Delete Chat" + "Delete Friend" |
| Add Friend Button | Shown | Hidden |
| Set Remark | Hidden | Shown |

### 6.2 Chat Message Context Menu (Right-Click)

| Option | Icon | Function |
|--------|------|----------|
| Copy Content | Copy | Copy message text to clipboard |
| Recall Message | Undo | Recall within time limit (own messages only) |
| Forward Message | Forward | Forward to another chat |
| Quote Message | Quote | Reply with quote |
| Delete Message | Trash | Delete locally |

### 6.3 Right Panel - Media Tabs

| Tab | Content |
|-----|---------|
| Info | User/Friend basic information |
| Photos | Grid of shared images with thumbnails |
| Voice Messages | List of voice messages with duration |

### 6.4 Quick Reply Feature

| Element | Description |
|---------|-------------|
| Quick Reply Buttons | Preset responses above input |
| Example 1 | "I'm busy handling something urgent" |
| Example 2 | "I'll be away for a while" |

### 6.5 Delete Chat Confirmation

| Element | Description |
|---------|-------------|
| Warning | "This will delete all chat history with this contact" |
| Cancel | Close dialog |
| Confirm | Red "Confirm Delete" button |

---

## 7. Group System

Reference: [11-create-group-chat.jpg](ui-reference/11-create-group-chat.jpg), [12-group-chat-interface.jpg](ui-reference/12-group-chat-interface.jpg), [13-group-info-owner.jpg](ui-reference/13-group-info-owner.jpg), [14-group-info-member.jpg](ui-reference/14-group-info-member.jpg)

### 7.1 Create Group Flow

**Step 1: Select Members Dialog**
| Element | Description |
|---------|-------------|
| Title | "Create Group" |
| Member List | Checkboxes with Avatar, Name, ID |
| Selected Count | "Confirm (X)" showing selected count |
| Min Members | At least 2 members required |

**Step 2: Success**
- Checkmark with "Group Created Successfully"
- Auto-navigates to new group chat

### 7.2 Group Chat Interface

| Element | Description |
|---------|-------------|
| Header | Group name with star icon, Member count (e.g., "8 members") |
| System Messages | "[User] invited [Users] to join the group" |
| Member Join | "[User] joined via [Inviter]'s group card" |
| File Sharing | File messages with download button |

### 7.3 Group Info Panel (Owner View)

| Section | Content |
|---------|---------|
| Basic Info | Group Owner (with "Me" badge), Nickname in Group, Creator, Creation Date |
| Announcement | Editable text area (owner only) |
| Member Info | "Manage Members (X)", "Invite to Group" buttons |
| Footer Actions | "Transfer Group", "Dissolve Group" |

### 7.4 Group Info Panel (Member View)

| Section | Content |
|---------|---------|
| Basic Info | Same as owner but read-only |
| Announcement | Read-only view |
| Member Info | "View Members (X)", "Invite to Group" buttons |
| Footer Actions | "Leave Group" only |

### 7.5 Manage Members Dialog (Owner)

| Element | Description |
|---------|-------------|
| Member List | Avatar, Name, ID, Role badges (Owner, Admin) |
| Remove Button | "Remove (X)" to kick selected members |
| Role Assignment | Can assign admin role |

### 7.6 Group Management Functions

| Function | Owner | Admin | Member |
|----------|-------|-------|--------|
| Edit Group Name | Yes | Yes | No |
| Edit Nickname | Yes | Yes | Yes |
| Edit Announcement | Yes | Yes | No |
| Invite Members | Yes | Yes | Yes |
| Remove Members | Yes | Yes (not owner) | No |
| Transfer Group | Yes | No | No |
| Dissolve Group | Yes | No | No |
| Leave Group | No | Yes | Yes |

### 7.7 Transfer Group Dialog

| Element | Description |
|---------|-------------|
| Title | "Select New Owner" |
| Member List | All members except current owner |
| Confirm | Select one member to transfer |

### 7.8 Dissolve Group Confirmation

| Element | Description |
|---------|-------------|
| Warning | "Once dissolved, all related records will be deleted. Confirm dissolve?" |
| Cancel | Close dialog |
| Confirm | Red "Confirm Dissolve" button |

### 7.9 Leave Group Confirmation

| Element | Description |
|---------|-------------|
| Warning | "Once you leave, all related records will be deleted. Confirm leave?" |
| Cancel | Close dialog |
| Confirm | "Confirm Leave" button |

---

## 8. Message Actions

Reference: [18-message-recall.jpg](ui-reference/18-message-recall.jpg), [19-message-forward.jpg](ui-reference/19-message-forward.jpg), [20-message-quote.jpg](ui-reference/20-message-quote.jpg)

### 8.1 Message Recall

| Aspect | Specification |
|--------|---------------|
| Time Limit | 2 minutes after sending |
| Scope | Both parties see "Message recalled" |
| UI | Loading spinner during recall |
| Result | Gray "[Name] recalled a message" text |

### 8.2 Message Forward

**Forward Dialog**
| Element | Description |
|---------|-------------|
| Tabs | "Friends" / "Groups" toggle |
| List | Avatar, Name, ID/Creation date |
| Selection | Single select with checkmark |
| Confirm | "Confirm" button |

**Forward Result**
- Checkmark with "Forward Successful"
- Message appears in target chat

### 8.3 Message Quote/Reply

| Element | Description |
|---------|-------------|
| Quote Block | Gray background with original message preview |
| Quote Types | Can quote: Text, Image, File, Voice, Video, Location, Personal Card, Group Card |
| Voice Quote | Shows playback in modal dialog |
| Auto-Recall | If original message is recalled, quote shows "[Content has been recalled]" |

---

## 9. Special Message Types

Reference: [15-send-personal-card.jpg](ui-reference/15-send-personal-card.jpg), [16-send-group-card.jpg](ui-reference/16-send-group-card.jpg), [17-send-location.jpg](ui-reference/17-send-location.jpg)

### 9.1 Personal Card Message

**Send Flow**
1. Click @ icon in toolbar
2. "Select Friend" dialog opens
3. Select one friend from list
4. Confirm to send card

**Card Display**
| Element | Description |
|---------|-------------|
| Avatar | Friend's avatar |
| Name | Friend's nickname |
| UID | User ID |
| Label | "Personal Card" |
| Click Action | Opens user info dialog with "Add as Friend" option |

### 9.2 Group Card Message

**Send Flow**
1. Click @ icon (long press or context menu)
2. "Select Group" dialog opens
3. Select one group from list
4. Confirm to send card

**Card Display**
| Element | Description |
|---------|-------------|
| Avatar | Group avatar |
| Name | Group name |
| Group ID | Group identifier |
| Label | "Group Card" |
| Click Action | Opens group invitation dialog |

**Group Invitation Dialog**
| Element | For Non-Member | For Member |
|---------|----------------|------------|
| Group Info | Name, ID, Owner, Creator, Creation Date | Same |
| Announcement | Group announcement preview | Same |
| Action Button | "Join Group" | "Enter Group" |
| Result | "Joined Successfully" notification | Navigate to group |

### 9.3 Location Message

**Send Flow**
1. Click location pin icon
2. Map dialog opens with current location
3. Drag to select location or search
4. Location name auto-populated
5. Click send button

**Location Display**
| Element | Description |
|---------|-------------|
| Map Preview | Static map thumbnail |
| Location Name | Place name |
| Address | Full address |
| Click Action | Opens full map view |

**Full Map View**
| Element | Description |
|---------|-------------|
| Map | Full interactive map |
| Location Pin | Red marker at location |
| Address Bar | Location name and full address |
| Open in Maps | Button to open in external map app |

---

## 10. V5 Features

Reference: [21-mention-function.jpg](ui-reference/21-mention-function.jpg), [22-v5-new-features.jpg](ui-reference/22-v5-new-features.jpg)

### 10.1 @ Mention Function

**Trigger**
- Type "@" in group chat input
- Opens member selection dialog

**Selection Dialog**
| Element | Description |
|---------|-------------|
| Title | "Select people to @ (Multiple)" |
| Special Option | "@All" (Owner/Admin only) |
| Member List | Avatar, Name, ID with checkboxes |
| Confirm | "Confirm (X)" with count |

**Display**
- @mentions highlighted in blue in message
- Mentioned users see "[Someone @me]" in conversation list

### 10.2 Conversation Types (V5)

| Type | Header Display | Info Panel Title |
|------|---------------|------------------|
| System Message | "System Message" with Admin badge | N/A |
| Stranger Chat | Name with "Stranger" badge | "User Info" |
| Friend Chat | Name only | "Friend Info" |
| Group Chat | Name with member count | "Group Info" |

### 10.3 Multi-Device Indicators

| Indicator | Description |
|-----------|-------------|
| Online Status | Green dot for online |
| Last Seen | "Last login: [DateTime]" |
| Multiple Sessions | Shows active device count |

### 10.4 Message Status Indicators

| Status | Icon | Description |
|--------|------|-------------|
| Sending | Spinner | Message being sent |
| Sent | Single check | Server received |
| Delivered | Double check | Recipient received |
| Read | Blue double check | Recipient read |
| Failed | Red exclamation | Send failed, tap to retry |

---

## Implementation Checklist

### Phase 1: Core Layout
- [ ] Three-column responsive layout
- [ ] Left sidebar with user panel
- [ ] Conversation list with all indicators
- [ ] Chat area with message display
- [ ] Right info panel (collapsible)

### Phase 2: Authentication
- [ ] Login page
- [ ] Registration page
- [ ] Forgot password flow
- [ ] Session management

### Phase 3: Messaging
- [ ] Text messages with emoji
- [ ] Image upload and preview
- [ ] File sharing with download
- [ ] Voice message recording
- [ ] Video message support
- [ ] Location sharing with map

### Phase 4: Friend System
- [ ] Search and add friend
- [ ] Friend request handling
- [ ] Friend list management
- [ ] Delete friend functionality

### Phase 5: Group System
- [ ] Create group
- [ ] Group chat interface
- [ ] Group management (owner)
- [ ] Member management
- [ ] Group info editing
- [ ] Transfer/dissolve group

### Phase 6: Advanced Features
- [ ] Message recall
- [ ] Message forward
- [ ] Message quote/reply
- [ ] @ mention in groups
- [ ] Personal card sharing
- [ ] Group card sharing

### Phase 7: Settings
- [ ] Profile editing
- [ ] Password change
- [ ] About page
- [ ] Notification settings

---

## API Endpoint Requirements

Based on UI functions, the following API endpoints are required:

### Auth
- POST /auth/login
- POST /auth/register
- POST /auth/logout
- POST /auth/refresh
- POST /auth/forgot-password
- POST /auth/reset-password

### User
- GET /user/profile
- PUT /user/profile
- PUT /user/avatar
- PUT /user/password
- GET /user/search?q={query}

### Friends
- GET /friends
- POST /friends/request
- GET /friends/requests
- POST /friends/requests/{id}/accept
- POST /friends/requests/{id}/reject
- DELETE /friends/{id}

### Groups
- GET /groups
- POST /groups
- GET /groups/{id}
- PUT /groups/{id}
- DELETE /groups/{id}
- GET /groups/{id}/members
- POST /groups/{id}/members
- DELETE /groups/{id}/members/{userId}
- POST /groups/{id}/transfer
- POST /groups/{id}/leave

### Conversations
- GET /conversations
- GET /conversations/{id}
- DELETE /conversations/{id}
- PUT /conversations/{id}/mute
- PUT /conversations/{id}/pin

### Messages
- GET /messages?conversationId={id}&before={cursor}
- POST /messages
- PUT /messages/{id}/recall
- POST /messages/{id}/forward
- DELETE /messages/{id}

### Files
- POST /files/upload
- GET /files/{id}

---

Last Updated: 2025-11-27
Reference: RainbowChat-Web v7.0
