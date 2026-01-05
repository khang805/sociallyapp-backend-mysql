<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");

include_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

$user_id = isset($_GET['user_id']) ? $_GET['user_id'] : null;

if ($user_id) {
    // Get followers (people who follow this user)
    $query = "SELECT u.id, u.username, u.profile_picture, u.is_online, u.last_seen
              FROM users u
              INNER JOIN follow_requests fr ON u.id = fr.sender_id
              WHERE fr.receiver_id = :user_id AND fr.status = 'accepted'
              ORDER BY u.username ASC";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(":user_id", $user_id);
    $stmt->execute();
    
    $followers = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Get following (people this user follows)
    $query2 = "SELECT u.id, u.username, u.profile_picture, u.is_online, u.last_seen
               FROM users u
               INNER JOIN follow_requests fr ON u.id = fr.receiver_id
               WHERE fr.sender_id = :user_id AND fr.status = 'accepted'
               ORDER BY u.username ASC";
    
    $stmt2 = $db->prepare($query2);
    $stmt2->bindParam(":user_id", $user_id);
    $stmt2->execute();
    
    $following = $stmt2->fetchAll(PDO::FETCH_ASSOC);
    
    echo json_encode([
        "status" => "success",
        "followers" => $followers,
        "following" => $following,
        "followers_count" => count($followers),
        "following_count" => count($following)
    ]);
} else {
    http_response_code(400);
    echo json_encode([
        "status" => "error",
        "message" => "User ID is required."
    ]);
}
?>
