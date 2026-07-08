# 可视化配方编写 (Workstation Recipe Exporter)

[English](#english) | [中文](#中文)

## 中文

### 导出格式
- **JSON格式** - Minecraft原版的数据包配方格式
- **KubeJS格式** - KubeJS模组的JS配方格式

### 使用示例
```
/re <workstation_type>
/recipeexporter <workstation_type>
/recipeexporter <+下方对应的工作方块id打开不同界面> [示例：/recipeexporter furnace]
- crafting - 工作台
- furnace - 熔炉
- blast_furnace - 高炉
- smoker - 烟熏炉
- campfire - 营火[还未添加]
- stonecutter - 切石机
- smithing - 锻造台
- cutting_board / farmersdelight:cutting_board - Farmer's Delight 砧板
- cooking_pot / farmersdelight:cooking_pot - Farmer's Delight 烹饪锅

> 注：Farmer's Delight 的砧板与烹饪锅目前仅支持导出 JSON，不提供 KubeJS 导出。
```

### 导出位置

#### JSON配方文件
- 路径：`<游戏目录>/exported_recipes/`
- 文件名格式：`<随机字符串>.json`

#### KubeJS脚本文件
- 路径：`<游戏目录>/kubejs/server_scripts/`
- 文件名：`exported_recipes.js`

---

## English

### Export Formats
- **JSON Format** - Minecraft vanilla datapack recipe format
- **KubeJS Format** - KubeJS mod's JavaScript recipe format

### Usage Examples
```
/re <workstation_type>
/recipeexporter <workstation_type>
/recipeexporter <+workstation_block_id_below_to_open_different_interfaces> [Example: /recipeexporter furnace]
- crafting - Crafting Table
- furnace - Furnace
- blast_furnace - Blast Furnace
- smoker - Smoker
- campfire - Campfire [Not yet added]
- stonecutter - Stonecutter
- smithing - Smithing Table
- cutting_board / farmersdelight:cutting_board - Farmer's Delight Cutting Board
- cooking_pot / farmersdelight:cooking_pot - Farmer's Delight Cooking Pot

> Note: Farmer's Delight Cutting Board and Cooking Pot currently support JSON export only, with no KubeJS export.
```

### Export Locations

#### JSON Recipe Files
- Path: `<game_directory>/exported_recipes/`
- File name format: `<random_string>.json`

#### KubeJS Script Files
- Path: `<game_directory>/kubejs/server_scripts/`
- File name: `exported_recipes.js`