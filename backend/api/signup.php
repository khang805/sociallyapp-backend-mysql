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
if (
    !empty($data->username) &&
    !empty($data->email) &&
    !empty($data->password)
) {
    
    // Check if user already exists
    $query = "SELECT id FROM users WHERE email = :email OR username = :username";
    $stmt = $db->prepare($query);
    $stmt->bindParam(":email", $data->email);
    $stmt->bindParam(":username", $data->username);
    $stmt->execute();
    
    if ($stmt->rowCount() > 0) {
        http_response_code(400);
        echo json_encode([
            "status" => "error",
            "message" => "User with this email or username already exists."
        ]);
        exit();
    }
    
    // Hash password
    $password_hash = password_hash($data->password, PASSWORD_BCRYPT);
    
    // Generate authentication token
    $auth_token = bin2hex(random_bytes(32));
    
    // Insert user
    $query = "INSERT INTO users (username, email, password, fcm_token) 
              VALUES (:username, :email, :password, :fcm_token)";
    
    $stmt = $db->prepare($query);
    
    $stmt->bindParam(":username", $data->username);
    $stmt->bindParam(":email", $data->email);
    $stmt->bindParam(":password", $password_hash);
    $fcm_token = isset($data->fcm_token) ? $data->fcm_token : null;
    $stmt->bindParam(":fcm_token", $fcm_token);
    
    if ($stmt->execute()) {
        $user_id = $db->lastInsertId();
        
        // Get user details including profile picture
        $getUserQuery = "SELECT id, username, email, profile_picture FROM users WHERE id = :user_id";
        $getUserStmt = $db->prepare($getUserQuery);
        $getUserStmt->bindParam(":user_id", $user_id);
        $getUserStmt->execute();
        $user = $getUserStmt->fetch(PDO::FETCH_ASSOC);
        
        http_response_code(201);
        echo json_encode([
            "status" => "success",
            "message" => "User registered successfully.",
            "user_id" => (int)$user['id'],
            "username" => $user['username'],
            "email" => $user['email'],
            "profile_picture" => $user['profile_picture'],
            "auth_token" => $auth_token,
            "is_first_time" => true
        ]);
    } else {
        http_response_code(503);
        echo json_encode([
            "status" => "error",
            "message" => "Unable to register user."
        ]);
    }
} else {
    http_response_code(400);
    echo json_encode([
        "status" => "error",
        "message" => "Unable to register user. Data is incomplete."
    ]);
}
?>
