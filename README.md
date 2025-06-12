# NoIllegals

This plugin is designed to maintain a fair and balanced gameplay experience by automatically detecting and preventing the use of items that violate server rules or exceed vanilla Minecraft limits. It targets a variety of illegal modifications to items, ensuring that players cannot exploit the game unfairly.

# Features
### Custom Potion Detection
Prevents the use of potions with effects or durations that are not possible in vanilla Minecraft.

Blocks potions with non-standard effect levels (e.g., Level 255 or higher).

Removes potions with incompatible or custom attributes.

### Enchanted Items
Detects and removes items with enchantment levels beyond vanilla limits (e.g., Sharpness Level 10).

Ensures items only have enchantments that are appropriate for their type (e.g., no Efficiency on swords).

Blocks items with enchantments that are not possible in vanilla Minecraft (e.g., Mending on Totem of Undying.).

### Over-Stacked Items

Detects and prevents stacks of items exceeding the normal stack limit (e.g., 65+ blocks or stacked tools).

Automatically adjusts illegal stacks to their proper count or removes them entirely.

### Attribute Modifiers

Identifies and removes items with attribute modifiers

### Unbreakable Items

Detects and removes items marked as "unbreakable" if this is against server rules.
Restores the original durability settings where applicable.

### Additional Benefits

Deletes Unobtainable items.

Keeps the server environment fair and fun for all players.

Lightweight and efficient, ensuring minimal impact on server performance.
Customizable settings to enable or disable specific checks based on server preferences.

# Commands
```/illegal``` the main command.

# Permissions
```illegal.cmd```: the main permission.
```illegal.alerts```: The permission to see alerts.
```illegal.bypass```: Bypass the illegal check.