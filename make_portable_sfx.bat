@echo off
chcp 65001 >nul
echo === Сборка приложения (createDistributable) ===
call gradlew.bat createDistributable

echo === Создание конфигурации WinRAR SFX ===
echo Setup=materia-launcher.exe> sfx_config.txt
echo TempMode>> sfx_config.txt
echo Silent=1>> sfx_config.txt
echo Overwrite=1>> sfx_config.txt

set WINRAR_PATH="C:\Program Files\WinRAR\WinRAR.exe"

if not exist %WINRAR_PATH% (
    echo [ОШИБКА] WinRAR не найден по пути %WINRAR_PATH%
    echo Убедитесь, что WinRAR установлен, или исправьте путь в скрипте.
    del sfx_config.txt
    pause
    exit /b 1
)

echo === Упаковка в портативный SFX-архив ===
if exist MateriaLauncher-Portable.exe del MateriaLauncher-Portable.exe

%WINRAR_PATH% a -sfx -zsfx_config.txt -r -ep1 MateriaLauncher-Portable.exe "build\compose\binaries\main\app\materia-launcher\*"

del sfx_config.txt

echo === Готово! ===
echo Портативная версия (MateriaLauncher-Portable.exe) успешно создана в корне проекта!
pause
