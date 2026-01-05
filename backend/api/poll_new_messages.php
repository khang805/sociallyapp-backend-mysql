<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET");

include_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

// Get user1_id, user2_id, and last_message_id from query parameters
$user1_id = isset($_GET['user1_id']) ? intval($_GET['user1_id']) : 0;
$user2_id = isset($_GET['user2_id']) ? intval($_GET['user2_id']) : 0;
$last_message_id = isset($_GET['last_message_id']) ? intval($_GET['last_message_id']) : 0;

if ($user1_id > 0 && $user2_id > 0) {
    
    // Get new messages since last_message_id
    $query = "SELECT m.*, 
                     sender.username as sender_username,
                     sender.profile_picture as sender_profile_picture,
                     receiver.username as receiver_username
              FROM messages m
              LEFT JOIN users sender ON m.sender_id = sender.id
              LEFT JOIN users receiver ON m.receiver_id = receiver.id
              WHERE ((m.sender_id = :user1_id AND m.receiver_id = :user2_id)
                     OR (m.sender_id = :user2_id AND m.receiver_id = :user1_id))
              AND m.id > :last_message_id
              AND m.is_deleted = FALSE
              ORDER BY m.created_at ASC";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(":user1_id", $user1_id);
    $stmt->bindParam(":user2_id", $user2_id);
    $stmt->bindParam(":last_message_id", $last_message_id);
    $stmt->execute();
    
    $messages = array();
    
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $message = array(
            "id" => $row['id'],
            "sender_id" => $row['sender_id'],
            "receiver_id" => $row['receiver_id'],
            "message_text" => $row['message_text'],
            "media_url" => $row['media_url'],
            "media_type" => $row['media_type'],
            "is_vanish_mode" => (bool)$row['is_vanish_mode'],
            "is_seen" => (bool)$row['is_seen'],
            "is_edited" => (bool)$row['is_edited'],
            "created_at" => $row['created_at'],
            "edited_at" => $row['edited_at'],
            "sender_username" => $row['sender_username'],
            "sender_profile_picture" => $row['sender_profile_picture'],
            "receiver_username" => $row['receiver_username']
        );
        
        array_push($messages, $message);
        
        // Mark as seen if current user is receiver
        if ($row['receiver_id'] == $user1_id && !$row['is_seen']) {
            $updateQuery = "UPDATE messages SET is_seen = TRUE, seen_at = NOW() WHERE id = :message_id";
            $updateStmt = $db->prepare($updateQuery);
            $updateStmt->bindParam(":message_id", $row['id']);
            $updateStmt->execute();
        }
    }
    
    echo json_encode(array(
        "status" => "success",
        "messages" => $messages,
        "count" => count($messages)
    ));
    
} else {
    http_response_code(400);
    echo json_encode(array(
        "status" => "error",
        "message" => "Invalid user IDs"
    ));
}
?>
