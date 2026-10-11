<?php
header('Content-Type: application/json');

require_once __DIR__ . '/db.php';

try {
    $stmt = $pdo->query('SELECT 1 AS connection_test');
    $result = $stmt->fetch(PDO::FETCH_ASSOC);

    echo json_encode([
        'status' => 'ok',
        'database' => 'connected',
        'test' => $result['connection_test']
    ]);
} catch (Throwable $e) {
    error_log($e->getMessage());

    http_response_code(500);
    echo json_encode([
        'status' => 'error',
        'message' => 'Database query failed'
    ]);
}
