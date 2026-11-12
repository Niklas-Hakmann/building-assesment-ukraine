# Windows build script.
#
# Windows keeps a lock on a JAR while a java process has it on the module path,
# so `mvn clean` fails if the app is still running. This stops java first, then
# clears both runtime layers before building.

Write-Host "Stopping java processes..." -ForegroundColor Green
try { Stop-Process -Name java -Force -ErrorAction Stop } catch {}

Write-Host "Clearing runtime layers..." -ForegroundColor Green
try { Remove-Item -Path ".\mods-mvn" -Recurse -Force -ErrorAction Stop } catch {}
try { Remove-Item -Path ".\plugins\*.jar" -Force -ErrorAction Stop } catch {}

Write-Host "Building..." -ForegroundColor Green
mvn install -DskipTests

if ($?) {
    Write-Host "Build complete. Run with: mvn -N exec:exec" -ForegroundColor Cyan
}
