param(
  [string]$BackendBase = "http://localhost:8080",
  [string]$FrontendBase = "http://localhost:5173",
  [string]$AdminUsername = "admin",
  [string]$AdminPassword = "admin123"
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Net.Http

$handler = New-Object System.Net.Http.HttpClientHandler
$handler.UseCookies = $true
$handler.AllowAutoRedirect = $true
$client = New-Object System.Net.Http.HttpClient($handler)
$client.Timeout = [TimeSpan]::FromSeconds(30)

$results = @()
$videoId = $null
$abExperimentId = $null
$createdUserName = $null

function Add-Result {
  param(
    [string]$Name,
    [bool]$Passed,
    [string]$Detail
  )

  $script:results += [PSCustomObject]@{
    Name = $Name
    Passed = $Passed
    Detail = $Detail
  }

  if ($Passed) {
    Write-Host "[PASS] $Name - $Detail" -ForegroundColor Green
  } else {
    Write-Host "[FAIL] $Name - $Detail" -ForegroundColor Red
  }
}

function Parse-JsonOrNull {
  param([string]$Text)
  if ([string]::IsNullOrWhiteSpace($Text)) {
    return $null
  }
  try {
    return ($Text | ConvertFrom-Json)
  } catch {
    return $null
  }
}

function Resolve-HttpMethod {
  param([string]$Method)

  switch ($Method.ToUpperInvariant()) {
    "GET" { return [System.Net.Http.HttpMethod]::Get }
    "POST" { return [System.Net.Http.HttpMethod]::Post }
    "PUT" { return [System.Net.Http.HttpMethod]::Put }
    "PATCH" { return [System.Net.Http.HttpMethod]::new("PATCH") }
    "DELETE" { return [System.Net.Http.HttpMethod]::Delete }
    default { return [System.Net.Http.HttpMethod]::new($Method) }
  }
}

function Invoke-Api {
  param(
    [string]$Name,
    [string]$Method,
    [string]$Url,
    [string]$Body = $null,
    [hashtable]$Headers = @{},
    [int[]]$AcceptStatus = @(200)
  )

  try {
    $methodUpper = $Method.ToUpperInvariant()

    if ($methodUpper -eq "GET") {
      $resp = $client.GetAsync($Url).Result
    } elseif ($methodUpper -eq "POST" -and $null -ne $Body) {
      $content = New-Object System.Net.Http.StringContent($Body, [System.Text.Encoding]::UTF8, "application/json")
      $resp = $client.PostAsync($Url, $content).Result
    } elseif ($methodUpper -eq "POST") {
      $resp = $client.PostAsync($Url, $null).Result
    } else {
      $httpMethod = Resolve-HttpMethod $Method
      $req = New-Object System.Net.Http.HttpRequestMessage($httpMethod, $Url)

      foreach ($k in $Headers.Keys) {
        $req.Headers.TryAddWithoutValidation($k, [string]$Headers[$k]) | Out-Null
      }

      if ($null -ne $Body) {
        $content = New-Object System.Net.Http.StringContent($Body, [System.Text.Encoding]::UTF8, "application/json")
        $req.Content = $content
      }

      $resp = $client.SendAsync($req).Result
    }

    $statusCode = [int]$resp.StatusCode
    $text = if ($null -ne $resp.Content) { $resp.Content.ReadAsStringAsync().Result } else { "" }
    $json = Parse-JsonOrNull $text

    $pass = $AcceptStatus -contains $statusCode
    Add-Result -Name $Name -Passed $pass -Detail "HTTP $statusCode"

    return [PSCustomObject]@{
      StatusCode = $statusCode
      Text = $text
      Json = $json
    }
  } catch {
    Add-Result -Name $Name -Passed $false -Detail ("{0}; url={1}" -f $_.Exception.Message, $Url)
    return $null
  }
}

function Ensure-ApiCodeZero {
  param(
    [string]$Name,
    $ApiResponse
  )

  if ($null -eq $ApiResponse -or $null -eq $ApiResponse.Json) {
    Add-Result -Name "$Name.api" -Passed $false -Detail "响应非 JSON"
    return $false
  }

  $codeObj = $ApiResponse.Json.code
  if ($null -eq $codeObj) {
    Add-Result -Name "$Name.api" -Passed $false -Detail "缺少 code 字段"
    return $false
  }

  $code = [int]$codeObj
  $pass = ($code -eq 0)
  $msg = if ($pass) { "code=0" } else { "code=$code message=$($ApiResponse.Json.message)" }
  Add-Result -Name "$Name.api" -Passed $pass -Detail $msg
  return $pass
}

try {
  Write-Host "=== Task14 Integration Smoke Start ===" -ForegroundColor Cyan

  $me0 = Invoke-Api -Name "auth.me.anonymous" -Method "GET" -Url "$BackendBase/api/admin/auth/me"
  if (Ensure-ApiCodeZero -Name "auth.me.anonymous" -ApiResponse $me0) {
    $loggedIn = [bool]$me0.Json.data.loggedIn
    Add-Result -Name "auth.me.anonymous.loggedIn" -Passed (-not $loggedIn) -Detail "loggedIn=$loggedIn"
  }

  $loginBody = @{ username = $AdminUsername; password = $AdminPassword } | ConvertTo-Json -Compress
  $login = Invoke-Api -Name "auth.login" -Method "POST" -Url "$BackendBase/api/admin/auth/login" -Body $loginBody
  if (Ensure-ApiCodeZero -Name "auth.login" -ApiResponse $login) {
    $loggedIn = [bool]$login.Json.data.loggedIn
    Add-Result -Name "auth.login.loggedIn" -Passed $loggedIn -Detail "loggedIn=$loggedIn"
  }

  $me1 = Invoke-Api -Name "auth.me.afterLogin" -Method "GET" -Url "$BackendBase/api/admin/auth/me"
  if (Ensure-ApiCodeZero -Name "auth.me.afterLogin" -ApiResponse $me1) {
    $loggedIn = [bool]$me1.Json.data.loggedIn
    Add-Result -Name "auth.me.afterLogin.loggedIn" -Passed $loggedIn -Detail "loggedIn=$loggedIn"
  }

  $uniqueTitle = "smoke_" + [DateTime]::Now.ToString("yyyyMMddHHmmss")
  $initBody = @{
    title = $uniqueTitle
    description = "task14 integration smoke"
    fileName = "smoke.mp4"
    mimeType = "video/mp4"
    fileSize = 32
  } | ConvertTo-Json -Compress

  $uploadInit = Invoke-Api -Name "upload.init" -Method "POST" -Url "$BackendBase/api/videos/upload/init" -Body $initBody
  if (Ensure-ApiCodeZero -Name "upload.init" -ApiResponse $uploadInit) {
    $videoId = [long]$uploadInit.Json.data.videoId
    $objectKey = [string]$uploadInit.Json.data.objectKey
    $uploadUrlFromInit = [string]$uploadInit.Json.data.uploadUrl
    Add-Result -Name "upload.init.ids" -Passed ($videoId -gt 0 -and -not [string]::IsNullOrWhiteSpace($objectKey)) -Detail "videoId=$videoId objectKey=$objectKey"

    $tmpFile = Join-Path $env:TEMP "video-site-smoke.mp4"
    [System.IO.File]::WriteAllBytes($tmpFile, [byte[]](0..31))

    try {
      $uploadLocalUrl = if ([string]::IsNullOrWhiteSpace($uploadUrlFromInit)) {
        "$BackendBase/api/videos/upload/local/$videoId?objectKey=$objectKey"
      } elseif ($uploadUrlFromInit.StartsWith("http", [System.StringComparison]::OrdinalIgnoreCase)) {
        $uploadUrlFromInit
      } else {
        "$($BackendBase.TrimEnd('/'))$uploadUrlFromInit"
      }

      $multipartReq = New-Object System.Net.Http.HttpRequestMessage([System.Net.Http.HttpMethod]::Post, $uploadLocalUrl)
      $multipart = New-Object System.Net.Http.MultipartFormDataContent
      $fileStream = [System.IO.File]::OpenRead($tmpFile)
      try {
        $fileContent = [System.Net.Http.StreamContent]::new($fileStream)
        $fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("video/mp4")
        $multipart.Add($fileContent, "file", "smoke.mp4")
        $multipartReq.Content = $multipart

        $uploadResp = $client.SendAsync($multipartReq).Result
      } finally {
        $fileStream.Dispose()
      }
      $uploadStatus = [int]$uploadResp.StatusCode
      $uploadText = $uploadResp.Content.ReadAsStringAsync().Result
      $uploadJson = Parse-JsonOrNull $uploadText
      $uploadBodyPreview = if ([string]::IsNullOrWhiteSpace($uploadText)) { "" } else { " body=$($uploadText.Substring(0, [Math]::Min(240, $uploadText.Length)))" }
      Add-Result -Name "upload.local" -Passed ($uploadStatus -eq 200) -Detail "HTTP $uploadStatus$uploadBodyPreview"
      if ($null -ne $uploadJson) {
        $uploadCode = $uploadJson.code
        $uploadOk = ($null -ne $uploadCode -and [int]$uploadCode -eq 0)
        Add-Result -Name "upload.local.api" -Passed $uploadOk -Detail "code=$uploadCode"
      } else {
        Add-Result -Name "upload.local.api" -Passed $false -Detail "响应非 JSON"
      }
    } catch {
      Add-Result -Name "upload.local" -Passed $false -Detail $_.Exception.Message
    }

    $completeBody = @{
      videoId = $videoId
      objectKey = $objectKey
      mimeType = "video/mp4"
      fileSize = 32
    } | ConvertTo-Json -Compress

    $uploadComplete = Invoke-Api -Name "upload.complete" -Method "POST" -Url "$BackendBase/api/videos/upload/complete" -Body $completeBody
    Ensure-ApiCodeZero -Name "upload.complete" -ApiResponse $uploadComplete | Out-Null

    $adminVideo = Invoke-Api -Name "admin.video.detail" -Method "GET" -Url "$BackendBase/api/admin/videos/$videoId"
    Ensure-ApiCodeZero -Name "admin.video.detail" -ApiResponse $adminVideo | Out-Null

    $patchBody = @{ title = "$uniqueTitle-updated"; description = "updated by smoke" } | ConvertTo-Json -Compress
    $videoPatch = Invoke-Api -Name "admin.video.patch" -Method "PATCH" -Url "$BackendBase/api/admin/videos/$videoId" -Body $patchBody
    Ensure-ApiCodeZero -Name "admin.video.patch" -ApiResponse $videoPatch | Out-Null

    $publish = Invoke-Api -Name "admin.video.publish" -Method "POST" -Url "$BackendBase/api/admin/videos/$videoId/publish"
    Ensure-ApiCodeZero -Name "admin.video.publish" -ApiResponse $publish | Out-Null

    $createdUserName = "smoke_user_" + [DateTime]::Now.ToString("yyyyMMddHHmmss")
    $userPassword = "123456"
    $registerBody = @{ username = $createdUserName; password = $userPassword } | ConvertTo-Json -Compress
    $userRegister = Invoke-Api -Name "user.register" -Method "POST" -Url "$BackendBase/api/auth/register" -Body $registerBody
    if (Ensure-ApiCodeZero -Name "user.register" -ApiResponse $userRegister) {
      Add-Result -Name "user.register.username" -Passed ($userRegister.Json.data.username -eq $createdUserName) -Detail "username=$($userRegister.Json.data.username)"
    }

    $userMe = Invoke-Api -Name "user.me" -Method "GET" -Url "$BackendBase/api/auth/me"
    Ensure-ApiCodeZero -Name "user.me" -ApiResponse $userMe | Out-Null

    $likeAdd = Invoke-Api -Name "user.like.add" -Method "POST" -Url "$BackendBase/api/videos/$videoId/likes"
    Ensure-ApiCodeZero -Name "user.like.add" -ApiResponse $likeAdd | Out-Null

    $likeSummary = Invoke-Api -Name "user.like.summary" -Method "GET" -Url "$BackendBase/api/videos/$videoId/likes/summary"
    Ensure-ApiCodeZero -Name "user.like.summary" -ApiResponse $likeSummary | Out-Null

    $commentCreateBody = @{ content = "smoke comment" } | ConvertTo-Json -Compress
    $commentCreate = Invoke-Api -Name "user.comment.create" -Method "POST" -Url "$BackendBase/api/videos/$videoId/comments" -Body $commentCreateBody
    $commentId = $null
    if (Ensure-ApiCodeZero -Name "user.comment.create" -ApiResponse $commentCreate) {
      $commentId = $commentCreate.Json.data.id
      Add-Result -Name "user.comment.create.id" -Passed ($null -ne $commentId) -Detail "id=$commentId"
    }

    $commentList = Invoke-Api -Name "user.comment.list" -Method "GET" -Url "$BackendBase/api/videos/$videoId/comments?page=1&pageSize=10"
    Ensure-ApiCodeZero -Name "user.comment.list" -ApiResponse $commentList | Out-Null

    if ($null -ne $commentId) {
      $commentDelete = Invoke-Api -Name "user.comment.delete" -Method "DELETE" -Url "$BackendBase/api/videos/$videoId/comments/$commentId"
      Ensure-ApiCodeZero -Name "user.comment.delete" -ApiResponse $commentDelete | Out-Null
    }

    $favoriteAdd = Invoke-Api -Name "user.favorite.add" -Method "POST" -Url "$BackendBase/api/users/me/favorites/$videoId"
    Ensure-ApiCodeZero -Name "user.favorite.add" -ApiResponse $favoriteAdd | Out-Null

    $favoriteList = Invoke-Api -Name "user.favorite.list" -Method "GET" -Url "$BackendBase/api/users/me/favorites?page=1&pageSize=10"
    Ensure-ApiCodeZero -Name "user.favorite.list" -ApiResponse $favoriteList | Out-Null

    $historyUpdateBody = @{ progressSec = 30; durationSecSnapshot = 120 } | ConvertTo-Json -Compress
    $historyUpdate = Invoke-Api -Name "user.history.update" -Method "PUT" -Url "$BackendBase/api/users/me/history/$videoId/progress" -Body $historyUpdateBody
    Ensure-ApiCodeZero -Name "user.history.update" -ApiResponse $historyUpdate | Out-Null

    $historyList = Invoke-Api -Name "user.history.list" -Method "GET" -Url "$BackendBase/api/users/me/history?page=1&pageSize=10"
    Ensure-ApiCodeZero -Name "user.history.list" -ApiResponse $historyList | Out-Null

    $playSources = Invoke-Api -Name "public.playSources" -Method "GET" -Url "$BackendBase/api/videos/$videoId/play-sources" -AcceptStatus @(200, 500)
    if ($null -ne $playSources -and $playSources.StatusCode -eq 200) {
      if ($null -ne $playSources.Json -and $null -ne $playSources.Json.code -and [int]$playSources.Json.code -eq 51003) {
        Add-Result -Name "public.playSources.api" -Passed $true -Detail "code=51003 (transcode pending)"
      } else {
        Ensure-ApiCodeZero -Name "public.playSources" -ApiResponse $playSources | Out-Null
      }
    }

    $eventTime = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    $eventBody = @{
      events = @(
        @{ eventType = "exposure"; videoId = $videoId; eventTime = $eventTime; pagePath = "/"; sessionId = "s_smoke"; abVariant = "A" },
        @{ eventType = "click"; videoId = $videoId; eventTime = $eventTime; pagePath = "/"; sessionId = "s_smoke"; abVariant = "A" }
      )
    } | ConvertTo-Json -Compress

    $eventsBatch = Invoke-Api -Name "events.batch" -Method "POST" -Url "$BackendBase/api/events/batch" -Body $eventBody
    Ensure-ApiCodeZero -Name "events.batch" -ApiResponse $eventsBatch | Out-Null

    $abCreateBody = @{
      name = "smoke_ab_$videoId"
      scene = "home_cover"
      targetVideoId = $videoId
      metricPrimary = "ctr"
      variants = @(
        @{ variantCode = "A"; coverUrl = ""; trafficRatio = 50 },
        @{ variantCode = "B"; coverUrl = ""; trafficRatio = 50 }
      )
    } | ConvertTo-Json -Compress

    $abCreate = Invoke-Api -Name "ab.create" -Method "POST" -Url "$BackendBase/api/admin/ab/experiments" -Body $abCreateBody
    if (Ensure-ApiCodeZero -Name "ab.create" -ApiResponse $abCreate) {
      $abExperimentId = [long]$abCreate.Json.data.id
      Add-Result -Name "ab.create.id" -Passed ($abExperimentId -gt 0) -Detail "id=$abExperimentId"

      $abStart = Invoke-Api -Name "ab.start" -Method "POST" -Url "$BackendBase/api/admin/ab/experiments/$abExperimentId/start"
      Ensure-ApiCodeZero -Name "ab.start" -ApiResponse $abStart | Out-Null

      $abAssign = Invoke-Api -Name "ab.assignment" -Method "GET" -Url "$BackendBase/api/ab/assignment?scene=home_cover&targetVideoId=$videoId"
      Ensure-ApiCodeZero -Name "ab.assignment" -ApiResponse $abAssign | Out-Null

      $eventAbBody = @{
        events = @(
          @{ eventType = "exposure"; videoId = $videoId; eventTime = $eventTime; pagePath = "/"; sessionId = "s_smoke"; abExperimentId = $abExperimentId; abVariant = "A" },
          @{ eventType = "click"; videoId = $videoId; eventTime = $eventTime; pagePath = "/"; sessionId = "s_smoke"; abExperimentId = $abExperimentId; abVariant = "A" }
        )
      } | ConvertTo-Json -Compress
      $eventsAb = Invoke-Api -Name "events.batch.ab" -Method "POST" -Url "$BackendBase/api/events/batch" -Body $eventAbBody
      Ensure-ApiCodeZero -Name "events.batch.ab" -ApiResponse $eventsAb | Out-Null

      $abReport = Invoke-Api -Name "ab.ctrReport" -Method "GET" -Url "$BackendBase/api/admin/ab/experiments/$abExperimentId/ctr-report"
      Ensure-ApiCodeZero -Name "ab.ctrReport" -ApiResponse $abReport | Out-Null
    }

    $recommendHome = Invoke-Api -Name "recommend.home" -Method "GET" -Url "$BackendBase/api/recommend/home?limit=5"
    Ensure-ApiCodeZero -Name "recommend.home" -ApiResponse $recommendHome | Out-Null

    $recommendHot24 = Invoke-Api -Name "recommend.hot.24h" -Method "GET" -Url "$BackendBase/api/recommend/hot?windowType=24h&limit=5"
    Ensure-ApiCodeZero -Name "recommend.hot.24h" -ApiResponse $recommendHot24 | Out-Null

    $recommendHot7d = Invoke-Api -Name "recommend.hot.7d" -Method "GET" -Url "$BackendBase/api/recommend/hot?windowType=7d&limit=5"
    Ensure-ApiCodeZero -Name "recommend.hot.7d" -ApiResponse $recommendHot7d | Out-Null

    $recommendFeedbackBody = @{ videoId = $videoId; action = "click"; scene = "home" } | ConvertTo-Json -Compress
    $recommendFeedback = Invoke-Api -Name "recommend.feedback" -Method "POST" -Url "$BackendBase/api/recommend/feedback" -Body $recommendFeedbackBody
    Ensure-ApiCodeZero -Name "recommend.feedback" -ApiResponse $recommendFeedback | Out-Null

    $coverSubmit = Invoke-Api -Name "cover.submit" -Method "POST" -Url "$BackendBase/api/admin/cover-analysis/videos/$videoId/tasks?analyzerType=rule"
    Ensure-ApiCodeZero -Name "cover.submit" -ApiResponse $coverSubmit | Out-Null

    $coverProcess = Invoke-Api -Name "cover.processNext" -Method "POST" -Url "$BackendBase/api/admin/cover-analysis/tasks/process-next"
    Ensure-ApiCodeZero -Name "cover.processNext" -ApiResponse $coverProcess | Out-Null

    $coverList = Invoke-Api -Name "cover.tags.list" -Method "GET" -Url "$BackendBase/api/admin/cover-analysis/videos/$videoId/tags"
    Ensure-ApiCodeZero -Name "cover.tags.list" -ApiResponse $coverList | Out-Null

    $coverCorrectBody = @{ tags = @("手工标签A", "手工标签B") } | ConvertTo-Json -Compress
    $coverCorrect = Invoke-Api -Name "cover.tags.correct" -Method "PATCH" -Url "$BackendBase/api/admin/cover-analysis/videos/$videoId/tags" -Body $coverCorrectBody
    Ensure-ApiCodeZero -Name "cover.tags.correct" -ApiResponse $coverCorrect | Out-Null

    $coverList2 = Invoke-Api -Name "cover.tags.list.afterCorrect" -Method "GET" -Url "$BackendBase/api/admin/cover-analysis/videos/$videoId/tags"
    Ensure-ApiCodeZero -Name "cover.tags.list.afterCorrect" -ApiResponse $coverList2 | Out-Null

    $dashFrom = (Get-Date).AddDays(-7).ToString("yyyy-MM-dd")
    $dashTo = (Get-Date).ToString("yyyy-MM-dd")

    $dashboardOverview = Invoke-Api -Name "dashboard.overview" -Method "GET" -Url "$BackendBase/api/admin/dashboard/overview"
    Ensure-ApiCodeZero -Name "dashboard.overview" -ApiResponse $dashboardOverview | Out-Null

    $dashboardTraffic = Invoke-Api -Name "dashboard.traffic" -Method "GET" -Url "$BackendBase/api/admin/dashboard/traffic-trend?from=$dashFrom&to=$dashTo"
    Ensure-ApiCodeZero -Name "dashboard.traffic" -ApiResponse $dashboardTraffic | Out-Null

    $dashboardGrowth = Invoke-Api -Name "dashboard.userGrowth" -Method "GET" -Url "$BackendBase/api/admin/dashboard/user-growth?from=$dashFrom&to=$dashTo"
    Ensure-ApiCodeZero -Name "dashboard.userGrowth" -ApiResponse $dashboardGrowth | Out-Null

    $dashboardFunnel = Invoke-Api -Name "dashboard.playFunnel" -Method "GET" -Url "$BackendBase/api/admin/dashboard/play-funnel?videoId=$videoId&from=$dashFrom&to=$dashTo"
    Ensure-ApiCodeZero -Name "dashboard.playFunnel" -ApiResponse $dashboardFunnel | Out-Null

    $frontendProxy = Invoke-Api -Name "frontend.proxy.me" -Method "GET" -Url "$FrontendBase/api/admin/auth/me" -AcceptStatus @(200, 404, 502)
    if ($null -ne $frontendProxy -and $frontendProxy.StatusCode -eq 200) {
      Ensure-ApiCodeZero -Name "frontend.proxy.me" -ApiResponse $frontendProxy | Out-Null
    } else {
      $statusText = if ($null -eq $frontendProxy) { "null" } else { [string]$frontendProxy.StatusCode }
      Add-Result -Name "frontend.proxy.me.note" -Passed $false -Detail "前端 dev 服务可能未运行或代理不可达，status=$statusText"
    }
  }

  Write-Host "" 
  Write-Host "=== Summary ===" -ForegroundColor Cyan
  $passCount = ($results | Where-Object { $_.Passed }).Count
  $failCount = ($results | Where-Object { -not $_.Passed }).Count
  Write-Host "PASS=$passCount FAIL=$failCount" -ForegroundColor Cyan

  $results | ForEach-Object {
    $flag = if ($_.Passed) { "PASS" } else { "FAIL" }
    Write-Output ("{0}`t{1}`t{2}" -f $flag, $_.Name, $_.Detail)
  }

  if ($failCount -gt 0) {
    exit 1
  }
  exit 0
}
finally {
  $client.Dispose()
}
