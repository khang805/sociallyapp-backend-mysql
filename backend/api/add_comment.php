<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

$data = json_decode(file_get_contents("php://input"));

if (!empty($data->post_id) && !empty($data->user_id) && !empty($data->comment_text)) {
    
    $query = "INSERT INTO comments (post_id, user_id, comment_text) 
              VALUES (:post_id, :user_id, :comment_text)";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(":post_id", $data->post_id);
    $stmt->bindParam(":user_id", $data->user_id);
    $stmt->bindParam(":comment_text", $data->comment_text);
    
    if ($stmt->execute()) {
        $comment_id = $db->lastInsertId();
        
        // Send notification to post owner
        $postQuery = "SELECT user_id FROM posts WHERE id = :post_id";
        $postStmt = $db->prepare($postQuery);
        $postStmt->bindParam(":post_id", $data->post_id);
        $postStmt->execute();
        $post = $postStmt->fetch(PDO::FETCH_ASSOC);
        
        if ($post && $post['user_id'] != $data->user_id) {
            $notifQuery = "INSERT INTO notifications (user_id, sender_id, type, title, body, reference_id) 
                          VALUES (:user_id, :sender_id, 'comment', 'New Comment', 'Someone commented on your post', :post_id)";
            $notifStmt = $db->prepare($notifQuery);
            $notifStmt->bindParam(":user_id", $post['user_id']);
            $notifStmt->bindParam(":sender_id", $data->user_id);
            $notifStmt->bindParam(":post_id", $data->post_id);
            $notifStmt->execute();
        }
        
        echo json_encode([
            "status" => "success",
            "message" => "Comment added.",
            "comment_id" => $comment_id
        ]);
    } else {
        http_response_code(503);
        echo json_encode([
            "status" => "error",
            "message" => "Unable to add comment."
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
