import re

with open("app/src/main/java/com/dettle/app/data/backup/BackupManager.kt", "r") as f:
    content = f.read()

# Remove import
content = re.sub(r'import android\.webkit\.CookieManager\n', '', content)

# BackupOptions
content = re.sub(r'\s*val includeCookies: Boolean = true,', '', content)
content = re.sub(r'includeCookies && ', '', content)
content = re.sub(r'!includeCookies && ', '', content)

# BackupSummary
content = re.sub(r'\s*val cookieDomainCount: Int = 0,', '', content)

# RestoreResult
content = re.sub(r'\s*val cookiesRestored: Int = 0,', '', content)

# Docs
content = re.sub(r'\s*\* 3\. Live browser session cookies via Android CookieManager across all AI & developer domains\.', '', content)

# getLiveSummary logic
content = re.sub(r'\s*// Count domains with active cookies\n\s*val cookieDomains = getAllTargetCookieDomains\(\)\n\s*val cookieCount = withContext\(Dispatchers\.Main\) \{\n\s*val cm = CookieManager\.getInstance\(\)\n\s*cookieDomains\.count \{ url -> !cm\.getCookie\(url\)\.isNullOrBlank\(\) \}\n\s*\}', '', content)

content = re.sub(r'\s*cookieDomainCount = cookieCount,', '', content)

# wellKnownCookieDomains
content = re.sub(r'\s*private val wellKnownCookieDomains = listOf\([\s\S]*?\)', '', content)

# getAllTargetCookieDomains
content = re.sub(r'\s*private fun getAllTargetCookieDomains\(\): List<String> \{[\s\S]*?\}', '', content)

# Backup generation block
backup_block = r'\s*// 3\. Browser Sessions & Cookies\n\s*if \(options\.includeCookies\) \{[\s\S]*?root\.put\("cookies", cookieList\)\n\s*\}'
content = re.sub(backup_block, '', content)

# Restore logic blocks
content = re.sub(r'\s*var cookiesCount = 0', '', content)
restore_block = r'\s*// 3\. Browser Sessions & Cookies\n\s*if \(options\.includeCookies && root\.has\("cookies"\)\) \{[\s\S]*?cookiesCount\+\+\n\s*\}\n\s*\}'
content = re.sub(restore_block, '', content)
# It's better to just do string replacements for the specific restore block if regex is tricky, but let's try a regex for the restore block that ends with cookiesCount++ and a few braces. Let's make it simpler: match from "// 3. Browser Sessions & Cookies" up to the next "// 4. Cognitive Memories"
restore_block2 = r'\s*// 3\. Browser Sessions & Cookies[\s\S]*?(?=\s*// 4\. Cognitive Memories)'
content = re.sub(restore_block2, '\n', content)

content = re.sub(r'\s*cookiesRestored = cookiesCount,', '', content)

with open("app/src/main/java/com/dettle/app/data/backup/BackupManager.kt", "w") as f:
    f.write(content)
