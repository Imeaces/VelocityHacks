# Imeaces Origin Server Entrypoint V2

Imeaces Origin 服务器的下一代入口设施，缓慢开发中……

## Build

本项目使用 [Gradle](https://gradle.org/) 构建，并提供 Gradle Wrapper。

由于当前 `LevelLoginMod` 的 Access Widener 依赖需要直接读取 `VelocityServer` 的构建产物，而该依赖暂时无法在 Gradle 中正确声明，因此需要按照以下顺序进行构建：

1. 构建 Velocity Server：

   ```bash
   ./gradlew :VelocityServer:assemble
   ```

2. 构建应用启动器：

   ```bash
   ./gradlew :app:installDist
   ```

   构建完成后，可运行的文件集位于：

   ```text
   app/build/install/VelocityHacks/
   ```

3. 构建 LevelLogin Velocity 插件：

   ```bash
   ./gradlew :LevelLoginPlugin:assemble
   ```

   构建完成后，可在以下位置找到插件：

   ```text
   LevelLoginPlugin/build/libs/LevelLoginPlugin-1.0.0-all.jar
   ```

## 运行

`app` 模块是整个服务器的启动包装器。它负责启动 Velocity Server，并同时加载 KeitaLoader 与 LevelLoginMod。

运行前：

1. 将构建得到的 `LevelLoginPlugin-1.0.0-all.jar` 放入 Velocity Server 的插件目录。
2. 准备好需要运行的 Velocity Server 文件。
3. 使用 `app` 模块生成的启动器运行服务器。

例如：

```bash
./app/build/install/VelocityHacks/bin/VelocityHacks <velocity-server.jar>
```

具体启动参数以当前 `app` 模块的命令行参数实现为准。

### 构建产物

完成上述构建步骤后，主要需要关注：

```text
app/build/install/VelocityHacks/
└── bin/
    └── VelocityHacks

LevelLoginPlugin/build/libs/
└── LevelLoginPlugin-1.0.0-all.jar
```
