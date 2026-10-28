# 🌲 pet 项目 Git 分支与工程协作管理规范
> **标准基线**：完全对齐 `dianchu-demo` 工业级研发协作标准。通过**结构化分支层级**与 **`--no-ff` 非快进合并拓扑**，确保工程具备清晰的可视化分叉树（Git Graph）与可回溯历史。

---

## 一、分支架构层级模型

所有分支命名统一遵循 **三段式 / 四段式** 语义化命名法：

```text
<阶段/主线>/<项目/模块>/[类别]/<特性名>/<日期 YYMMDD>
```

### 1. 分支类型与命名规则

| 分支类型 | 命名格式 | 示例 | 职责说明 |
| :--- | :--- | :--- | :--- |
| **主发布分支** | `master` 或 `main` | `master` | 生产稳定基线，仅合并经测试验证的阶段主版本 |
| **阶段主迭代分支** | `mvp/<项目>/<YYMMDD>` | `mvp/pet/261028` | 当前迭代周期的核心集成分支，所有子特性分支均汇聚于此 |
| **功能特性分支** | `mvp/<项目>/feat/<特性名>/<YYMMDD>` | `mvp/pet/feat/doodle-pet/261028` | 独立功能研发（如动画、声音、菜单），开发完后合入阶段分支 |
| **缺陷修复分支** | `mvp/<项目>/fix/<问题简述>/<YYMMDD>` | `mvp/pet/fix/icon-duplicate/261028` | 修复编译报错、资源冲突或运行时 Bug |
| **文档与方案分支** | `docs/<模块名>/<YYMMDD>` | `docs/architecture/261028` | 需求文档、方案设计、汇报说明独立分支 |

---

## 二、Commit 提交信息规范

每个提交必须遵循 **Angular 规范 + 中文破折号详述**，严格禁止宽泛无意义的提交文字。

### 1. 结构模板
```text
<type>(<scope>): <精炼动词短语> — <详细解释说明/量化影响>
```

### 2. Type 动词类型词典
- **`feat`**：新增功能（新动作、新玩法、新界面）
- **`fix`**：修复 Bug 或构建报错
- **`style`**：视觉样式、美术贴图、图标、主题调整（不改变业务逻辑）
- **`refactor`**：代码重构（优化架构、抽取方法，不增删功能）
- **`perf`**：性能调优（减少 GC、降低能耗、提升流畅度）
- **`chore`**：构建配置、依赖升级、脚手架维护
- **`docs`**：文档更新、方案设计说明

### 3. Commit 范例对照

| 推荐规范（对齐 dianchu-demo） | 错误示范（禁止） |
| :--- | :--- |
| `feat(floating): 极简手绘线条小狗物理引擎 — 呼吸/摆尾/被抓受困/触底Q弹` | `update pet` |
| `style(icon): 替换 2D 极简手绘线条小狗图标 — 全面适配 Android 多分辨率 mipmap` | `fix icon` |
| `fix(build): 移除旧版冗余 webp 图标 — 解决 Gradle 资源合并报错` | `fix bug` |
| `docs(workflow): §1 建立 Git 工业级多分支开发与拓扑管理规范` | `add doc` |

---

## 三、标准开发与分支合并 SOP（生成彩色分支树的关键）

为什么普通的 Git 会变成一条直线，而 `dianchu-demo` 是一棵漂亮的**分叉树**？
核心就在于：**合并时必须加 `--no-ff`（Non-Fast-Forward，非快进合并）**。

### 标准协作 5 步法：

#### 步骤 1：从阶段主线切出特性分支
```bash
# 确保本地与远端阶段主线同步
git checkout mvp/pet/261028
git pull origin mvp/pet/261028

# 切出新的功能分支
git checkout -b mvp/pet/feat/doodle-pet/261028
```

#### 步骤 2：在特性分支上进行多次原子提交
```bash
git add .
git commit -m "style(icon): 替换 2D 极简手绘线条小狗图标 — 全面适配 mipmap"

git add .
git commit -m "feat(floating): 极简手绘线条小狗物理引擎 — 呼吸/摆尾/触底Q弹"
```

#### 步骤 3：推送到远端特性分支
```bash
git push -u origin mvp/pet/feat/doodle-pet/261028
```

#### 步骤 4：切回阶段主线，执行非快进合并（核心步骤！）
```bash
git checkout mvp/pet/261028

# 关键：--no-ff 会强制生成一个 Merge Commit，形成漂亮的分支回路
git merge --no-ff mvp/pet/feat/doodle-pet/261028 -m "Merge branch 'mvp/pet/feat/doodle-pet/261028' into mvp/pet/261028 — 整合极简手绘线条小狗悬浮窗动画与专属图标"
```

#### 步骤 5：推送到远端阶段主线
```bash
git push origin mvp/pet/261028
```
