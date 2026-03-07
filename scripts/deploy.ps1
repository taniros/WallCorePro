# WallCorePro - Safe git push (triggers Render auto-deploy)
$ErrorActionPreference = "Stop"
Write-Host "WallCorePro Deploy" -ForegroundColor Cyan

# Safety: no secrets in staged files
$staged = git diff --cached --name-only 2>$null
if ($staged) {
    $danger = $staged | Where-Object { $_ -match '\.env$|secrets|api.key' }
    if ($danger) {
        Write-Host "BLOCKED: .env or secrets would be committed!" -ForegroundColor Red
        exit 1
    }
}

# Check .env not in git
$check = git check-ignore backend/.env 2>$null
if (-not $check) {
    if (Test-Path "backend/.env") {
        $ignored = git check-ignore -v backend/.env 2>$null
        if (-not $ignored) {
            Write-Host "WARNING: backend/.env exists. Ensure it's in .gitignore (it should be)." -ForegroundColor Yellow
        }
    }
}

git status
$msg = if ($args[0]) { $args[0] } else { "Update" }
git add .
git commit -m $msg 2>$null
if ($LASTEXITCODE -eq 0) {
    $hasRemote = git remote get-url origin 2>$null
    if ($hasRemote) {
        git push
        Write-Host "`nPushed. Render will auto-deploy." -ForegroundColor Green
    } else {
        Write-Host "`nCommitted. Add remote: git remote add origin https://github.com/YOUR_USER/WallCorePro.git" -ForegroundColor Yellow
        Write-Host "Then: git push -u origin main" -ForegroundColor Yellow
    }
} else {
    Write-Host "`nNothing to commit (or already committed)." -ForegroundColor Gray
}
