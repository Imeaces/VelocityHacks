# AGENTS.md

本仓库的通用约定，供在此仓库中工作的 agent 与协作者参考。

- 项目结构与构建/运行方式见 `README.md`。
- 本机环境相关的约定见 `AGENTS.local.md`（不随仓库共享）。

## Mixin

### 注解的真实来源

本项目**不使用 SpongePowered Mixin 运行时**，而是由 KeitaLoader 在类加载阶段完成类变换：

1. 编译期：`org.spongepowered.asm.mixin.*` 与 `com.llamalad7.mixinextras.*` 注解由 `net.lenni0451.classtransform:mixinsdummy` 提供的占位类满足（`compileOnlyApi`，**运行时不加载**；该包同时提供了 MixinExtras 的占位注解，无需额外依赖）。
2. 运行时：`net.lenni0451.classtransform:mixinstranslator` 的 `MixinsTranslator` 把这些注解改写成 ClassTransform 自己的 `@C*` 注解，再由 ClassTransform 执行变换。

注册位置：`KeitaLoader/launcher/src/main/java/org/imeaces/keitaload/KeitaLoader.java`
依赖声明：`KeitaLoader/launcher/build.gradle.kts`（`classtransform` 1.15.1）

> 由此得出的关键结论：**一个注解是否生效，取决于 MixinTranslator 是否为它注册了翻译器，而不是取决于上游 Mixin / MixinExtras 是否支持它。**
> 未注册的注解不会被改写，会原样留在字节码里并被 ClassTransform 忽略——也就是**静默失效，不报错**。

### 受支持的注解

下表来自 `mixinstranslator` 的 `AnnotationTranslatorManager` 静态注册表（1.15.1），只有其中的注解会被翻译：

| 注解 | 改写为 |
| --- | --- |
| `@Mixin` | `@CTransformer` |
| `@Shadow` | `@CShadow` |
| `@Overwrite` | `@COverride` |
| `@Inject` | `@CInject` |
| `@Redirect` | `@CRedirect` |
| `@ModifyConstant` | `@CModifyConstant` |
| `@At` | At 翻译 |
| `@Slice` | Slice 翻译 |
| `@Local`（MixinExtras） | Local 翻译 |
| `@Share`（MixinExtras） | `@CShared` |
| `@WrapWithCondition`（MixinExtras，含 `injector.v2`） | WrapWithCondition 翻译 |
| `@ModifyExpressionValue`（MixinExtras） | ModifyExpressionValue 翻译 |

### 不受支持的注解：不要使用

以下注解**没有**注册翻译器，写了能编译通过，但不会生效：

- `@Final`、`@Mutable`、`@Unique`
- `@Accessor`、`@Invoker`
- `@ModifyArg`、`@ModifyArgs`、`@ModifyVariable`、`@ModifyReturnValue`、`@ModifyReceiver`
- `@Debug`、`@Dynamic`、`@Pseudo`、`@Implements`、`@Interface`、`@Coerce`

> **不要依赖编译警告来判断注解是否可用。** `mixinsdummy` 只把 `@Final`、`@Mutable`、`@Unique` 标记为
> `@Deprecated`，因此只有这三个会产生"已过时"的编译警告；`@Accessor`、`@Invoker`、`@ModifyArg`、
> `@ModifyVariable` 等同样不受支持的注解**完全没有警告**，会静默失效。以本节清单和上面的注册表为准。

### 替代做法

- **需要只属于自己的辅助方法/字段时**：不要用 `@Unique`。改用带唯一前缀的普通 `private` 成员（本项目惯用 `levelLogin$` 前缀），翻译后会被并入目标类。
- **需要读写目标类成员时**：用 `@Shadow`（受支持）；不要用 `@Accessor` / `@Invoker`。
- **需要覆盖整个方法时**：用 `@Overwrite`（受支持）。
- **需要改动常量时**：用 `@ModifyConstant`（受支持），不要用未支持的 `@ModifyArg` / `@ModifyVariable`。
- **需要捕获局部变量时**：用 MixinExtras 的 `@Local`（受支持，依赖目标类的 `LocalVariableTable`，务必先确认目标产物确实带局部变量名）。
- **需要跨注入点传递值**：用 MixinExtras 的 `@Share`（受支持）。

### Mixin 类的位置与注册

- 源码目录：`LevelLoginMod/src/main/java/org/imeaces/levellogin/mixin/`
- 注册文件：`LevelLoginMod/src/main/resources/keitaload-mod.mixins.json`（新增 Mixin 类后必须同步添加）

### 维护提示

上面的支持清单与 1.15.1 的注册表一一对应。升级 `classtransform` 版本后需要重新核对：查看 `mixinstranslator` 的 `AnnotationTranslatorManager` 静态注册表，只有出现在其中的注解才会被翻译。

## Access Widener

### 一个文件，两个阶段

`LevelLoginMod/src/main/resources/keitaload.accesswidener` 同时服务编译期和运行时：

| 阶段 | 由谁应用 | 作用 |
| --- | --- | --- |
| 编译期 | Gradle 插件 `me.glicz.access-widen`（3.0.0，见 `LevelLoginMod/build.gradle.kts`） | 把加宽后的依赖变体放进编译 classpath，让源码能引用原本不可见的类/方法/字段 |
| 运行时 | KeitaLoader | 读取模组 jar 内的 `keitaload.accesswidener`，在类加载时改写目标类的访问标志 |

编译期：插件把 `src/main/resources` 下的 `*.accesswidener` 收集进 `accessWiden` 扩展，再通过
`compileOnlyApi(accessWiden("com.velocitypowered:velocity-proxy:..."))` 应用到依赖上。

运行时：`ModsResourceManager.KEITA_ACCESS_WIDENER_CONF_FILE`（值为 `keitaload.accesswidener`）从每个模组 jar
收集条目 → `AccessWidenerManager.init()` 用 `net.fabricmc:access-widener`（`KeitaLoader/launcher/build.gradle.kts`）
合并成单个 `AccessWidener` → `createTransformer()` 返回的 `ClassFileTransformer` 由 `KeitaAgent.INSTRUMENTATION`
注册，在类加载时经 `AccessWidenerClassVisitor` 改写。

> 运行时读的文件名是硬编码契约：必须是 `keitaload.accesswidener`。改名会让运行时静默读不到，而编译期仍然正常。

### 注意点

- **编译期目前只加宽了 `velocity-proxy`**（只有它是 `accessWiden(...)` 的参数）。若给其他产物（`velocity-api`、floodgate 等）添加条目，运行时能生效但编译器看不到，需要同时把该产物也包进 `accessWiden(...)`。
- **两端必须是同一份代码**：编译期加宽的 jar 与运行时被改写的类若来自不同构建，会出现"编译通过、运行时 `IllegalAccessError`"。`velocity-proxy` 来自 `includeBuild("VelocityServer")`，因此改动 AW 后需要按 `README.md` 的顺序先重建 VelocityServer。
- **新增条目后要重新构建**：Gradle 插件需要重新生成加宽后的依赖变体，否则编译器看到的仍是旧视图。
- AW 只放宽访问性，不改变方法签名。格式为 `accessWidener v2 named`（Velocity 未混淆，直接用 named），条目形如 `accessible class ...` / `accessible method ...` / `accessible field ...`。
- 现有条目（`LevelLoginMod/src/main/resources/keitaload.accesswidener`）都是为 mixin 服务的：`InitialLoginSessionHandler$LoginState`、`AuthSessionHandler` 构造器、`EncryptionRequestPacket.shouldAuthenticate`、`LoginInboundConnection.delegatedConnection()`。
