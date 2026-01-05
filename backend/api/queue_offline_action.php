<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

$data = json_decode(file_get_contents("php://input"));

if (!empty($data->user_id) && !empty($data->action_type) && !empty($data->action_data)) {
    
    $action_data_json = json_encode($data->action_data);
    
    $query = "INSERT INTO offline_queue (user_id, action_type, action_data) 
              VALUES (:user_id, :action_type, :action_data)";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(":user_id", $data->user_id);
    $stmt->bindParam(":action_type", $data->action_type);
    $stmt->bindParam(":action_data", $action_data_json);
    
    if ($stmt->execute()) {
        echo json_encode([
            "status" => "success",
            "message" => "Action queued for sync.",
            "queue_id" => $db->lastInsertId()
        ]);
    } else {
        http_response_code(503);
        echo json_encode([
            "status" => "error",
            "message" => "Unable to queue action."
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
