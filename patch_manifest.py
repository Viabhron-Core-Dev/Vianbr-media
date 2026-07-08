import re

with open("app/src/main/AndroidManifest.xml", "r") as f:
    content = f.read()

content = content.replace('android:label="Batch Convert — Vianbr Media"', 'android:label="Batch Compress"')
content = content.replace('android:label="Play — Vianbr Media"', 'android:label="Play"')
content = content.replace('android:label="Edit — Vianbr Media"', 'android:label="Edit"')

with open("app/src/main/AndroidManifest.xml", "w") as f:
    f.write(content)
