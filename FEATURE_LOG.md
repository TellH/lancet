### 配置
- 在build.gradle添加闭包:
```
lancet {
    strictMode true // true直接抛异常中断构建，false在根目录下的lancet_extra里输出error log
    checkMethodNotFoundEnable true // 检查无用方法检查的开关
}
```
```
    dependencies {
        provided 'com.tellh.me.ele:lancet-base:1.0.2-alpha2'
    }
    dependencies {
        classpath 'com.tellh.me.ele:lancet-plugin:1.0.2-alpha2'
    }
```
- 在主工程中创建目录 lancet_extra，在目录内存放android_sdk.json和white_list.json。android_sdk.json内容是sdk中的所有类的信息，用于插件检查时构建一个完整的Graph。
white_list.json配置一个json数组，数组元素是需要检查过滤的类的正则表达式。