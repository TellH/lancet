# Lancet

## 原作者和说明

[eleme lancet github 地址 https://github.com/eleme/lancet](https://github.com/eleme/lancet)   

[Chinese README](README_zh.md)   
[English README](README_en.md)


## 构建打包发布说明

### 九年义务教育使我优秀

```shell

// sample-test 纯java 工程，直接run即可，实时编译plugin ，通过 test 触发构建

// 构建并打包推送到本地仓库 testapp 的测试需要先执行这个构建
./shell/uploadToLocal

// 推送到头条 nexus 仓库   ，修改gradle.properties版本号后，执行脚本即可

./shell/uploadToTTNexus

```
 