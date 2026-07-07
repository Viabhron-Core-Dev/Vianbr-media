with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "r") as f:
    content = f.read()

target = """                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                            }
                            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                DropdownMenuItem(text = { Text("Sort by Name") }, onClick = { sortOrder = SortOrder.NAME; showSortMenu = false })
                                DropdownMenuItem(text = { Text("Sort by Date") }, onClick = { sortOrder = SortOrder.DATE; showSortMenu = false })
                            }
                        }
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }"""

replacement = """                        Box {
                            var showOverflowMenu by remember { mutableStateOf(false) }
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More Options")
                            }
                            DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                                DropdownMenuItem(text = { Text("Sort by Name") }, onClick = { sortOrder = SortOrder.NAME; showOverflowMenu = false })
                                DropdownMenuItem(text = { Text("Sort by Date") }, onClick = { sortOrder = SortOrder.DATE; showOverflowMenu = false })
                                DropdownMenuItem(text = { Text("Settings") }, onClick = { showSettingsDialog = true; showOverflowMenu = false })
                            }
                        }"""

content = content.replace(target, replacement)
content = content.replace("    var showSortMenu by remember { mutableStateOf(false) }\n", "")

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "w") as f:
    f.write(content)
