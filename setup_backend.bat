@echo off
echo ================================================
echo  Social Media App - Backend Setup Script
echo ================================================
echo.

REM Navigate to backend directory
cd /d "%~dp0backend"

echo [1/4] Creating upload directories...
if not exist "uploads\posts" mkdir "uploads\posts"
if not exist "uploads\stories" mkdir "uploads\stories"
if not exist "uploads\profiles" mkdir "uploads\profiles"
if not exist "uploads\covers" mkdir "uploads\covers"
if not exist "uploads\messages" mkdir "uploads\messages"
if not exist "logs" mkdir "logs"

echo [2/4] Setting directory permissions...
icacls uploads /grant Everyone:(OI)(CI)F /T >nul 2>&1
icacls logs /grant Everyone:(OI)(CI)F /T >nul 2>&1

echo [3/4] Creating placeholder files...
echo. > "uploads\posts\.gitkeep"
echo. > "uploads\stories\.gitkeep"
echo. > "uploads\profiles\.gitkeep"
echo. > "uploads\covers\.gitkeep"
echo. > "uploads\messages\.gitkeep"
echo. > "logs\.gitkeep"

echo [4/4] Setup complete!
echo.
echo ================================================
echo  Next Steps:
echo ================================================
echo 1. Start XAMPP (Apache + MySQL)
echo 2. Open phpMyAdmin: http://localhost/phpmyadmin
echo 3. Import database: backend/database_schema.sql
echo 4. Test API: http://localhost/Assignment-3/backend/
echo 5. Update Android app BASE_URL if needed
echo ================================================
echo.
pause
