# Ultra-lightweight native PowerShell static web server
# Serves http://localhost:8080 so microphone & modern web features work seamlessly!

$port = 8080
$folder = $PSScriptRoot
if (-not $folder) { $folder = (Get-Location).Path }

$listener = New-Object System.Net.HttpListener
$listener.Prefixes.Add("http://localhost:$port/")

try {
    $listener.Start()
    Write-Host "==========================================================" -ForegroundColor Magenta
    Write-Host "   🌸 Komal's Birthday Web Experience is LIVE! 🌸" -ForegroundColor Cyan
    Write-Host "   Running at: http://localhost:$port/" -ForegroundColor Yellow
    Write-Host "   Press Ctrl+C in this window to stop the server." -ForegroundColor Gray
    Write-Host "==========================================================" -ForegroundColor Magenta

    # Launch browser automatically
    Start-Process "http://localhost:$port/"

    while ($listener.IsListening) {
        $context = $listener.GetContext()
        $request = $context.Request
        $response = $context.Response

        $path = $request.Url.LocalPath.TrimStart('/')
        if ([string]::IsNullOrEmpty($path)) { $path = "index.html" }
        $filePath = Join-Path $folder $path

        if (Test-Path $filePath -PathType Leaf) {
            $ext = [System.IO.Path]::GetExtension($filePath).ToLower()
            $mime = switch ($ext) {
                ".html" { "text/html; charset=utf-8" }
                ".css"  { "text/css; charset=utf-8" }
                ".js"   { "application/javascript; charset=utf-8" }
                ".json" { "application/json; charset=utf-8" }
                ".png"  { "image/png" }
                ".jpg"  { "image/jpeg" }
                ".jpeg" { "image/jpeg" }
                ".svg"  { "image/svg+xml" }
                ".ico"  { "image/x-icon" }
                default { "application/octet-stream" }
            }

            $bytes = [System.IO.File]::ReadAllBytes($filePath)
            $response.ContentType = $mime
            $response.ContentLength64 = $bytes.Length
            $response.OutputStream.Write($bytes, 0, $bytes.Length)
        } else {
            $response.StatusCode = 404
            $notFound = [System.Text.Encoding]::UTF8.GetBytes("404 Not Found")
            $response.OutputStream.Write($notFound, 0, $notFound.Length)
        }
        $response.Close()
    }
} catch {
    Write-Host "Server error or stopped: $_" -ForegroundColor Red
} finally {
    if ($listener.IsListening) {
        $listener.Stop()
    }
    $listener.Close()
}
