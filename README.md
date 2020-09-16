# chatroom
A simple chat with command line\
一個簡單的命令介面聊天室
![image](https://raw.githubusercontent.com/coreyhao0313/chatroom/master/example.png)

A keyboard remote as bluetooth keyboard with PC or NB in LAN network\
一個可作為藍牙鍵盤的遠端鍵盤，適合在區域網路下的 PC 或 NB
![image](https://raw.githubusercontent.com/coreyhao0313/chatroom/master/example_remote.png)


Usage on Win
在 Windows 下
```
javac -encoding UTF-8 <Main FileName>
```
```
java -Dfile.encoding=UTF-8 <Main ClassName> <Method [-s, -c]> <HOST> <PORT>
```
```
java -Dfile.encoding=UTF-8 <Main ClassName> <Method [-s, -c]> <PORT>
```

Example
範例
```
java -Dfile.encoding=UTF-8 Online -s 3000
```
```
java -Dfile.encoding=UTF-8 Online -c 127.0.0.1 3000
```