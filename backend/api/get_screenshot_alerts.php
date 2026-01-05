<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET");

include_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

// Get user_id from query parameter
$user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : 0;
$last_notification_id = isset($_GET['last_notification_id']) ? intval($_GET['last_notification_id']) : 0;

if ($user_id > 0) {
    
    // Get new screenshot notifications
    $query = "SELECT sa.*, 
                     u.username as taker_username,
                     u.profile_picture as taker_profile_picture
              FROM screenshot_alerts sa
              LEFT JOIN users u ON sa.screenshot_taker_id = u.id
              WHERE sa.chat_user_id = :user_id
              AND sa.id > :last_id
              ORDER BY sa.created_at DESC";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(":user_id", $user_id);
    $stmt->bindParam(":last_id", $last_notification_id);
    $stmt->execute();
    
    $alerts = array();
    
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $alert = array(
            "id" => $row['id'],
            "screenshot_taker_id" => $row['screenshot_taker_id'],
            "taker_username" => $row['taker_username'],
            "taker_profile_picture" => $row['taker_profile_picture'],
            "created_at" => $row['created_at'],
            "message" => $row['taker_username'] . " took a screenshot of your chat"
        );
        
        array_push($alerts, $alert);
    }
    
    echo json_encode(array(
        "status" => "success",
        "alerts" => $alerts,
        "count" => count($alerts)
    ));
    
} else {
    http_response_code(400);
    echo json_encode(array(
        "status" => "error",
        "message" => "Invalid user ID"
    ));
}
?>
