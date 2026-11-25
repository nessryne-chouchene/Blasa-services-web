@echo off
rem Double-clique ici pour lancer Blasa (Redis + API + page web).
rem L'option -ExecutionPolicy Bypass vaut seulement pour cette fenetre : rien n'est modifie sur le PC.
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start.ps1" %*
echo.
echo Blasa est arrete. Appuie sur une touche pour fermer.
pause >nul
