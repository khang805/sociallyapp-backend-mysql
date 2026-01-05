<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET");

include_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

// Get user_id from query parameter
$user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : 0;

if ($user_id > 0) {
    
    // Get user's online status
    $query = "SELECT id, username, profile_picture, is_online, last_seen 
              FROM users 
              WHERE id = :user_id";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(":user_id", $user_id);
    $stmt->execute();
    
    if ($stmt->rowCount() > 0) {
        $row = $stmt->fetch(PDO::FETCH_ASSOC);
        
        echo json_encode(array(
            "status" => "success",
            "user" => array(
                "id" => $row['id'],
                "username" => $row['username'],
                "profile_picture" => $row['profile_picture'],
                "is_online" => (bool)$row['is_online'],
                "last_seen" => $row['last_seen'],
                "status_text" => (bool)$row['is_online'] ? "online" : "offline"
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
