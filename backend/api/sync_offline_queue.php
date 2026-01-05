<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

$data = json_decode(file_get_contents("php://input"));

if (!empty($data->user_id)) {
    
    // Get pending queue items
    $query = "SELECT * FROM offline_queue 
              WHERE user_id = :user_id 
              AND status = 'pending'
              ORDER BY created_at ASC";
    
    $stmt = $db->prepare($query);
    $stmt->bindParam(":user_id", $data->user_id);
    $stmt->execute();
    
    $queue_items = $stmt->fetchAll(PDO::FETCH_ASSOC);
    $results = [];
    
    foreach ($queue_items as $item) {
        // Mark as processing
        $updateQuery = "UPDATE offline_queue SET status = 'processing' WHERE id = :id";
        $updateStmt = $db->prepare($updateQuery);
        $updateStmt->bindParam(":id", $item['id']);
        $updateStmt->execute();
        
        $action_data = json_decode($item['action_data'], true);
        $success = false;
        
        try {
            // Process based on action type
            switch ($item['action_type']) {
                case 'message':
                    // Send message
                    $msgQuery = "INSERT INTO messages (sender_id, receiver_id, message_text, media_url, media_type) 
                                VALUES (:sender_id, :receiver_id, :message_text, :media_url, :media_type)";
                    $msgStmt = $db->prepare($msgQuery);
                    $msgStmt->execute($action_data);
                    $success = true;
                    break;
                    
                case 'post':
                    // Create post
                    $postQuery = "INSERT INTO posts (user_id, caption, image_url) 
                                 VALUES (:user_id, :caption, :image_url)";
                    $postStmt = $db->prepare($postQuery);
                    $postStmt->execute($action_data);
                    $success = true;
                    break;
                    
                case 'story':
                    // Create story
                    $storyQuery = "INSERT INTO stories (user_id, media_url, media_type, expires_at) 
                                  VALUES (:user_id, :media_url, :media_type, :expires_at)";
                    $storyStmt = $db->prepare($storyQuery);
                    $storyStmt->execute($action_data);
                    $success = true;
                    break;
                    
                case 'like':
                    // Add like
                    $likeQuery = "INSERT IGNORE INTO post_likes (post_id, user_id) 
                                 VALUES (:post_id, :user_id)";
                    $likeStmt = $db->prepare($likeQuery);
                    $likeStmt->execute($action_data);
                    $success = true;
                    break;
                    
                case 'comment':
                    // Add comment
                    $commentQuery = "INSERT INTO comments (post_id, user_id, comment_text) 
                                    VALUES (:post_id, :user_id, :comment_text)";
                    $commentStmt = $db->prepare($commentQuery);
                    $commentStmt->execute($action_data);
                    $success = true;
                    break;
            }
            
            if ($success) {
                // Mark as completed
                $completeQuery = "UPDATE offline_queue 
                                 SET status = 'completed', processed_at = NOW() 
                                 WHERE id = :id";
                $completeStmt = $db->prepare($completeQuery);
                $completeStmt->bindParam(":id", $item['id']);
                $completeStmt->execute();
                
                $results[] = [
                    "queue_id" => $item['id'],
                    "status" => "completed"
                ];
            }
            
        } catch (Exception $e) {
            // Mark as failed and increment retry count
            $failQuery = "UPDATE offline_queue 
                         SET status = 'failed', retry_count = retry_count + 1 
                         WHERE id = :id";
            $failStmt = $db->prepare($failQuery);
            $failStmt->bindParam(":id", $item['id']);
            $failStmt->execute();
            
            $results[] = [
                "queue_id" => $item['id'],
                "status" => "failed",
                "error" => $e->getMessage()
            ];
        }
    }
    
    echo json_encode([
        "status" => "success",
        "message" => "Sync completed.",
        "processed_count" => count($results),
        "results" => $results
    ]);
    
} else {
    http_response_code(400);
    echo json_encode([
        "status" => "error",
        "message" => "User ID is required."
    ]);
}
?>
