# Social Media Application - Backend API Documentation

## 📋 Overview
This is a complete PHP backend implementation for Assignment 3 - Social Media Application with Instagram-like features.

## 🗄️ Database Setup   

### 1. Import Database Schema
1. Open phpMyAdmin or MySQL command line
2. Run the SQL script: `backend/database_schema.sql`
3. This will create:
   - Database: `social_media_db`
   - All required tables (users, posts, stories, messages, etc.)

### 2. Configure Database Connection
Edit `backend/config/database.php`:
```php
private $host = "localhost";
private $database_name = "social_media_db";
private $username = "root";
private $password = ""; // Your MySQL password
```

### 3. Create Upload Directories
Create these folders in the project root (they must be writable):
```
backend/
├── uploads/
│   ├── posts/
│   ├── stories/
│   ├── profiles/
│   ├── covers/
│   └── messages/
└── logs/
```

**Windows (Run in Command Prompt as Admin):**
```batch
cd /d "e:\SMD A3\backend"
mkdir uploads\posts uploads\stories uploads\profiles uploads\covers uploads\messages logs
icacls uploads /grant Everyone:(OI)(CI)F /T
icacls logs /grant Everyone:(OI)(CI)F /T
```

## 🚀 API Endpoints

### Base URL
Update in Android app: `e:\SMD A3\app\src\main\java\com\coderduo\a21i0753_23i0721\api\RetrofitClient.kt`
```kotlin
private const val BASE_URL = "http://10.0.2.2/Assignment-3/backend/api/"
```

For physical device, use your computer's IP:
```kotlin
private const val BASE_URL = "http://192.168.x.x/Assignment-3/backend/api/"
```

### Authentication Endpoints

#### 1. **Signup** - `POST /signup.php`
```json
Request:
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "password123",
  "fcm_token": "firebase_token_here"
}

Response:
{
  "status": "success",
  "message": "User registered successfully.",
  "user_id": 1,
  "username": "john_doe"
}
```

#### 2. **Login** - `POST /login.php`
```json
Request:
{
  "email": "john@example.com",
  "password": "password123",
  "fcm_token": "firebase_token_here"
}

Response:
{
  "status": "success",
  "message": "Login successful.",
  "user_id": 1,
  "username": "john_doe"
}
```

### Post Endpoints

#### 3. **Upload Post** - `POST /upload_post.php`
```json
Request:
{
  "user_id": 1,
  "caption": "My first post!",
  "image_base64": "data:image/jpeg;base64,/9j/4AAQ..."
}

Response:
{
  "status": "success",
  "message": "Post created successfully.",
  "post_id": 1,
  "image_url": "uploads/posts/abc123.jpg"
}
```

#### 4. **Get Posts** - `GET /get_posts.php?user_id=1`
Returns posts from followed users

#### 5. **Toggle Like** - `POST /toggle_like.php`
```json
Request:
{
  "post_id": 1,
  "user_id": 1
}
```

#### 6. **Add Comment** - `POST /add_comment.php`
```json
Request:
{
  "post_id": 1,
  "user_id": 1,
  "comment_text": "Great post!"
}
```

### Story Endpoints

#### 7. **Upload Story** - `POST /upload_story.php`
```json
Request:
{
  "user_id": 1,
  "media_base64": "data:image/jpeg;base64,/9j/4AAQ...",
  "media_type": "image"
}

Response:
{
  "status": "success",
  "story_id": 1,
  "expires_at": "2025-11-21 12:00:00"
}
```

#### 8. **Get Stories** - `GET /get_stories.php?user_id=1`
Returns active stories (not expired) from followed users

### Messaging Endpoints

#### 9. **Send Message** - `POST /send_message.php`
```json
Request:
{
  "sender_id": 1,
  "receiver_id": 2,
  "message_text": "Hello!",
  "media_base64": "optional_image_data",
  "media_type": "text",
  "is_vanish_mode": false
}
```

#### 10. **Get Messages** - `GET /get_messages.php?user1_id=1&user2_id=2`

#### 11. **Edit Message** - `POST /edit_message.php`
```json
Request:
{
  "message_id": 1,
  "new_text": "Updated message"
}
Note: Only works within 5 minutes of sending
```

#### 12. **Delete Message** - `POST /delete_message.php`
```json
Request:
{
  "message_id": 1
}
Note: Only works within 5 minutes of sending
```

### Follow System Endpoints

#### 13. **Send Follow Request** - `POST /send_follow_request.php`
```json
Request:
{
  "sender_id": 1,
  "receiver_id": 2
}
```

#### 14. **Respond to Follow Request** - `POST /respond_follow_request.php`
```json
Request:
{
  "request_id": 1,
  "action": "accept" // or "reject"
}
```

#### 15. **Get Followers/Following** - `GET /get_followers.php?user_id=1`

### Search Endpoint

#### 16. **Search Users** - `GET /search_users.php?query=john&user_id=1&filter=all`
Filter options: `all`, `followers`, `following`

### User Status Endpoints

#### 17. **Update Online Status** - `POST /update_status.php`
```json
Request:
{
  "user_id": 1,
  "is_online": true
}
```

#### 18. **Update Profile** - `POST /update_profile.php`
```json
Request:
{
  "user_id": 1,
  "profile_picture_base64": "data:image/jpeg;base64,...",
  "cover_photo_base64": "data:image/jpeg;base64,...",
  "bio": "My new bio"
}
```

### Security Endpoints

#### 19. **Log Screenshot** - `POST /log_screenshot.php`
```json
Request:
{
  "chat_user_id": 2,
  "screenshot_taker_id": 1
}
```

### Notification Endpoints

#### 20. **Get Notifications** - `GET /get_notifications.php?user_id=1`

### Offline Support Endpoints

#### 21. **Queue Offline Action** - `POST /queue_offline_action.php`
```json
Request:
{
  "user_id": 1,
  "action_type": "message",
  "action_data": {
    "sender_id": 1,
    "receiver_id": 2,
    "message_text": "Offline message"
  }
}
```

#### 22. **Sync Offline Queue** - `POST /sync_offline_queue.php`
```json
Request:
{
  "user_id": 1
}
```

## 🔄 Automated Tasks

### Cleanup Expired Stories (Cron Job)
Add to your server's crontab (runs every hour):
```bash
0 * * * * /usr/bin/php /path/to/backend/cron/cleanup_stories.php
```

**For Windows (Task Scheduler):**
1. Open Task Scheduler
2. Create Basic Task
3. Trigger: Daily, repeat every 1 hour
4. Action: Start a program
5. Program: `C:\xampp\php\php.exe`
6. Arguments: `"e:\SMD A3\backend\cron\cleanup_stories.php"`

## 🔧 Testing the Backend

### Using Postman
1. Install Postman
2. Import API endpoints
3. Test each endpoint individually

### Example Test - Signup:
```
Method: POST
URL: http://localhost/Assignment-3/backend/api/signup.php
Headers: Content-Type: application/json
Body (raw JSON):
{
  "username": "testuser",
  "email": "test@example.com",
  "password": "test123",
  "fcm_token": "test_token"
}
```

## 📱 Android App Configuration

### Update RetrofitClient.kt
```kotlin
private const val BASE_URL = "http://10.0.2.2/Assignment-3/backend/api/"
```

### Internet Permission (already added)
Check `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## 🔥 Firebase Setup for FCM

1. Go to Firebase Console: https://console.firebase.google.com/
2. Select your project
3. Download `google-services.json`
4. Place in `app/` directory
5. FCM Server Key is in: Project Settings → Cloud Messaging → Server Key

### Send Push Notifications
You can send FCM notifications from the server using the user's `fcm_token` stored in the database.

## 📊 Database Tables

- **users** - User accounts with profile info
- **posts** - User posts with images
- **post_likes** - Post likes tracking
- **comments** - Post comments
- **stories** - Temporary stories (24hr expiry)
- **story_views** - Story view tracking
- **messages** - Chat messages with media support
- **follow_requests** - Follow system
- **notifications** - App notifications
- **screenshot_alerts** - Screenshot detection logs
- **offline_queue** - Offline action queue

## 🛠️ Troubleshooting

### Issue: Cannot connect to API
- Check XAMPP is running (Apache + MySQL)
- Verify BASE_URL in RetrofitClient.kt
- For emulator: use `10.0.2.2`
- For physical device: use computer IP

### Issue: Upload folder permission denied
- Run the icacls command (Windows) or chmod 777 (Linux/Mac)

### Issue: Database connection error
- Verify MySQL is running
- Check credentials in `config/database.php`
- Ensure database exists

### Issue: Stories not disappearing
- Set up the cron job for cleanup
- Manually run: `php backend/cron/cleanup_stories.php`

## ✅ Assignment Completion Checklist

- [x] Stories Feature (10 Marks) - upload_story.php, get_stories.php
- [x] Photo & Media Uploads (5 Marks) - upload_post.php
- [x] Messaging System (15 Marks) - send_message.php, edit_message.php, delete_message.php
- [x] Voice & Video Calls (10 Marks) - Already in Android (Agora SDK)
- [x] Follow System (5 Marks) - send_follow_request.php, respond_follow_request.php
- [x] Push Notifications (10 Marks) - FCM tokens stored, notifications table
- [x] Search & Filters (5 Marks) - search_users.php
- [x] Online/Offline Status (5 Marks) - update_status.php
- [x] Security & Privacy (5 Marks) - log_screenshot.php
- [x] Offline Support (10 Marks) - queue_offline_action.php, sync_offline_queue.php

## 📞 Support
For questions, check the code comments or review the database schema.

---
**Author:** Generated for Assignment 3  
**Date:** November 2025
