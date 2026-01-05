<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

$data = json_decode(file_get_contents("php://input"));

if (!empty($data->message_id)) {
    
    // Check if message was sent within last 5 minutes
    $checkQuery = "SELECT created_at FROM messages WHERE id = :message_id";
    $checkStmt = $db->prepare($checkQuery);
    $checkStmt->bindParam(":message_id", $data->message_id);
    $checkStmt->execute();
    $message = $checkStmt->fetch(PDO::FETCH_ASSOC);
    
    if ($message) {
        $time_diff = time() - strtotime($message['created_at']);
        
        if ($time_diff <= 300) { // 5 minutes
            // Mark message as deleted
            $query = "UPDATE messages SET is_deleted = TRUE WHERE id = :message_id";
            $stmt = $db->prepare($query);
            $stmt->bindParam(":message_id", $data->message_id);
            
            if ($stmt->execute()) {
                echo json_encode([
                    "status" => "success",
                    "message" => "Message deleted successfully."
                ]);
            } else {
                http_response_code(503);
                echo json_encode([
                    "status" => "error",
                    "message" => "Unable to delete message."
                ]);
            }
        } else {
            http_response_code(403);
            echo json_encode([
                "status" => "error",
                "message" => "Cannot delete message after 5 minutes."
            ]);
        }
    } else {
        http_response_code(404);
        echo json_encode([
            "status" => "error",
            "message" => "Message not found."
        ]);
    }
} else {
    http_response_code(400);
    echo json_encode([
        "status" => "error",
        "message" => "Message ID is required."
    ]);
}
?>
