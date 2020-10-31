# chatroom

A simple chat with command line\
一個簡單的命令介面聊天室
![image](https://raw.githubusercontent.com/coreyhao0313/chatroom/master/example.png)

A keyboard remote as bluetooth keyboard with PC or NB in LAN network\
一個可作為藍牙鍵盤的遠端鍵盤，適合在區域網路下的 PC 或 NB
![image](https://raw.githubusercontent.com/coreyhao0313/chatroom/master/example_remote.png)

---

## Usage on Win
## 在 Windows 下

```
javac -encoding UTF-8 <Main FileName>
```

```
java -Dfile.encoding=UTF-8 <Main ClassName> <Method [-s, -c]> <HOST> <PORT>
```

```
java -Dfile.encoding=UTF-8 <Main ClassName> <Method [-s, -c]> <PORT>
```

---

## Start server/client Example
## 啟動範例

```
java -Dfile.encoding=UTF-8 Online -s 3000
```

```
java -Dfile.encoding=UTF-8 Online -c 127.0.0.1 3000
```

---

#### 啟動後，於 Client 中直接輸入自訂 (Key)
##### 其他 Client 所輸入的 (Key) 若對應到伺服器中存在之 (Key) ，則成立一個群組 (即聊天室 或 共享遠端、檔案)
```
自訂 (Key) 後，輸入純文字按下 Enter 則發出聊天訊息
```

#### 特殊指令

###### 1. 遠端

```
/remote
```
作為被控端

```
/remote me
```

###### 2. 檔案傳輸

```
/file <File Path>
```
為聊天室中所有成員將接收檔案\
＊必須有 ./files 目錄
