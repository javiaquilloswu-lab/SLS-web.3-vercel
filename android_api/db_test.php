
<?php
declare(strict_types=1);

header('Content-Type: application/json; charset=UTF-8');

$expected = getenv('DIAGNOSTIC_TOKEN');
$provided = $_SERVER['HTTP_X_DIAGNOSTIC_TOKEN'] ?? '';

if (!$expected || !hash_equals($expected, $provided)) {
    http_response_code(404);
    echo json_encode(['error' => 'Not found']);
    exit;
}

require_once __DIR__ . '/db.php';

$tables = [
    'students',
    'student_tokens',
    'admin_users',
    'admin_tokens',
    'scholarships',
    'scholarship_applications',
    'scholarship_requirements',
    'service_types',
    'student_requests',
    'student_documents',
    'student_notifications',
    'student_concerns',
    'student_concern_messages'
];

try {
    $stmt = $pdo->prepare("
        SELECT table_name
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_type = 'BASE TABLE'
    ");
    $stmt->execute();

    $existing = $stmt->fetchAll(PDO::FETCH_COLUMN);

    $results = [];
    foreach ($tables as $table) {
        $results[$table] = in_array($table, $existing, true)
            ? 'found'
            : 'missing';
    }

    $missing = array_keys(
        array_filter($results, fn($status) => $status === 'missing')
    );

    echo json_encode([
        'status' => empty($missing) ? 'ok' : 'incomplete',
        'expected_tables' => count($tables),
        'found_tables' => count($tables) - count($missing),
        'tables' => $results
    ], JSON_PRETTY_PRINT);

} catch (Throwable $e) {
    error_log('Schema verification failed: ' . $e->getMessage());

    http_response_code(500);
    echo json_encode([
        'status' => 'error',
        'message' => 'Schema verification failed'
    ]);
}
