<?php
/**
 * Debug endpoint to check posts and image URLs
 */

header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");

include_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

$user_id = isset($_GET['user_id']) ? $_GET['user_id'] : null;

try {
    // Get all posts for debugging
    $query = "SELECT p.*, u.username, u.profile_picture 
              FROM posts p
              INNER JOIN users u ON p.user_id = u.id
              ORDER BY p.created_at DESC
              LIMIT 10";
    
    $stmt = $db->prepare($query);
    $stmt->execute();
    $posts = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Build absolute URLs
    $base_url = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http") . "://" . $_SERVER['HTTP_HOST'];
    $base_path = str_replace('/backend/api/debug_posts.php', '', $_SERVER['SCRIPT_NAME']);
    
    $debug_info = [
        "base_url" => $base_url,
        "base_path" => $base_path,
        "full_base" => $base_url . $base_path,
        "script_name" => $_SERVER['SCRIPT_NAME'],
        "http_host" => $_SERVER['HTTP_HOST'],
        "total_posts" => count($posts),
        "posts" => []
    ];
    
    foreach ($posts as $post) {
        $relative_url = $post['image_url'];
        $absolute_url = $base_url . $base_path . '/' . $relative_url;
        
        // Check if file exists
        $file_path = "../../" . $relative_url;
        $file_exists = file_exists($file_path);
        
        $debug_info['posts'][] = [
            "id" => $post['id'],
            "username" => $post['username'],
            "relative_url" => $relative_url,
            "absolute_url" => $absolute_url,
            "file_path" => $file_path,
            "file_exists" => $file_exists,
            "created_at" => $post['created_at']
        ];
    }
    
    echo json_encode($debug_info, JSON_PRETTY_PRINT);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "status" => "error",
        "message" => "Error: " . $e->getMessage()
    ]);
}
?>
