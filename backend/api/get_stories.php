<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");

include_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

$user_id = isset($_GET['user_id']) ? $_GET['user_id'] : null;

if ($user_id) {
    // Get stories from followed users that haven't expired
    $query = "SELECT s.*, u.username, u.profile_picture
              FROM stories s
              INNER JOIN users u ON s.user_id = u.id
              WHERE s.expires_at > NOW() 
              AND (s.user_id IN (
                  SELECT receiver_id FROM follow_requests 
                  WHERE sender_id = :user_id AND status = 'accepted'
                  UNION 
                  SELECT sender_id FROM follow_requests 
                  WHERE receiver_id = :user_id AND status = 'accepted'
              ) OR s.user_id = :user_id)
              ORDER BY s.created_at DESC";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(":user_id", $user_id);
    $stmt->execute();
    
    $stories = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Group stories by user
    $grouped_stories = [];
    foreach ($stories as $story) {
        $userId = $story['user_id'];
        if (!isset($grouped_stories[$userId])) {
            $grouped_stories[$userId] = [
                'user_id' => $userId,
                'username' => $story['username'],
                'profile_picture' => $story['profile_picture'],
                'stories' => []
            ];
        }
        $grouped_stories[$userId]['stories'][] = $story;
    }
    
    echo json_encode([
        "status" => "success",
        "stories" => array_values($grouped_stories)
    ]);
} else {
    http_response_code(400);
    echo json_encode([
        "status" => "error",
        "message" => "User ID is required."
    ]);
}
?>
