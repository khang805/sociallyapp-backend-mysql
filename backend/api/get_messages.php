<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");

include_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

$user1_id = isset($_GET['user1_id']) ? $_GET['user1_id'] : null;
$user2_id = isset($_GET['user2_id']) ? $_GET['user2_id'] : null;

if ($user1_id && $user2_id) {
    // Get messages between two users
    $query = "SELECT * FROM messages 
              WHERE (sender_id = :user1_id AND receiver_id = :user2_id 
                 OR sender_id = :user2_id AND receiver_id = :user1_id)
              AND is_deleted = FALSE
              ORDER BY created_at ASC";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(":user1_id", $user1_id);
    $stmt->bindParam(":user2_id", $user2_id);
    $stmt->execute();
    
    $messages = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Mark messages as seen
    $updateQuery = "UPDATE messages 
                   SET is_seen = TRUE, seen_at = NOW() 
                   WHERE receiver_id = :user1_id 
                   AND sender_id = :user2_id 
                   AND is_seen = FALSE";
    $updateStmt = $db->prepare($updateQuery);
    $updateStmt->bindParam(":user1_id", $user1_id);
    $updateStmt->bindParam(":user2_id", $user2_id);
    $updateStmt->execute();
    
    echo json_encode([
        "status" => "success",
        "messages" => $messages
    ]);
} else {
    http_response_code(400);
    echo json_encode([
        "status" => "error",
        "message" => "Both user IDs are required."
    ]);
}
?>
