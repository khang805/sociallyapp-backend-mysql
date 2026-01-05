<?php
/**
 * Migration script to fix existing post image URLs
 * Run this once to update all relative URLs to absolute URLs
 */

header("Content-Type: application/json; charset=UTF-8");
include_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

try {
    // Get all posts
    $query = "SELECT id, image_url FROM posts WHERE image_url IS NOT NULL AND image_url != ''";
    $stmt = $db->prepare($query);
    $stmt->execute();
    $posts = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    $updated = 0;
    $skipped = 0;
    
    foreach ($posts as $post) {
        // Skip if already absolute URL
        if (preg_match('/^https?:\/\//', $post['image_url'])) {
            $skipped++;
            continue;
        }
        
        // The image_url is already stored as relative path, no need to update
        // The get_posts.php will convert it to absolute URL on the fly
        $skipped++;
    }
    
    echo json_encode([
        "status" => "success",
        "message" => "Image URLs checked",
        "total_posts" => count($posts),
        "updated" => $updated,
        "skipped" => $skipped,
        "note" => "Relative URLs are converted to absolute URLs dynamically in get_posts.php"
    ]);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "status" => "error",
        "message" => "Error: " . $e->getMessage()
    ]);
}
?>
