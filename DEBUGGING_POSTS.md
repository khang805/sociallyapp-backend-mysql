# Debugging Posts Not Showing Images

## Steps to Debug

### 1. Check Backend Response
Visit this URL in your browser (replace with your actual IP):
```
http://10.0.2.2/Assignment-3/backend/api/debug_posts.php
```

This will show you:
- All posts in the database
- The image URLs (relative and absolute)
- Whether the image files actually exist on the server

### 2. Check Android Logcat
Run the app and filter logcat for these tags:
- `Main_feed` - Shows post loading process
- `PostRepo` - Shows API calls and database operations
- `PostAdapter` - Shows image loading attempts

Look for:
- "Retrieved X cached posts" - confirms posts are being fetched
- "Loading image from URL: ..." - shows the actual URL being used
- "Failed to load image" - indicates Picasso errors

### 3. Common Issues and Fixes

#### Issue: Images not in database
**Symptom**: "No image URL found for post"
**Fix**: Upload a new post using the camera icon in the app

#### Issue: Relative URLs in database
**Symptom**: "WARNING: Post X has relative URL"
**Fix**: The backend should convert these automatically. Check get_posts.php

#### Issue: Files don't exist on server
**Symptom**: debug_posts.php shows "file_exists": false
**Fix**: Check that uploads/posts/ directory exists and has the image files

#### Issue: Wrong base URL
**Symptom**: Images load but show 404
**Fix**: Update BASE_URL in RetrofitClient.kt:
- For emulator: `http://10.0.2.2/Assignment-3/backend/api/`
- For real device: `http://YOUR_COMPUTER_IP/Assignment-3/backend/api/`

#### Issue: Picasso not loading
**Symptom**: "Failed to load image" in logcat
**Fix**: 
1. Check internet permission in AndroidManifest.xml (already added)
2. Check usesCleartextTraffic="true" (already added)
3. Verify the URL is accessible from the device

### 4. Test Image Loading Manually

Add this test code to Main_feed.kt temporarily:

```kotlin
// Test Picasso directly
val testUrl = "http://10.0.2.2/Assignment-3/uploads/posts/test.jpg"
Picasso.get()
    .load(testUrl)
    .into(object : com.squareup.picasso.Target {
        override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
            android.util.Log.d("PicassoTest", "SUCCESS: Image loaded")
        }
        override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
            android.util.Log.e("PicassoTest", "FAILED: ${e?.message}")
        }
        override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
    })
```

### 5. Verify Backend Setup

1. Check that XAMPP/WAMP is running
2. Check that MySQL is running
3. Verify database has posts:
   ```sql
   SELECT * FROM posts;
   ```
4. Check uploads directory exists and has write permissions:
   ```
   Assignment-3/uploads/posts/
   ```

### 6. Network Debugging

Use Chrome DevTools or Postman to test the API:
```
GET http://localhost/Assignment-3/backend/api/get_posts.php?user_id=1
```

Should return JSON with absolute URLs like:
```json
{
  "status": "success",
  "posts": [
    {
      "id": 1,
      "image_url": "http://localhost/Assignment-3/uploads/posts/abc123.jpg",
      ...
    }
  ]
}
```
