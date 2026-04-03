# ChatRoom API 文件（繁體中文）

本文件整理目前專案可用的 API，包含：
- REST API 路由
- WebSocket / STOMP 路由
- 主要回傳內容與欄位說明

## 1. REST API

### 1.1 使用者 UserController（/user）

#### POST /user/register
- 說明：註冊新使用者
- Request Body：
```json
{
  "username": "alice",
  "displayName": "Alice",
  "password": "123456"
}
```
- 回傳：`boolean`
- 回傳範例：
```json
true
```

#### POST /user/login
- 說明：登入
- Request Body：
```json
{
  "username": "alice",
  "password": "123456"
}
```
- 回傳：`ResponseEntity<Map<String, String>>`
- 成功回傳範例：
```json
{
  "message": "login success"
}
```

#### POST /user/logout
- 說明：登出（清除 Session）
- 回傳：`ResponseEntity<Map<String, String>>`
- 成功回傳範例：
```json
{
  "message": "logout success"
}
```

#### GET /user/me
- 說明：取得目前登入者資訊
- 回傳：`UserInfoDTO`
- 回傳欄位：
  - `id`: 使用者 ID
  - `username`: 帳號
  - `displayName`: 顯示名稱
  - `createdAt`: 建立日期（`LocalDate`）
- 回傳範例：
```json
{
  "id": 1,
  "username": "alice",
  "displayName": "Alice",
  "createdAt": "2026-04-02"
}
```

#### GET /user/info?username={username}
- 說明：依帳號查詢使用者資訊
- 回傳：`UserInfoDTO`
- 回傳格式同 `/user/me`

#### PUT /user/changeDisplayname
- 說明：修改目前登入者顯示名稱
- Request Body（至少要有 displayName）：
```json
{
  "displayName": "Alice New"
}
```
- 回傳：`boolean`
- 回傳範例：
```json
true
```

### 1.2 房間 RoomController（/rooms）

#### POST /rooms
- 說明：建立聊天室
- Request Body：
```json
{
  "name": "General"
}
```
- 回傳：`ResponseEntity<Map<String, String>>`
- 成功回傳範例（HTTP 201）：
```json
{
  "message": "Room created"
}
```

#### GET /rooms/{roomId}
- 說明：取得房間資訊（房間名稱 + 成員清單）
- 回傳：`RoomInfoDTO`
- 回傳欄位：
  - `id`: 房間 ID
  - `name`: 房間名稱
  - `members`: 成員陣列（`RoomMemberDTO[]`）
    - `username`: 帳號
    - `displayName`: 顯示名稱
    - `role`: 角色（例如 `MEMBER`）
    - `joinedAt`: 加入日期（`LocalDate`）
- 回傳範例：
```json
{
  "id": 1,
  "name": "General",
  "members": [
    {
      "username": "alice",
      "displayName": "Alice",
      "role": "MEMBER",
      "joinedAt": "2026-04-02"
    }
  ]
}
```

#### POST /rooms/{roomId}/members/join
- 說明：加入聊天室
- 回傳：`ResponseEntity<Map<String, String>>`
- 成功回傳範例：
```json
{
  "message": "Joined room"
}
```

#### DELETE /rooms/{roomId}/members/leave
- 說明：離開聊天室
- 回傳：`ResponseEntity<Map<String, String>>`
- 成功回傳範例：
```json
{
  "message": "Left room"
}
```
- 失敗回傳範例（HTTP 400）：
```json
{
  "message": "Not a member of this room"
}
```

### 1.3 訊息歷史 MessageHistoryController（/messages）

#### GET /messages/rooms/{roomId}
- 說明：取得指定聊天室的聊天歷史
- 回傳：`ChatMessage[]`
- `ChatMessage` 欄位：
  - `roomId`: 房間 ID
  - `content`: 訊息內容
  - `type`: 訊息類型（例如 `CHAT`）
  - `sender`: 發送者
  - `createdAt`: 發送時間（`LocalDateTime`）
- 回傳範例：
```json
[
  {
    "roomId": 1,
    "content": "Hello",
    "type": "CHAT",
    "sender": "alice",
    "createdAt": "2026-04-02T10:30:00"
  }
]
```

## 2. WebSocket / STOMP API

由 `WebSocketConfig` 設定：
- STOMP 連線端點：`/ws`（SockJS）
- App 前綴：`/app`
- Broker 前綴：`/topic`

### 2.1 Client Send

#### SEND /app/chat.send
- 說明：送出聊天訊息
- Payload：`ChatMessage`
```json
{
  "roomId": 1,
  "content": "Hello everyone",
  "type": "CHAT",
  "createdAt": "2026-04-02T10:35:00"
}
```

### 2.2 Client Subscribe

#### SUBSCRIBE /topic/rooms/{roomId}
- 說明：接收房間聊天訊息推播
- 推播內容：`ChatMessage`

#### SUBSCRIBE /topic/rooms/{roomId}/members
- 說明：接收成員異動事件推播（加入/離開）
- 推播內容範例：
```json
{
  "event": "JOIN",
  "username": "alice",
  "roomId": 1
}
```
或
```json
{
  "event": "LEAVE",
  "username": "alice",
  "roomId": 1
}
```

## 3. 備註

- 多數 API 需要先登入（Session/Cookie）。
- 發生例外（例如房間不存在）時，實際錯誤格式可能由全域例外處理或 Spring 預設錯誤回應決定。
