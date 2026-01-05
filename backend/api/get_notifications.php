<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");

include_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

$user_id = isset($_GET['user_id']) ? $_GET['user_id'] : null;

if ($user_id) {
    // Get notifications - simple query without filtering
    $query = "SELECT n.*, u.username as sender_username, u.profile_picture as sender_profile_picture
              FROM notifications n
              LEFT JOIN users u ON n.sender_id = u.id
              WHERE n.user_id = :user_id
              ORDER BY n.created_at DESC
              LIMIT 50";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(":user_id", $user_id);
    $stmt->execute();
    
    $notifications = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    echo json_encode([
        "status" => "success",
        "notifications" => $notifications
    ]);
} else {
    http_response_code(400);
    echo json_encode([
        "status" => "error",
        "message" => "User ID is required."
    ]);
}
?>
