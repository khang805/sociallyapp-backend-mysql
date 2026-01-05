<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET");

include_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

$user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : 0;

if ($user_id > 0) {
    
    // Get user profile data
    $query = "SELECT id, username, email, profile_picture, cover_photo, bio, 
                     is_online, last_seen, created_at
              FROM users 
              WHERE id = :user_id";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(":user_id", $user_id);
    $stmt->execute();
    
    if ($stmt->rowCount() > 0) {
        $row = $stmt->fetch(PDO::FETCH_ASSOC);
        
        // Get followers count
        $followersQuery = "SELECT COUNT(*) as count FROM follow_requests 
                          WHERE receiver_id = :user_id AND status = 'accepted'";
        $followersStmt = $db->prepare($followersQuery);
        $followersStmt->bindParam(":user_id", $user_id);
        $followersStmt->execute();
        $followersCount = $followersStmt->fetch(PDO::FETCH_ASSOC)['count'];
        
        // Get following count
        $followingQuery = "SELECT COUNT(*) as count FROM follow_requests 
                          WHERE sender_id = :user_id AND status = 'accepted'";
        $followingStmt = $db->prepare($followingQuery);
        $followingStmt->bindParam(":user_id", $user_id);
        $followingStmt->execute();
        $followingCount = $followingStmt->fetch(PDO::FETCH_ASSOC)['count'];
        
        // Get posts count
        $postsQuery = "SELECT COUNT(*) as count FROM posts WHERE user_id = :user_id";
        $postsStmt = $db->prepare($postsQuery);
        $postsStmt->bindParam(":user_id", $user_id);
        $postsStmt->execute();
        $postsCount = $postsStmt->fetch(PDO::FETCH_ASSOC)['count'];
        
        echo json_encode(array(
            "status" => "success",
            "user" => array(
                "id" => $row['id'],
                "username" => $row['username'],
                "email" => $row['email'],
                "profile_picture" => $row['profile_picture'],
                "cover_photo" => $row['cover_photo'],
                "bio" => $row['bio'],
                "is_online" => (bool)$row['is_online'],
                "last_seen" => $row['last_seen'],
                "created_at" => $row['created_at'],
                "followers_count" => intval($followersCount),
                "following_count" => intval($followingCount),
                "posts_count" => intval($postsCount)
            )
        ));
    } else {
        http_response_code(404);
        echo json_encode(array(
            "status" => "error",
            "message" => "User not found"
        ));
    }
    
} else {
    http_response_code(400);
    echo json_encode(array(
        "status" => "error",
        "message" => "Invalid user ID"
    ));
}
?>
