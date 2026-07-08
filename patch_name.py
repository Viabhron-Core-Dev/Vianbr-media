import re

def replace_in_file(filepath, target, replacement):
    with open(filepath, 'r') as f:
        content = f.read()
    content = content.replace(target, replacement)
    with open(filepath, 'w') as f:
        f.write(content)

replace_in_file("app/src/main/res/values/strings.xml", "Vianbr Play", "Vianbhr Media")
replace_in_file("app/src/main/java/com/example/ui/screens/WelcomeScreen.kt", "Welcome to Vianbr Play", "Welcome to Vianbhr Media")
replace_in_file("app/src/main/java/com/example/ui/screens/MainScreen.kt", "Vianbr Play", "Vianbhr Media")
replace_in_file("settings.gradle.kts", 'rootProject.name = "My Application"', 'rootProject.name = "Vianbhr Media"')
replace_in_file("metadata.json", '"name": "Vianbr media"', '"name": "Vianbhr Media"')

