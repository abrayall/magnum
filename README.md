<img src="https://s-media-cache-ak0.pinimg.com/236x/4b/a4/58/4ba4585562cccc9ef6af381b8b8bb43b.jpg" width="150px" />

# Magnum - Streaming Build / CI Tool
Magnum is a streaming build tool that can watch a set of source files and execute a command on any change.  It is super lightweight and fast to install and run.  Use it to continously run your maven, ant, shell script based builds, or really any other command!

### Install
 -  Download the magnum-<version>.tar.gz file and extract on to your system
<br>

### Run
 -  From your project home directory run [location]/bin/magnum.sh [command]
<br>

### Examples
#### Maven
```sh
 $/opt/magnum/bin/magnum.sh mvn package
```

#### Ant (default target)
```sh
 $/opt/magnum/bin/magnum.sh ant
```

#### Ant (alternative target)
```sh
 $/opt/magnum/bin/magnum.sh ant compile
```

#### Shell
```sh
 $/opt/magnum/bin/magnum.sh ls
```
