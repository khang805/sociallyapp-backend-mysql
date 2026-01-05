<?php
// Cron job to clean up expired stories
// Run this script every hour via cron: 0 * * * * /usr/bin/php /path/to/cleanup_stories.php

include_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

// Delete expired stories
$query = "DELETE FROM stories WHERE expires_at < NOW()";
$stmt = $db->prepare($query);

if ($stmt->execute()) {
    $deleted_count = $stmt->rowCount();
    
    echo json_encode([
        "status" => "success",
        "message" => "Cleanup completed.",
        "deleted_count" => $deleted_count,
        "timestamp" => date('Y-m-d H:i:s')
    ]);
    
    // Log to file
    $log_message = date('Y-m-d H:i:s') . " - Deleted " . $deleted_count . " expired stories\n";
    file_put_contents('../../logs/cleanup.log', $log_message, FILE_APPEND);
} else {
    echo json_encode([
        "status" => "error",
        "message" => "Cleanup failed."
    ]);
}
?>
