module.exports = {
    testEnvironment: 'node',
    testMatch: ['**/__tests__/**/*.test.js'],
    // Не требуем БД: тесты проверяют валидацию, авторизацию, модерацию и health.
};
