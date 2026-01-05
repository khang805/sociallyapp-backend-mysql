<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

$data = json_decode(file_get_contents("php://input"));

if (!empty($data->user_id) && isset($data->is_online)) {
    
    $query = "UPDATE users SET is_online = :is_online";
    
    if ($data->is_online == false) {
        $query .= ", last_seen = NOW()";
    }
    
    $query .= " WHERE id = :user_id";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(":is_online", $data->is_online, PDO::PARAM_BOOL);
    $stmt->bindParam(":user_id", $data->user_id);
    
    if ($stmt->execute()) {
        echo json_encode([
            "status" => "success",
            "message" => "Status updated."
        ]);
    } else {
        http_response_code(503);
        echo json_encode([
            "status" => "error",
            "message" => "Unable to update status."
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
