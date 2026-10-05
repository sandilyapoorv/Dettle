import re

with open("app/src/main/java/com/dettle/app/ui/backup/BackupRestoreSheet.kt", "r") as f:
    content = f.read()

# var includeCookies by remember { mutableStateOf(true) }
content = re.sub(r'\s*var includeCookies by remember \{ mutableStateOf\(true\) \}', '', content)
content = re.sub(r'includeCookies, ', '', content)
content = re.sub(r'\s*includeCookies = includeCookies,', '', content)
content = re.sub(r'\s*includeCookies = target', '', content)

# Remove the whole row
row_pattern = r'\s*BackupOptionRow\(\n\s*title = "Browser Sessions & Cookies",\n\s*subtitle = "Web login sessions for Claude, ChatGPT, Grok, DeepSeek\.\.\.",\n\s*checked = includeCookies,\n\s*onCheckedChange = \{ includeCookies = it \}\n\s*\)'
content = re.sub(row_pattern, '', content)

with open("app/src/main/java/com/dettle/app/ui/backup/BackupRestoreSheet.kt", "w") as f:
    f.write(content)
