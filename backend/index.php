<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Social Media API - Test Page</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            padding: 20px;
        }
        
        .container {
            max-width: 1200px;
            margin: 0 auto;
        }
        
        .header {
            background: white;
            border-radius: 15px;
            padding: 30px;
            margin-bottom: 30px;
            box-shadow: 0 10px 40px rgba(0,0,0,0.1);
        }
        
        .header h1 {
            color: #667eea;
            margin-bottom: 10px;
        }
        
        .status-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            margin-bottom: 30px;
        }
        
        .status-card {
            background: white;
            border-radius: 15px;
            padding: 25px;
            box-shadow: 0 10px 40px rgba(0,0,0,0.1);
            transition: transform 0.3s ease;
        }
        
        .status-card:hover {
            transform: translateY(-5px);
        }
        
        .status-card h3 {
            color: #333;
            margin-bottom: 15px;
            font-size: 18px;
        }
        
        .status {
            display: inline-block;
            padding: 8px 16px;
            border-radius: 20px;
            font-size: 14px;
            font-weight: 600;
        }
        
        .status.success {
            background: #d4edda;
            color: #155724;
        }
        
        .status.error {
            background: #f8d7da;
            color: #721c24;
        }
        
        .status.warning {
            background: #fff3cd;
            color: #856404;
        }
        
        .endpoints {
            background: white;
            border-radius: 15px;
            padding: 30px;
            box-shadow: 0 10px 40px rgba(0,0,0,0.1);
        }
        
        .endpoints h2 {
            color: #667eea;
            margin-bottom: 20px;
        }
        
        .endpoint {
            padding: 15px;
            margin-bottom: 15px;
            background: #f8f9fa;
            border-radius: 10px;
            border-left: 4px solid #667eea;
        }
        
        .endpoint-method {
            display: inline-block;
            padding: 4px 12px;
            border-radius: 5px;
            font-size: 12px;
            font-weight: 700;
            margin-right: 10px;
        }
        
        .method-get {
            background: #28a745;
            color: white;
        }
        
        .method-post {
            background: #007bff;
            color: white;
        }
        
        .btn {
            display: inline-block;
            padding: 12px 30px;
            background: #667eea;
            color: white;
            border-radius: 8px;
            text-decoration: none;
            font-weight: 600;
            margin-top: 10px;
            transition: background 0.3s ease;
        }
        
        .btn:hover {
            background: #5568d3;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🚀 Social Media API - Assignment 3</h1>
            <p>Backend API Status Dashboard</p>
        </div>
        
        <div class="status-grid">
            <div class="status-card">
                <h3>Database Connection</h3>
                <div class="status-info">
                    <?php
                    include_once 'config/database.php';
                    $database = new Database();
                    $db = $database->getConnection();
                    
                    if ($db) {
                        echo '<span class="status success">✓ Connected</span>';
                    } else {
                        echo '<span class="status error">✗ Failed</span>';
                    }
                    ?>
                </div>
            </div>
            
            <div class="status-card">
                <h3>Upload Directories</h3>
                <div class="status-info">
                    <?php
                    $dirs = ['uploads/posts', 'uploads/stories', 'uploads/profiles', 'uploads/covers', 'uploads/messages'];
                    $all_exist = true;
                    foreach ($dirs as $dir) {
                        if (!file_exists($dir)) {
                            $all_exist = false;
                            break;
                        }
                    }
                    
                    if ($all_exist) {
                        echo '<span class="status success">✓ All Ready</span>';
                    } else {
                        echo '<span class="status warning">⚠ Missing Folders</span>';
                    }
                    ?>
                </div>
            </div>
            
            <div class="status-card">
                <h3>Total Users</h3>
                <div class="status-info">
                    <?php
                    if ($db) {
                        $query = "SELECT COUNT(*) as count FROM users";
                        $stmt = $db->prepare($query);
                        $stmt->execute();
                        $result = $stmt->fetch(PDO::FETCH_ASSOC);
                        echo '<span class="status success">' . $result['count'] . ' users</span>';
                    }
                    ?>
                </div>
            </div>
            
            <div class="status-card">
                <h3>Total Posts</h3>
                <div class="status-info">
                    <?php
                    if ($db) {
                        $query = "SELECT COUNT(*) as count FROM posts";
                        $stmt = $db->prepare($query);
                        $stmt->execute();
                        $result = $stmt->fetch(PDO::FETCH_ASSOC);
                        echo '<span class="status success">' . $result['count'] . ' posts</span>';
                    }
                    ?>
                </div>
            </div>
        </div>
        
        <div class="endpoints">
            <h2>📡 Available API Endpoints</h2>
            
            <div class="endpoint">
                <span class="endpoint-method method-post">POST</span>
                <strong>/api/signup.php</strong> - User Registration
            </div>
            
            <div class="endpoint">
                <span class="endpoint-method method-post">POST</span>
                <strong>/api/login.php</strong> - User Login
            </div>
            
            <div class="endpoint">
                <span class="endpoint-method method-post">POST</span>
                <strong>/api/upload_post.php</strong> - Upload Post with Image
            </div>
            
            <div class="endpoint">
                <span class="endpoint-method method-get">GET</span>
                <strong>/api/get_posts.php</strong> - Get Feed Posts
            </div>
            
            <div class="endpoint">
                <span class="endpoint-method method-post">POST</span>
                <strong>/api/upload_story.php</strong> - Upload Story (24hr expiry)
            </div>
            
            <div class="endpoint">
                <span class="endpoint-method method-get">GET</span>
                <strong>/api/get_stories.php</strong> - Get Active Stories
            </div>
            
            <div class="endpoint">
                <span class="endpoint-method method-post">POST</span>
                <strong>/api/send_message.php</strong> - Send Message
            </div>
            
            <div class="endpoint">
                <span class="endpoint-method method-get">GET</span>
                <strong>/api/get_messages.php</strong> - Get Chat Messages
            </div>
            
            <div class="endpoint">
                <span class="endpoint-method method-post">POST</span>
                <strong>/api/send_follow_request.php</strong> - Send Follow Request
            </div>
            
            <div class="endpoint">
                <span class="endpoint-method method-get">GET</span>
                <strong>/api/search_users.php</strong> - Search Users
            </div>
            
            <a href="README.md" class="btn">📖 View Full Documentation</a>
        </div>
    </div>
</body>
</html>
