import re

with open("app/src/main/java/com/example/ui/components/MiniPlayerOverlay.kt", "r") as f:
    content = f.read()

# Let's clean up the multiple variables and functions
content = re.sub(r'var loopMode by remember.*?Player\.REPEAT_MODE_OFF\)\}?', '', content)
content = re.sub(r'var shuffleMode by remember.*?false\)\}?', '', content)

# Now find the line 'var currentIndex by remember' and add them back exactly ONCE
content = re.sub(
    r'(var currentIndex by remember \{ mutableIntStateOf\(player\?\.currentMediaItemIndex \?: 0\) \})',
    r'\1\n    var loopMode by remember { mutableIntStateOf(player?.repeatMode ?: Player.REPEAT_MODE_OFF) }\n    var shuffleMode by remember { mutableStateOf(player?.shuffleModeEnabled ?: false) }',
    content, count=1
)

# Remove the multiple onRepeatModeChanged and onShuffleModeEnabledChanged
content = re.sub(r'override fun onRepeatModeChanged\(repeatMode: Int\) \{\s+loopMode = repeatMode\s+\}', '', content)
content = re.sub(r'override fun onShuffleModeEnabledChanged\(shuffleModeEnabled: Boolean\) \{\s+shuffleMode = shuffleModeEnabled\s+\}', '', content)

content = re.sub(
    r'(override fun onTimelineChanged\(timeline: androidx\.media3\.common\.Timeline, reason: Int\) \{)',
    r'override fun onRepeatModeChanged(repeatMode: Int) {\n                loopMode = repeatMode\n            }\n            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {\n                shuffleMode = shuffleModeEnabled\n            }\n            \1',
    content, count=1
)

# Clean up initial setup overrides
content = re.sub(r'loopMode = player\.repeatMode\s+shuffleMode = player\.shuffleModeEnabled', '', content)

content = re.sub(
    r'(duration = player\.duration\.coerceAtLeast\(1L\))',
    r'\1\n                loopMode = player.repeatMode\n                shuffleMode = player.shuffleModeEnabled',
    content
)

with open("app/src/main/java/com/example/ui/components/MiniPlayerOverlay.kt", "w") as f:
    f.write(content)

