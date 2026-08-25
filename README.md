# Halo 创作者申请插件

面向 Halo 2.26 的两阶段创作者申请插件。

## 当前功能

- `/creator/apply` 自助申请页面（必须登录）
- 自动读取 Halo 登录用户名和网站昵称
- 第一阶段：QQ 群截图 + 申请理由
- 第二阶段：文章名称 + 文章链接
- 阻止同一用户同阶段重复提交待审核申请
- 控制台“内容 → 创作者申请”审核页面
- 通过第一阶段后授予 Halo 内置 `post-contributor`（投稿者）角色
- 通过第二阶段后授予 Halo 内置 `post-author`（作者）角色
- 保存驳回原因与审核记录

## 构建与安装

环境要求：JDK 21、Node.js 20+、pnpm 9+。

```bash
./gradlew clean build
```

构建产物位于 `build/libs/`。在 Halo 控制台上传 JAR 并启动，然后访问：

```text
https://你的域名/creator/apply
```

超级管理员默认可以审核。其他管理员需要“创作者申请管理”权限。

GitHub Actions 已配置在 `.github/workflows/ci.yaml`。推送到 `main` 或创建 Pull Request 时会使用 JDK 21、Node.js 24 和 pnpm 9 执行 Halo 官方插件 CI。

## 数据与隐私

0.1.0 将截图以受 RBAC 保护的插件自定义模型字段保存，并限制约 2 MB。该方案部署简单，但会增加 Halo 数据库体积。大量使用前建议迁移到插件私有文件存储，不能把群截图放入公开附件库。

## 下一阶段

- Webhook 设置页（地址、共享密钥、开关、重试）
- 提交、通过和驳回事件签名推送
- AstrBot 插件接收事件并向指定群聊发送消息
- 申请人查看自己的历史和驳回原因
