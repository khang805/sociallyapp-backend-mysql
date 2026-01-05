<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

$data = json_decode(file_get_contents("php://input"));

if (!empty($data->post_id) && !empty($data->user_id)) {
    
    // Check if already liked
    $checkQuery = "SELECT id FROM post_likes WHERE post_id = :post_id AND user_id = :user_id";
    $checkStmt = $db->prepare($checkQuery);
    $checkStmt->bindParam(":post_id", $data->post_id);
    $checkStmt->bindParam(":user_id", $data->user_id);
    $checkStmt->execute();
    
    if ($checkStmt->rowCount() > 0) {
        // Unlike
        $query = "DELETE FROM post_likes WHERE post_id = :post_id AND user_id = :user_id";
        $stmt = $db->prepare($query);
        $stmt->bindParam(":post_id", $data->post_id);
        $stmt->bindParam(":user_id", $data->user_id);
        $stmt->execute();
        
        $action = "unliked";
    } else {
        // Like
        $query = "INSERT INTO post_likes (post_id, user_id) VALUES (:post_id, :user_id)";
        $stmt = $db->prepare($query);
        $stmt->bindParam(":post_id", $data->post_id);
        $stmt->bindParam(":user_id", $data->user_id);
        $stmt->execute();
        
        $action = "liked";
        
        // Send notification to post owner
        $postQuery = "SELECT user_id FROM posts WHERE id = :post_id";
        $postStmt = $db->prepare($postQuery);
        $postStmt->bindParam(":post_id", $data->post_id);
        $postStmt->execute();
        $post = $postStmt->fetch(PDO::FETCH_ASSOC);
        
        if ($post && $post['user_id'] != $data->user_id) {
            $notifQuery = "INSERT INTO notifications (user_id, sender_id, type, title, body, reference_id) 
                          VALUES (:user_id, :sender_id, 'like', 'New Like', 'Someone liked your post', :post_id)";
            $notifStmt = $db->prepare($notifQuery);
            $notifStmt->bindParam(":user_id", $post['user_id']);
            $notifStmt->bindParam(":sender_id", $data->user_id);
            $notifStmt->bindParam(":post_id", $data->post_id);
            $notifStmt->execute();
        }
    }
    
    // Get updated like count
    $countQuery = "SELECT COUNT(*) as count FROM post_likes WHERE post_id = :post_id";
    $countStmt = $db->prepare($countQuery);
    $countStmt->bindParam(":post_id", $data->post_id);
    $countStmt->execute();
    $count = $countStmt->fetch(PDO::FETCH_ASSOC);
    
    echo json_encode([
        "status" => "success",
        "action" => $action,
        "likes_count" => $count['count']
    ]);
} else {
    http_response_code(400);
    echo json_encode([
        "status" => "error",
        "message" => "Incomplete data."
    ]);
}
?>
