import re

with open("app/src/main/java/com/dettle/app/data/backup/BackupManager.kt", "r") as f:
    content = f.read()

# Remove dangling return block
dangling_block = r'\s*return \(wellKnownCookieDomains \+ dynamicDomains\)[\s\S]*?\.distinct\(\)\n\s*\}'
content = re.sub(dangling_block, '', content)

with open("app/src/main/java/com/dettle/app/data/backup/BackupManager.kt", "w") as f:
    f.write(content)
