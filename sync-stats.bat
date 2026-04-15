@echo off
echo ========================================
echo   LinguaLearn - Sync Resources to Target
echo ========================================

set SRC=src\main\resources
set DEST=target\classes
set M2=%USERPROFILE%\.m2\repository

echo Copying FXML views...
xcopy /Y /S "%SRC%\views\*" "%DEST%\views\" >nul

echo Copying CSS...
xcopy /Y /S "%SRC%\css\*" "%DEST%\css\" >nul

echo Compiling BackofficeStats controllers...
set CP=%DEST%;%M2%\org\openjfx\javafx-fxml\23.0.2\javafx-fxml-23.0.2-win.jar;%M2%\org\openjfx\javafx-base\23.0.2\javafx-base-23.0.2-win.jar;%M2%\org\openjfx\javafx-controls\23.0.2\javafx-controls-23.0.2-win.jar;%M2%\org\openjfx\javafx-graphics\23.0.2\javafx-graphics-23.0.2-win.jar

for /r "%M2%\com\mysql" %%f in (*.jar) do set MYSQL_JAR=%%f
set CP=%CP%;%MYSQL_JAR%

"C:\Program Files\Java\jdk-23\bin\javac.exe" -cp "%CP%" -d "%DEST%" ^
  src\main\java\org\example\services\BackofficeStatsService.java ^
  src\main\java\org\example\controllers\BackofficeStatsController.java

if %ERRORLEVEL% == 0 (
    echo.
    echo [OK] All files synced and compiled successfully!
    echo [OK] You can now restart your app.
) else (
    echo.
    echo [ERROR] Compilation failed. Check the errors above.
)
echo ========================================
pause
