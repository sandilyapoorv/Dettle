import re

for filepath in ["docs/dettle-architecture.html", "docs/AI_MODELS_AND_PROVIDERS.md", "CHANGELOG.md", "task.md"]:
    with open(filepath, "r") as f:
        content = f.read()
    
    if filepath == "CHANGELOG.md":
        content = re.sub(r'2\. \*\*Browser Sessions & Cookies\*\*.*?\n', '', content)
        content = re.sub(r'Universal Full-State Backup & Restore Engine \(APIs, Cookies, Cognitive Memories, Preferences\)', 'Universal Full-State Backup & Restore Engine (APIs, Cognitive Memories, Preferences)', content)
    
    if filepath == "docs/AI_MODELS_AND_PROVIDERS.md":
        content = re.sub(r'\* Persistent cookie sessions via Android WebView:.*?\n', '', content)

    if filepath == "task.md":
        content = re.sub(r'- \[x\] WebViewSession — per-provider hidden WebView, cookie persistence, login detection, rate limit handling', '- [x] WebViewSession — per-provider hidden WebView, login detection, rate limit handling', content)
        
    with open(filepath, "w") as f:
        f.write(content)
