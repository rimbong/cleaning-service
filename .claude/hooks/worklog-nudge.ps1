# Stop hook: nudge to record work history when there are unlogged source changes.
# Non-blocking (warn only). MUST never break the session -> always exit 0. ASCII-only.
try {
  # read stdin (Stop hook JSON)
  $raw = [Console]::In.ReadToEnd()
  $data = $null
  if ($raw) { try { $data = $raw | ConvertFrom-Json } catch {} }
  if ($data -and $data.stop_hook_active -eq $true) { exit 0 }   # loop guard

  # resolve project root
  $proj = $env:CLAUDE_PROJECT_DIR
  if (-not $proj -and $data -and $data.cwd) { $proj = $data.cwd }
  if (-not $proj) { $proj = (Get-Location).Path }

  # collect changed files (svn first, then git). exit silently if neither.
  $changed = @()
  if (Test-Path (Join-Path $proj ".svn")) {
    $out = & svn status --non-interactive 2>$null
    foreach ($l in $out) {
      if ($l.Length -ge 9 -and 'MADR'.Contains($l.Substring(0,1))) { $changed += $l.Substring(8).Trim() }
    }
  } elseif (Test-Path (Join-Path $proj ".git")) {
    $out = & git -C "$proj" status --porcelain 2>$null
    foreach ($l in $out) {
      if ($l.Length -ge 4 -and ($l.Substring(0,2) -match '[MADR]')) { $changed += $l.Substring(3).Trim() }
    }
  } else { exit 0 }

  # keep only source-ish changes (exclude meta/build dirs)
  $src = $changed | Where-Object {
    $_ -and
    $_ -notmatch '(^|[\\/])\.claude([\\/]|$)' -and
    $_ -notmatch '(^|[\\/])target([\\/]|$)'   -and
    $_ -notmatch '(^|[\\/])\.idea([\\/]|$)'   -and
    $_ -notmatch '(^|[\\/])\.metadata([\\/]|$)'
  }
  if (-not $src -or @($src).Count -eq 0) { exit 0 }

  # if a monthly worklog file is newer than newest changed source -> assume already logged, skip
  $logT = $null
  $wdir = Join-Path $proj ".claude\docs\worklog"
  if (Test-Path $wdir) {
    foreach ($lf in (Get-ChildItem $wdir -Filter *.md -ErrorAction SilentlyContinue)) {
      if (-not $logT -or $lf.LastWriteTime -gt $logT) { $logT = $lf.LastWriteTime }
    }
  }
  if ($logT) {
    $newest = $null
    foreach ($f in $src) {
      $p = Join-Path $proj $f
      if (Test-Path $p) { $t = (Get-Item $p).LastWriteTime; if (-not $newest -or $t -gt $newest) { $newest = $t } }
    }
    if ($newest -and $logT -ge $newest) { exit 0 }
  }

  # non-blocking warning to user (ASCII only to avoid console-encoding issues)
  $n = @($src).Count
  $msg = "[worklog] $n changed file(s) not yet recorded. Run /worklog to append this month's work history (.claude/docs/worklog)."
  ([ordered]@{ systemMessage = $msg } | ConvertTo-Json -Compress)
  exit 0
} catch { exit 0 }
