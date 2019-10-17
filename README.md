# ShadowEdit
## Spark of ShadowEdit?
I am working on one remote Ubuntu 14, you can just access via SSH, because of the toolchain and development environment can not be setup in Windows, meanwhile, I am not skilled VIM or similar command line
IDE user. At first, I use synchronization tool in WinSCP, i.e. I edit source file in my eclipse and after a while I will click "synchronize" button in WinSCP to sychronize files to Ubuntu, then in my SSH client to run bash to CMake it. This is working out but not handy for me. so I want to develop one eclipse plugin to do clicking in behind.

## How ShadowEdit works?
ShadowEdit extended three eclipse plugin-in extension points, it added one metafile "shadowedit.sdexml" in project root, it is one XML file which defines all actions and filters in it. 
then ShadowEdit lauch file watch service to watch your ShadowEdit enabled project, once there are file event it will trigger below commands defined in "shadowedit.sdexml". It delegates the concrete 
command to your native command, such .bat or python script.
* onmoveto: when your file or folder moved to other places, what native command will execute
* onmodify: when your file(only file) content modified, what native command will execute
* onremove: when file( also folder) is removed
* oncreate: when file( not folder) is created
* mkdir: when folder( not file) is created

there are detailed coments in .sdexml template. Please just follow doc to refer arguments

## How to install ShadowEdit?
Just copy "plugin/ShadowEdit_version_timestamp.jar" to your eclipse/plugins, then restart your eclipse

## ShadowEdit的创作背景
我工作在一个远程Ubunt14上，这个服务器只能通过SSH远程连接， 由于工具链和开发环境不能在windows上设置起来，同时，我不习惯用VIM等基于命令行的集成开发环境。开始，用想了个办法，用WinSCP的同步工具做，步骤如下，先在windows的eclipse里面编辑我的文件，然后不定期的去WinSCP点击同步按钮把文件同步到服务器，然后去我的SSH里面敲CMake等命令。这个方法不顺手但是解决了我的问题。所以，我想开发一个插件来解决这些点击动作，让工作自动化起来。

## ShadowEdit的工作原理
ShadowEdit扩展了三个插件，在工程的根目录增加了一个元文件"shadowedit.sdexml"，里面包含文件过滤器规则和文件同步动作。ShadowEdit启动了文件监视服务，监视工作区下启动了ShadowEdit的项目，一旦文件事件被触发，"shadowedit.sdexml"对应的命令
会被执行，ShadowEdit把具体的执行委派给本地脚本，比如，你可以编写一个bat或python脚本来具体执行动作，
* onmoveto: 当文件或文件夹被移动时，执行动作
* onmodify:当文件（特指文件）内容被修改时执行
* onremove: 当文件或文件夹被删除时执行
* oncreate: 当文件（特指文件）被创建时执行
* mkdir: 当文件夹被创建时执行

命令行字符串可以使用{0}这样的变量替换，在生成的.sdexml模板里面有详细的参数描述，请参考编写你的参数。

## 如何安装ShadowEdit？
只需把"plugin/ShadowEdit_version_timestamp.jar"复制到你的eclipse/plugin下面，重启eclipse即可

![ShadowEdit](https://raw.githubusercontent.com/alexmao86/ShadowEdit/master/snapshot.jpg "ShadowEdit")
