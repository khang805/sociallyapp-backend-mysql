<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");

include_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

$query = isset($_GET['query']) ? $_GET['query'] : '';
$user_id = isset($_GET['user_id']) ? $_GET['user_id'] : null;
$filter = isset($_GET['filter']) ? $_GET['filter'] : 'all'; // all, followers, following

if (!empty($query)) {
    
    $search_term = "%" . $query . "%";
    
    if ($filter == 'followers' && $user_id) {
        // Search only followers
        $sql = "SELECT u.id, u.username, u.profile_picture, u.bio, u.is_online
                FROM users u
                INNER JOIN follow_requests fr ON u.id = fr.sender_id
                WHERE fr.receiver_id = :user_id 
                AND fr.status = 'accepted'
                AND u.username LIKE :search_term
                LIMIT 20";
        
        $stmt = $db->prepare($sql);
        $stmt->bindParam(":user_id", $user_id);
        $stmt->bindParam(":search_term", $search_term);
        
    } elseif ($filter == 'following' && $user_id) {
        // Search only following
        $sql = "SELECT u.id, u.username, u.profile_picture, u.bio, u.is_online
                FROM users u
                INNER JOIN follow_requests fr ON u.id = fr.receiver_id
                WHERE fr.sender_id = :user_id 
                AND fr.status = 'accepted'
                AND u.username LIKE :search_term
                LIMIT 20";
        
        $stmt = $db->prepare($sql);
        $stmt->bindParam(":user_id", $user_id);
        $stmt->bindParam(":search_term", $search_term);
        
    } else {
        // Search all users
        $sql = "SELECT id, username, profile_picture, bio, is_online 
                FROM users 
                WHERE username LIKE :search_term 
                LIMIT 20";
        
        $stmt = $db->prepare($sql);
        $stmt->bindParam(":search_term", $search_term);
    }
    
    $stmt->execute();
    $users = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    echo json_encode([
        "status" => "success",
        "users" => $users,
        "count" => count($users)
    ]);
} else {
    http_response_code(400);
    echo json_encode([
        "status" => "error",
        "message" => "Search query is required."
    ]);
}
?>
