<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

$data = json_decode(file_get_contents("php://input"));

if (!empty($data->sender_id) && !empty($data->receiver_id)) {
    
    // Check if follow request already exists
    $checkQuery = "SELECT id, status FROM follow_requests 
                  WHERE sender_id = :sender_id AND receiver_id = :receiver_id";
    $checkStmt = $db->prepare($checkQuery);
    $checkStmt->bindParam(":sender_id", $data->sender_id);
    $checkStmt->bindParam(":receiver_id", $data->receiver_id);
    $checkStmt->execute();
    
    if ($checkStmt->rowCount() > 0) {
        http_response_code(400);
        echo json_encode([
            "status" => "error",
            "message" => "Follow request already exists."
        ]);
    } else {
        // Create follow request
        $query = "INSERT INTO follow_requests (sender_id, receiver_id, status) 
                  VALUES (:sender_id, :receiver_id, 'pending')";
        
        $stmt = $db->prepare($query);
        $stmt->bindParam(":sender_id", $data->sender_id);
        $stmt->bindParam(":receiver_id", $data->receiver_id);
        
        if ($stmt->execute()) {
            $request_id = $db->lastInsertId();
            
            // Send notification
            $notifQuery = "INSERT INTO notifications (user_id, sender_id, type, title, body, reference_id) 
                          VALUES (:user_id, :sender_id, 'follow_request', 'New Follow Request', 'Someone wants to follow you', :request_id)";
            $notifStmt = $db->prepare($notifQuery);
            $notifStmt->bindParam(":user_id", $data->receiver_id);
            $notifStmt->bindParam(":sender_id", $data->sender_id);
            $notifStmt->bindParam(":request_id", $request_id);
            $notifStmt->execute();
            
            echo json_encode([
                "status" => "success",
                "message" => "Follow request sent.",
                "request_id" => $request_id
            ]);
        } else {
            http_response_code(503);
            echo json_encode([
                "status" => "error",
                "message" => "Unable to send follow request."
            ]);
        }
    }
} else {
    http_response_code(400);
    echo json_encode([
        "status" => "error",
        "message" => "Incomplete data."
    ]);
}
?>
