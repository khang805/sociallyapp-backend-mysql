<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

// Handle base64 encoded data (from Firebase converted)
$data = json_decode(file_get_contents("php://input"));

if (!empty($data->user_id) && !empty($data->media_base64)) {
    
    // Decode base64 image/video
    $media_data = base64_decode(preg_replace('#^data:image/\w+;base64,#i', '', $data->media_base64));
    
    $target_dir = "../../uploads/stories/";
    if (!file_exists($target_dir)) {
        mkdir($target_dir, 0777, true);
    }
    
    $media_type = isset($data->media_type) ? $data->media_type : 'image';
    $extension = $media_type == 'video' ? '.mp4' : '.jpg';
    $filename = uniqid() . '_' . time() . $extension;
    $filepath = $target_dir . $filename;
    
    if (file_put_contents($filepath, $media_data)) {
        $media_url = "uploads/stories/" . $filename;
        
        // Story expires after 24 hours
        $expires_at = date('Y-m-d H:i:s', strtotime('+24 hours'));
        
        // Insert story
        $query = "INSERT INTO stories (user_id, media_url, media_type, expires_at) 
                  VALUES (:user_id, :media_url, :media_type, :expires_at)";
        
        $stmt = $db->prepare($query);
        $stmt->bindParam(":user_id", $data->user_id);
        $stmt->bindParam(":media_url", $media_url);
        $stmt->bindParam(":media_type", $media_type);
        $stmt->bindParam(":expires_at", $expires_at);
        
        if ($stmt->execute()) {
            http_response_code(201);
            echo json_encode([
                "status" => "success",
                "message" => "Story uploaded successfully.",
                "story_id" => $db->lastInsertId(),
                "media_url" => $media_url,
                "expires_at" => $expires_at
            ]);
        } else {
            http_response_code(503);
            echo json_encode([
                "status" => "error",
                "message" => "Unable to create story."
            ]);
        }
    } else {
        http_response_code(503);
        echo json_encode([
            "status" => "error",
            "message" => "Unable to save media."
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
