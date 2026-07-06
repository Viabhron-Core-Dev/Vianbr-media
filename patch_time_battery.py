import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

old_box = """            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
            ) {"""

new_box = """            Box(
                modifier = Modifier
                    .padding(end = 32.dp)
            ) {"""

old_row = """                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(4.dp)
                ) {"""

new_row = """                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp, top = 0.dp)
                ) {"""

content = content.replace(old_box, new_box)
content = content.replace(old_row, new_row)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
