<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

$data = json_decode(file_get_contents("php://input"));

if (!empty($data->user_id)) {
    
    $profile_picture = null;
    $cover_photo = null;
    
    // Handle profile picture
    if (!empty($data->profile_picture_base64)) {
        $image_data = base64_decode(preg_replace('#^data:image/\w+;base64,#i', '', $data->profile_picture_base64));
        
        // Use absolute path based on this script's directory to ensure files go into backend/uploads
        $upload_base = realpath(__DIR__ . '/../uploads');
        if ($upload_base === false) {
            // fallback to __DIR__/../uploads path
            $upload_base = __DIR__ . '/../uploads';
        }

        $target_dir = rtrim($upload_base, '/') . '/profiles/';
        if (!file_exists($target_dir)) {
            mkdir($target_dir, 0777, true);
        }
        
        $filename = 'profile_' . $data->user_id . '_' . time() . '.jpg';
        $filepath = $target_dir . $filename;
        
        if (file_put_contents($filepath, $image_data) !== false) {
            // Save relative path (from backend root) for serving
            $profile_picture = 'uploads/profiles/' . $filename;
        }
    }
    
    // Handle cover photo
    if (!empty($data->cover_photo_base64)) {
        $image_data = base64_decode(preg_replace('#^data:image/\w+;base64,#i', '', $data->cover_photo_base64));
        
        $upload_base = realpath(__DIR__ . '/../uploads');
        if ($upload_base === false) {
            $upload_base = __DIR__ . '/../uploads';
        }

        $target_dir = rtrim($upload_base, '/') . '/covers/';
        if (!file_exists($target_dir)) {
            mkdir($target_dir, 0777, true);
        }
        
        $filename = 'cover_' . $data->user_id . '_' . time() . '.jpg';
        $filepath = $target_dir . $filename;
        
        if (file_put_contents($filepath, $image_data) !== false) {
            $cover_photo = 'uploads/covers/' . $filename;
        }
    }
    
    // Update bio if provided
    $bio = isset($data->bio) ? $data->bio : null;
    
    // Build update query
    $updates = [];
    $params = [":user_id" => $data->user_id];
    
    if ($profile_picture) {
        $updates[] = "profile_picture = :profile_picture";
        $params[":profile_picture"] = $profile_picture;
    }
    
    if ($cover_photo) {
        $updates[] = "cover_photo = :cover_photo";
        $params[":cover_photo"] = $cover_photo;
    }
    
    if ($bio !== null) {
        $updates[] = "bio = :bio";
        $params[":bio"] = $bio;
    }
    
    if (count($updates) > 0) {
        $query = "UPDATE users SET " . implode(", ", $updates) . " WHERE id = :user_id";
        $stmt = $db->prepare($query);
        
        foreach ($params as $key => $value) {
            $stmt->bindValue($key, $value);
        }
        
        if ($stmt->execute()) {
            echo json_encode([
                "status" => "success",
                "message" => "Profile updated successfully.",
                "profile_picture" => $profile_picture,
                "cover_photo" => $cover_photo
            ]);
        } else {
            http_response_code(503);
            echo json_encode([
                "status" => "error",
                "message" => "Unable to update profile."
            ]);
        }
    } else {
        http_response_code(400);
        echo json_encode([
            "status" => "error",
            "message" => "No data to update."
        ]);
    }
} else {
    http_response_code(400);
    echo json_encode([
        "status" => "error",
        "message" => "User ID is required."
    ]);
}
?>
