<?php
declare(strict_types=1);

header('Content-Type: application/json; charset=UTF-8');

$endpoint = $_GET['endpoint'] ?? 'health';

if ($endpoint === 'health') {
    echo json_encode([
        'status' => 'ok',
        'service' => 'Student Life PHP API',
        'php_version' => PHP_VERSION
    ]);
    exit;
}

if (!preg_match('/^[a-zA-Z0-9_]+$/', $endpoint)) {
    http_response_code(400);
    echo json_encode(['error' => 'Invalid endpoint']);
    exit;
}

$file = __DIR__ . '/../android_api/' . $endpoint . '.php';

if (!is_file($file)) {
    http_response_code(404);
    echo json_encode(['error' => 'Endpoint not found']);
    exit;
}

require $file;
