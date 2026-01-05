<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Max-Age: 3600");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

include_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

// Check if this is a file upload
if (!empty($_FILES['media'])) {
    // Handle file upload
    $user_id = $_POST['user_id'];
    $caption = isset($_POST['caption']) ? $_POST['caption'] : '';
    
    $target_dir = "../../uploads/posts/";
    if (!file_exists($target_dir)) {
        mkdir($target_dir, 0777, true);
    }
    
    $file_extension = strtolower(pathinfo($_FILES["media"]["name"], PATHINFO_EXTENSION));
    $new_filename = uniqid() . '_' . time() . '.' . $file_extension;
    $target_file = $target_dir . $new_filename;
    
    if (move_uploaded_file($_FILES["media"]["tmp_name"], $target_file)) {
        $image_url = "uploads/posts/" . $new_filename;
        
        // Insert post
        $query = "INSERT INTO posts (user_id, caption, image_url) VALUES (:user_id, :caption, :image_url)";
        $stmt = $db->prepare($query);
        $stmt->bindParam(":user_id", $user_id);
        $stmt->bindParam(":caption", $caption);
        $stmt->bindParam(":image_url", $image_url);
        
        if ($stmt->execute()) {
            // Return absolute URL
            $base_url = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http") . "://" . $_SERVER['HTTP_HOST'];
            $base_path = str_replace('/backend/api/upload_post.php', '', $_SERVER['SCRIPT_NAME']);
            $absolute_url = $base_url . $base_path . '/' . $image_url;
            
            echo json_encode([
                "status" => "success",
                "message" => "Post uploaded successfully.",
                "post_id" => $db->lastInsertId(),
                "image_url" => $absolute_url
            ]);
        } else {
            echo json_encode([
                "status" => "error",
                "message" => "Failed to save post."
            ]);
        }
    } else {
        echo json_encode([
            "status" => "error",
            "message" => "Failed to upload file."
        ]);
    }
    
} else {
    // Handle base64 encoded data
    $data = json_decode(file_get_contents("php://input"));
    
    if (!empty($data->user_id) && !empty($data->image_base64)) {
        
        // Decode base64 image
        $image_data = base64_decode(preg_replace('#^data:image/\w+;base64,#i', '', $data->image_base64));
        
        $target_dir = "../../uploads/posts/";
        if (!file_exists($target_dir)) {
            mkdir($target_dir, 0777, true);
        }
        
        $filename = uniqid() . '_' . time() . '.jpg';
        $filepath = $target_dir . $filename;
        
        if (file_put_contents($filepath, $image_data)) {
            $image_url = "uploads/posts/" . $filename;
            $caption = isset($data->caption) ? $data->caption : '';
            
            // Insert post
            $query = "INSERT INTO posts (user_id, caption, image_url) VALUES (:user_id, :caption, :image_url)";
            $stmt = $db->prepare($query);
            $stmt->bindParam(":user_id", $data->user_id);
            $stmt->bindParam(":caption", $caption);
            $stmt->bindParam(":image_url", $image_url);
            
            if ($stmt->execute()) {
                // Return absolute URL
                $base_url = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http") . "://" . $_SERVER['HTTP_HOST'];
                $base_path = str_replace('/backend/api/upload_post.php', '', $_SERVER['SCRIPT_NAME']);
                $absolute_url = $base_url . $base_path . '/' . $image_url;
                
                http_response_code(201);
                echo json_encode([
                    "status" => "success",
                    "message" => "Post created successfully.",
                    "post_id" => $db->lastInsertId(),
                    "image_url" => $absolute_url
                ]);
            } else {
                http_response_code(503);
                echo json_encode([
                    "status" => "error",
                    "message" => "Unable to create post."
                ]);
            }
        } else {
            http_response_code(503);
            echo json_encode([
                "status" => "error",
                "message" => "Unable to save image."
            ]);
        }
    } else {
        http_response_code(400);
        echo json_encode([
            "status" => "error",
            "message" => "Incomplete data."
        ]);
    }
}
?>
