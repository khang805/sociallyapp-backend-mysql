<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

$data = json_decode(file_get_contents("php://input"));

if (!empty($data->chat_user_id) && !empty($data->screenshot_taker_id)) {
    
    // Log screenshot alert
    $query = "INSERT INTO screenshot_alerts (chat_user_id, screenshot_taker_id) 
              VALUES (:chat_user_id, :screenshot_taker_id)";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(":chat_user_id", $data->chat_user_id);
    $stmt->bindParam(":screenshot_taker_id", $data->screenshot_taker_id);
    
    if ($stmt->execute()) {
        $alert_id = $db->lastInsertId();
        
        // Send notification
        $notifQuery = "INSERT INTO notifications (user_id, sender_id, type, title, body, reference_id) 
                      VALUES (:user_id, :sender_id, 'screenshot', 'Screenshot Alert', 'Someone took a screenshot of your chat', :alert_id)";
        $notifStmt = $db->prepare($notifQuery);
        $notifStmt->bindParam(":user_id", $data->chat_user_id);
        $notifStmt->bindParam(":sender_id", $data->screenshot_taker_id);
        $notifStmt->bindParam(":alert_id", $alert_id);
        $notifStmt->execute();
        
        // Get FCM token for push notification
        $tokenQuery = "SELECT fcm_token FROM users WHERE id = :user_id";
        $tokenStmt = $db->prepare($tokenQuery);
        $tokenStmt->bindParam(":user_id", $data->chat_user_id);
        $tokenStmt->execute();
        $user = $tokenStmt->fetch(PDO::FETCH_ASSOC);
        
        echo json_encode([
            "status" => "success",
            "message" => "Screenshot alert logged.",
            "alert_id" => $alert_id,
            "fcm_token" => $user['fcm_token'] ?? null
        ]);
    } else {
        http_response_code(503);
        echo json_encode([
            "status" => "error",
            "message" => "Unable to log screenshot alert."
        ]);
    }
} else {
    http_response_code(400);
    echo json_encode([
        "status" => "error",
        "message" => "Incomplete data."
    ]);
}
?>
