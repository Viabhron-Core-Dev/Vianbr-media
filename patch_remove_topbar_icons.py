import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

# I will replace the IconButton for repeatMode and backgroundPlayEnabled with nothing.
repeat_button_regex = r"                            IconButton\(onClick = \{\s*val nextMode = when \(repeatMode\) \{\s*androidx\.media3\.common\.Player\.REPEAT_MODE_OFF -> androidx\.media3\.common\.Player\.REPEAT_MODE_ALL\s*androidx\.media3\.common\.Player\.REPEAT_MODE_ALL -> androidx\.media3\.common\.Player\.REPEAT_MODE_ONE\s*else -> androidx\.media3\.common\.Player\.REPEAT_MODE_OFF\s*\}\s*repeatMode = nextMode\s*mediaController\?\.repeatMode = nextMode\s*\}\) \{\s*val repeatIcon = when \(repeatMode\) \{\s*androidx\.media3\.common\.Player\.REPEAT_MODE_ONE -> Icons\.Filled\.RepeatOne\s*else -> Icons\.Filled\.Repeat\s*\}\s*val repeatTint = if \(repeatMode == androidx\.media3\.common\.Player\.REPEAT_MODE_OFF\) Color\.White else Color\(0xFF2196F3\)\s*Icon\(repeatIcon, contentDescription = \"Repeat\", tint = repeatTint\)\s*\}\n"

bgplay_button_regex = r"                            IconButton\(onClick = \{\s*backgroundPlayEnabled = !backgroundPlayEnabled\s*Toast\.makeText\(context, \"Background play \" \+ if \(backgroundPlayEnabled\) \"enabled\" else \"disabled\", Toast\.LENGTH_SHORT\)\.show\(\)\s*\}\) \{\s*Icon\(Icons\.Filled\.Headphones, contentDescription = \"Background play\", tint = if \(backgroundPlayEnabled\) Color\(0xFF2196F3\) else Color\.White\)\s*\}\n"

content = re.sub(repeat_button_regex, "", content)
content = re.sub(bgplay_button_regex, "", content)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
