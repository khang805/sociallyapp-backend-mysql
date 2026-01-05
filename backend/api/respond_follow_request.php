<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

$data = json_decode(file_get_contents("php://input"));

if (!empty($data->request_id) && !empty($data->action)) {
    
    $status = ($data->action == 'accept') ? 'accepted' : 'rejected';
    
    // Update follow request status
    $query = "UPDATE follow_requests SET status = :status WHERE id = :request_id";
    $stmt = $db->prepare($query);
    $stmt->bindParam(":status", $status);
    $stmt->bindParam(":request_id", $data->request_id);
    
    if ($stmt->execute()) {
        // Delete the notification for this follow request so it doesn't show up again
        $deleteNotifQuery = "DELETE FROM notifications WHERE type = 'follow_request' AND reference_id = :request_id";
        $deleteStmt = $db->prepare($deleteNotifQuery);
        $deleteStmt->bindParam(":request_id", $data->request_id);
        $deleteStmt->execute();
        
        echo json_encode([
            "status" => "success",
            "message" => "Follow request " . $data->action . "ed."
        ]);
    } else {
        http_response_code(503);
        echo json_encode([
            "status" => "error",
            "message" => "Unable to update follow request."
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
