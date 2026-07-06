import re

with open("app/src/main/java/com/example/service/PlayerManager.kt", "r") as f:
    content = f.read()

# Fix setBoostGain
set_boost = """    fun setBoostGain(gainMb: Int) {
        if (gainMb <= 0) {
            loudnessEnhancer?.enabled = false
        } else {
            loudnessEnhancer?.setTargetGain(gainMb)
            loudnessEnhancer?.enabled = true
        }
    }
    
    fun applyAudioBoosterSettings(enabled: Boolean, gainMb: Int) {
        if (!enabled || gainMb <= 0) {
            loudnessEnhancer?.enabled = false
        } else {
            loudnessEnhancer?.setTargetGain(gainMb)
            loudnessEnhancer?.enabled = true
        }
    }"""

content = re.sub(r"    fun setBoostGain\(gainMb: Int\) \{.*?\n    \}", set_boost, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/service/PlayerManager.kt", "w") as f:
    f.write(content)
