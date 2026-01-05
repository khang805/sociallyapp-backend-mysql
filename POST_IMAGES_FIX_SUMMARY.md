# Post Images Fix Summary

## What Was Fixed

### Backend Changes
1. **get_posts.php** - Now converts relative image URLs to absolute URLs automatically
2. **upload_post.php** - Returns absolute URLs when posts are created
3. **debug_posts.php** (NEW) - Debug endpoint to check posts and verify image URLs

### Android App Changes
1. **PostRepository.kt** - Added comprehensive logging and clears old posts before inserting new ones
2. **Main_feed.kt** - Added detailed logging to track post loading and image URL validation
3. **PostAdapter.kt** - Added Picasso callbacks to log success/failure of image loading

## How to Debug

### Step 1: Check if posts exist in database
Open your browser and go to:
```
http://localhost/Assignment-3/backend/api/debug_posts.php
```

This will show you:
- All posts in the database
- Whether image files actually exist
- The absolute URLs being generated

### Step 2: Check Android Logcat
Run the app and filter for these tags:
- `Main_feed` - Post loading
- `PostRepo` - API calls
- `PostAdapter` - Image loading

Look for messages like:
- ✓ "Successfully loaded image for post X"
- ✗ "Failed to load image for post X"

### Step 3: Common Issues

#### Issue 1: No posts showing at all
**Check**: Logcat for "Retrieved X cached posts"
**Fix**: 
- Make sure you're logged in
- Make sure you have posts in the database
- Try uploading a new post

#### Issue 2: Posts show but images are blank
**Check**: Logcat for "Loading image from URL: ..."
**Possible causes**:
1. **Wrong base URL** - Update `RetrofitClient.kt`:
   - Emulator: `http://10.0.2.2/Assignment-3/backend/api/`
   - Real device: `http://YOUR_IP/Assignment-3/backend/api/`

2. **Image files don't exist** - Check `debug_posts.php` output
   - Look for `"file_exists": false`
   - Upload new posts to create new images

3. **Network issue** - Check:
   - XAMPP/WAMP is running
   - Can access `http://10.0.2.2/Assignment-3/` in device browser
   - Firewall isn't blocking connections

#### Issue 3: Old posts have relative URLs
**Check**: Logcat for "WARNING: Post X has relative URL"
**Fix**: The backend now converts these automatically, but if it's not working:
1. Check that `get_posts.php` has the URL conversion code
2. Try uploading a new post (it will have absolute URL)

## Testing Checklist

- [ ] XAMPP/WAMP is running
- [ ] MySQL database has posts (check via phpMyAdmin)
- [ ] Can access backend in browser: `http://10.0.2.2/Assignment-3/backend/api/debug_posts.php`
- [ ] Image files exist in `uploads/posts/` directory
- [ ] App shows posts (even if images are blank)
- [ ] Logcat shows "Loading image from URL: http://..."
- [ ] Logcat shows either success or error for each image

## Quick Test

1. Upload a new post using the camera icon in the app
2. Check logcat for "Post uploaded successfully"
3. Refresh the feed (pull down or reopen app)
4. Check logcat for image loading messages
5. If image still doesn't show, check the URL in logcat and try opening it in a browser

## Need More Help?

If images still don't show:
1. Copy the image URL from logcat
2. Try opening it in Chrome on your computer
3. Try opening it in Chrome on the emulator/device
4. Share the logcat output and debug_posts.php output for further diagnosis
