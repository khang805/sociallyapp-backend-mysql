<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Max-Age: 3600");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

include_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

// Get posted data
$data = json_decode(file_get_contents("php://input"));

// Validate input
if (!empty($data->email) && !empty($data->password)) {
    
    // Query to get user
    $query = "SELECT id, username, email, password, profile_picture, bio FROM users WHERE email = :email";
    $stmt = $db->prepare($query);
    $stmt->bindParam(":email", $data->email);
    $stmt->execute();
    
    if ($stmt->rowCount() > 0) {
        $row = $stmt->fetch(PDO::FETCH_ASSOC);
        
        // Verify password
        if (password_verify($data->password, $row['password'])) {
            
            // Generate authentication token
            $auth_token = bin2hex(random_bytes(32));
            
            // Update FCM token if provided
            if (!empty($data->fcm_token)) {
                $updateQuery = "UPDATE users SET fcm_token = :fcm_token WHERE id = :user_id";
                $updateStmt = $db->prepare($updateQuery);
                $updateStmt->bindParam(":fcm_token", $data->fcm_token);
                $updateStmt->bindParam(":user_id", $row['id']);
                $updateStmt->execute();
            }
            
            // Update online status
            $statusQuery = "UPDATE users SET is_online = TRUE WHERE id = :user_id";
            $statusStmt = $db->prepare($statusQuery);
            $statusStmt->bindParam(":user_id", $row['id']);
            $statusStmt->execute();
            
            // Check if user has completed profile setup (has bio or profile picture)
            $is_first_time = empty($row['bio']) && empty($row['profile_picture']);
            
            http_response_code(200);
            echo json_encode([
                "status" => "success",
                "message" => "Login successful.",
                "user_id" => (int)$row['id'],
                "username" => $row['username'],
                "email" => $row['email'],
                "profile_picture" => $row['profile_picture'],
                "auth_token" => $auth_token,
                "is_first_time" => $is_first_time
            ]);
        } else {
            http_response_code(401);
            echo json_encode([
                "status" => "error",
                "message" => "Invalid password."
            ]);
        }
    } else {
        http_response_code(404);
        echo json_encode([
            "status" => "error",
            "message" => "User not found."
        ]);
    }
} else {
    http_response_code(400);
    echo json_encode([
        "status" => "error",
        "message" => "Unable to login. Data is incomplete."
    ]);
}
?>
