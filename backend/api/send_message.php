<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

$data = json_decode(file_get_contents("php://input"));

if (!empty($data->sender_id) && !empty($data->receiver_id)) {
    
    $message_text = isset($data->message_text) ? $data->message_text : null;
    $media_url = null;
    $media_type = 'text';
    $is_vanish_mode = isset($data->is_vanish_mode) ? $data->is_vanish_mode : false;
    
    // Handle base64 media if provided
    if (!empty($data->media_base64)) {
        $media_data = base64_decode(preg_replace('#^data:(image|video)/\w+;base64,#i', '', $data->media_base64));
        
        $target_dir = "../../uploads/messages/";
        if (!file_exists($target_dir)) {
            mkdir($target_dir, 0777, true);
        }
        
        $media_type = isset($data->media_type) ? $data->media_type : 'image';
        $extension = $media_type == 'video' ? '.mp4' : ($media_type == 'file' ? '.pdf' : '.jpg');
        $filename = uniqid() . '_' . time() . $extension;
        $filepath = $target_dir . $filename;
        
        if (file_put_contents($filepath, $media_data)) {
            $media_url = "uploads/messages/" . $filename;
        }
    }
    
    // Insert message
    $query = "INSERT INTO messages 
              (sender_id, receiver_id, message_text, media_url, media_type, is_vanish_mode) 
              VALUES (:sender_id, :receiver_id, :message_text, :media_url, :media_type, :is_vanish_mode)";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(":sender_id", $data->sender_id);
    $stmt->bindParam(":receiver_id", $data->receiver_id);
    $stmt->bindParam(":message_text", $message_text);
    $stmt->bindParam(":media_url", $media_url);
    $stmt->bindParam(":media_type", $media_type);
    $stmt->bindParam(":is_vanish_mode", $is_vanish_mode, PDO::PARAM_BOOL);
    
    if ($stmt->execute()) {
        $message_id = $db->lastInsertId();
        
        // Send notification
        $notifQuery = "INSERT INTO notifications (user_id, sender_id, type, title, body, reference_id) 
                      VALUES (:user_id, :sender_id, 'message', 'New Message', :message, :message_id)";
        $notifStmt = $db->prepare($notifQuery);
        $notifStmt->bindParam(":user_id", $data->receiver_id);
        $notifStmt->bindParam(":sender_id", $data->sender_id);
        $notifStmt->bindParam(":message", $message_text);
        $notifStmt->bindParam(":message_id", $message_id);
        $notifStmt->execute();
        
        echo json_encode([
            "status" => "success",
            "message_id" => $message_id,
            "media_url" => $media_url
        ]);
    } else {
        http_response_code(503);
        echo json_encode([
            "status" => "error",
            "message" => "Unable to send message."
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
