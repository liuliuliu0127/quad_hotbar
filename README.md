## 中文

# 四重快捷栏 (Quad Hotbar)

> 魔改了双重快捷栏模组使其支持4重快捷栏的显示与快捷交互.  
> 目前支持我的世界 1.21.9–1.21.11 版本

**四重快捷栏 (Quad Hotbar)** 是双重快捷栏 (Double Hotbar) 模组的一个分支版本，灵感来源于 多重快捷栏、忄夬扌疌木兰 及 HyskLongHotbar 模组。  
本模组**完整保留**了原双重快捷栏的全部功能，并新增了**2×2 四重快捷栏布局**——相当于两个双重快捷栏并排，屏幕底部最多同时显示 18 个物品格子。

> **注意**  
> 本模组**并不会**实际扩展玩家的快捷栏容量或背包容量，它只改变了 HUD 显示和物品切换逻辑。  
> 本模组纯客户端运行，服务器无需安装。

### 主要特性
1. **四重快捷栏布局** – 四行物品栏以左右两列显示，上半部分固定，下半部分根据选择动态切换。  
2. **智能滚动** – 鼠标滚轮可在底部 18 个格子间循环移动；从左侧（0–8 格）滚动到右侧（9–17 格）时，会**自动把整个真实快捷栏与背包最底层物品互换**。  
3. **快捷键**  
   - `1`–`9` 选择左下快捷栏格子；`Ctrl`(默认，可配置) + `1`–`9` 选择右下格子。  
   - 双击数字键（或 `Ctrl`(默认，可配置) + 双击）可交换该格与上方对应格的物品。  
   - 交换键（默认 `R`）用于整行交换。  
4. **保留原双重快捷栏全部功能** – 长按交换、双击窗口、音效等完全可用。

### 布局说明
> 左上：背包第3行（18-26） | 右上：背包第2行（9-17）
> （左下：快捷栏（0-8） | 右下：背包最底层（27-35））或（左下：背包最底层（27-35） | 右下：快捷栏（0-8））

活跃行（带有高亮选择框的那一行）会随着滚轮或数字键在左右之间动态切换，并在跨边界时自动置换物品。

### 默认操作
| 操作                     | 按键                            |
|--------------------------|--------------------------------|
| 整行交换                 | 短按 `R`（可配置）               |
| 交换单个选中格           | 长按 `R`（可配置）                |
| 选择左下格子 1–9         | `1` – `9`                       |
| 选择右下格子 1–9         | `Ctrl`（可配置） + `1` – `9`     |
| 双击左侧单格交换         | 双击 `1` – `9`                   |
| 双击右侧单格交换         | `Ctrl`（可配置） + 双击 `1` – `9` |
| 滚轮切换18个底层格子     | 鼠标滚轮                          |
| 修饰键（可自定义）       | 默认 `Ctrl`（可配置）              |
| 只交换真实快捷栏整行     | `Ctrl`（可配置） + 短按 `R`（可配置）|
| 交换背包最底层与上层     | `Ctrl`（可配置） + 长按 `R`（可配置）|

你可以在 **选项 → 按键控制** 中自由修改所有键位。用于组合操作的修饰键也是一个独立的按键项（“四行快捷栏修饰键”）。

### 安装方法
1. 安装对应版本的 **Fabric Loader**。
2. 下载 **Fabric API** 和**Cloth Config API**并放入 `mods` 文件夹。
3. 将 Quad Hotbar 模组文件放入 `mods` 文件夹。
4. 启动游戏。若安装了 **Mod Menu**，可在模组列表中直接打开配置界面。

### 配置说明
模组使用 **Cloth Config** 提供游戏内配置界面且基础设置与原双重快捷栏保持一致（推荐搭配 Mod Menu），你也可以手动编辑 `config/double_hotbar.json`(本模组与原双重快捷栏共享配置文件)。  
主要选项：
- `quadHotbar` – 开启/关闭四重快捷栏模式。
- `inventoryRow` – 双行模式下显示背包的哪一行。
- `shift` – 额外快捷栏的垂直偏移。
- `renderCrop` – 第二行快捷栏背景的裁剪高度。
- `holdToSwap` / `holdToSwapBar` – 控制 `R` 键的行为。
- `allowDoubleTap` / `doubleTapWindow` – 启用并设置双击交换。
- `wooshVolume` – 交换音效音量。
- `reverseBars` – 交换主快捷栏与额外行的上下位置。
- `useSwapForHotbar` – 使用 `SWAP` 操作代替 `PICKUP` 进行某些交换。

---

## English

# Quad Hotbar

> Modified double hotbar mod that expands the HUD for more than 2 rows of hotbar.  
> Currently supports Minecraft 1.21.9–1.21.11

**Quad Hotbar** is a fork of the Double Hotbar mod, inspired by Multi Hotbar, Hot Baaaar, and Hysk LongHotbar.  
It retains **all** original Double Hotbar features while adding a new 2×2 quad hotbar layout – essentially two double hotbars side by side, giving you 18 visible inventory slots at the bottom of the screen.

> **Important**  
> This mod does **not** actually expand your hotbar or inventory capacity. It only makes extra inventory rows visible and easier to manage.  
> It is a **client‑side only** mod; no server installation is required.

### Features
1. **Quad Hotbar Layout** – Four inventory rows are shown in two columns (left/right).  
2. **Smart Scrolling** – Mouse wheel scrolls through all 18 bottom slots. Moving from left (slots 0–8) to right (slots 9–17) automatically swaps your real hotbar with the bottom inventory row.  
3. **Keybinds**  
   - Number keys `1–9` select the left bottom row; `Ctrl` + `1–9` select the right bottom row.  
   - Double‑tap a number (or `Ctrl` + double‑tap) swaps that single slot with the corresponding slot above.  
   - The swap key (default `R`) exchanges entire rows.  
4. **All original Double Hotbar features** – hold‑to‑swap, double‑tap window, sound effects, etc. remain fully functional.

### Layout

> Top left: inventory[18..26] | Top right: inventory[9..17]
> (Bottom left: hotbar (0..8) | Bottom right: inventory[27..35]) or (Bottom right: inventory[27..35] | Bottom left: hotbar (0..8))

The active row (where the selection highlight appears) switches dynamically between left and right as you scroll or use number keys. When crossing the border, the entire real hotbar is swapped with the bottom inventory row automatically.

### Default Controls
| Action                                   | Key                                            |
|------------------------------------------|------------------------------------------------|
| Swap active row with upper row           | Short press `R`(configurable)                  |
| Swap single selected slot                | Long press `R` (configurable)                  |
| Select left bottom slot 1–9              | `1` – `9`                                      |
| Select right bottom slot 1–9             | `Ctrl`(configurable) + `1` – `9`               |
| Double‑tap swap (left)                   | Double‑tap `1` – `9`                           |
| Double‑tap swap (right)                  | `Ctrl`(configurable) + double‑tap `1` – `9`    |
| Scroll through all 18 bottom slots       | Mouse wheel                                    |
| Modifier key (configurable)              | Default `Ctrl`                                 |
| Swap only real hotbar row                | `Ctrl`(configurable) + short `R`(configurable) |
| Swap bottom‑most inventory rows          | `Ctrl`(configurable) + long `R`(configurable)  |

You can change all keybindings in the **Options → Controls** menu. The modifier key used together with `1`–`9` and `R` is its own separate binding (`Quad Hotbar Modifier Key`).

### Installation
1. Install **Fabric Loader** for Minecraft 1.17.1–1.21.11.
2. Install **Fabric API** and **Cloth Config API**.
3. Place the Quad Hotbar `.jar` file into your `mods` folder.
4. Launch the game. If you have **Mod Menu**, you can access the configuration screen directly in-game.

### Configuration
The mod uses **Cloth Config** to provide an in‑game config screen (Mod Menu recommended) which has all configurables settings from original Double Hotbar mod. You can also manually edit `config/double_hotbar.json`(It shares the same config file with the original double hotbar mode).  
Key options:
- `quadHotbar` – Enable/disable the 2×2 quad layout.
- `inventoryRow` – Which inventory row to show in double‑bar mode.
- `shift` – Vertical position of the extra hotbars.
- `renderCrop` – Crop height of the secondary hotbar background.
- `holdToSwap` / `holdToSwapBar` – Control the `R` key behaviour.
- `allowDoubleTap` / `doubleTapWindow` – Enable and configure double‑tap swapping.
- `wooshVolume` – Volume of the swap sound.
- `reverseBars` – Swap main hotbar and extra row positions.
- `useSwapForHotbar` – Use `SWAP` instead of `PICKUP` for certain exchange.