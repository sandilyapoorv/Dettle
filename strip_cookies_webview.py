import re

with open("app/src/main/java/com/dettle/app/data/webview/WebViewSession.kt", "r") as f:
    content = f.read()

# Remove import
content = re.sub(r'import android\.webkit\.CookieManager\n', '', content)

# Remove comment mentioning it
content = re.sub(r'\s*\* Cookies persist across app restarts via CookieManager with cookie persistence enabled\.', '', content)

# Remove instantiation block
content = re.sub(r'\s*// Persist cookies across app restarts\n\s*CookieManager\.getInstance\(\)\.apply \{\n\s*setAcceptCookie\(true\)\n\s*setAcceptThirdPartyCookies\(wv, true\)\n\s*\}', '', content)

# Remove flush
content = re.sub(r'\s*// Flush cookies to disk immediately\n\s*CookieManager\.getInstance\(\)\.flush\(\)', '', content)

with open("app/src/main/java/com/dettle/app/data/webview/WebViewSession.kt", "w") as f:
    f.write(content)
