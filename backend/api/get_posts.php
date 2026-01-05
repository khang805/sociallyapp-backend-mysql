<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");

include_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

$user_id = isset($_GET['user_id']) ? $_GET['user_id'] : null;

if ($user_id) {
    // Get posts from followed users
    $query = "SELECT p.*, u.username, u.profile_picture, 
              (SELECT COUNT(*) FROM post_likes WHERE post_id = p.id) as likes_count,
              (SELECT COUNT(*) FROM comments WHERE post_id = p.id) as comments_count,
              EXISTS(SELECT 1 FROM post_likes WHERE post_id = p.id AND user_id = :user_id) as is_liked
              FROM posts p
              INNER JOIN users u ON p.user_id = u.id
              WHERE p.user_id IN (
                  SELECT receiver_id FROM follow_requests 
                  WHERE sender_id = :user_id AND status = 'accepted'
                  UNION 
                  SELECT sender_id FROM follow_requests 
                  WHERE receiver_id = :user_id AND status = 'accepted'
              ) OR p.user_id = :user_id
              ORDER BY p.created_at DESC
              LIMIT 50";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(":user_id", $user_id);
    $stmt->execute();
    
    $posts = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Convert relative URLs to absolute URLs
    $base_url = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http") . "://" . $_SERVER['HTTP_HOST'];
    $base_path = str_replace('/backend/api/get_posts.php', '', $_SERVER['SCRIPT_NAME']);
    
    foreach ($posts as &$post) {
        if (!empty($post['image_url']) && !preg_match('/^https?:\/\//', $post['image_url'])) {
            $post['image_url'] = $base_url . $base_path . '/' . $post['image_url'];
        }
        if (!empty($post['profile_picture']) && !preg_match('/^https?:\/\//', $post['profile_picture'])) {
            $post['profile_picture'] = $base_url . $base_path . '/' . $post['profile_picture'];
        }
    }
    
    echo json_encode([
        "status" => "success",
        "posts" => $posts
    ]);
} else {
    http_response_code(400);
    echo json_encode([
        "status" => "error",
        "message" => "User ID is required."
    ]);
}
?>
