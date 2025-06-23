# System Czatu - Instrukcje Integracji

## Opis systemu

System czatu umożliwia:
- Wysyłanie wiadomości w czasie rzeczywistym poprzez WebSocket
- Pobieranie historii wiadomości z paginacją
- Uwierzytelnianie użytkowników przez JWT token
- Zapisywanie wiadomości w bazie danych

## Endpointy REST API

### 1. Pobieranie wiadomości (GET)
```
GET /api/chat/messages?page=0&size=20
Authorization: Bearer {jwt_token}
```

**Parametry:**
- `page` - numer strony (domyślnie 0)
- `size` - ilość wiadomości na stronę (domyślnie 20)

**Odpowiedź:**
```json
{
  "content": [
    {
      "id": 1,
      "content": "Treść wiadomości",
      "timestamp": "2025-06-23T10:30:00",
      "sender": {
        "id": 1,
        "firstName": "Jan",
        "lastName": "Kowalski",
        "email": "jan@example.com",
        "avatarUrl": "http://localhost:8080/api/users/avatar/filename.jpg"
      }
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 100,
  "totalPages": 5,
  "first": true,
  "last": false
}
```

### 2. Wysyłanie wiadomości (POST)
```
POST /api/chat/send
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "content": "Treść wiadomości"
}
```

**Odpowiedź:**
```json
{
  "success": true,
  "message": "Message sent successfully",
  "data": {
    "id": 1,
    "content": "Treść wiadomości",
    "timestamp": "2025-06-23T10:30:00",
    "sender": {
      "id": 1,
      "firstName": "Jan",
      "lastName": "Kowalski",
      "email": "jan@example.com",
      "avatarUrl": "http://localhost:8080/api/users/avatar/filename.jpg"
    }
  }
}
```

## Połączenie WebSocket z React

### 1. Instalacja zależności

```bash
npm install sockjs-client stompjs
# lub
npm install @stomp/stompjs sockjs-client
```

### 2. Konfiguracja WebSocket

```javascript
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

class ChatService {
  constructor() {
    this.stompClient = null;
    this.isConnected = false;
  }

  connect(token, onMessageReceived, onConnected, onError) {
    const socket = new SockJS('http://localhost:8080/ws');
    this.stompClient = Stomp.over(socket);
    
    // Konfiguracja nagłówków z tokenem JWT
    const headers = {
      'Authorization': `Bearer ${token}`
    };

    this.stompClient.connect(
      headers,
      (frame) => {
        console.log('Connected: ' + frame);
        this.isConnected = true;
        
        // Subskrypcja na kanał publiczny
        this.stompClient.subscribe('/topic/public', (message) => {
          const chatMessage = JSON.parse(message.body);
          onMessageReceived(chatMessage);
        });
        
        if (onConnected) onConnected();
      },
      (error) => {
        console.error('Connection error: ', error);
        this.isConnected = false;
        if (onError) onError(error);
      }
    );
  }

  sendMessage(messageContent) {
    if (this.stompClient && this.isConnected) {
      const chatMessage = {
        content: messageContent,
        type: 'CHAT'
      };
      
      this.stompClient.send('/app/chat.sendMessage', {}, JSON.stringify(chatMessage));
    }
  }

  addUser(userName) {
    if (this.stompClient && this.isConnected) {
      const chatMessage = {
        senderName: userName,
        type: 'JOIN'
      };
      
      this.stompClient.send('/app/chat.addUser', {}, JSON.stringify(chatMessage));
    }
  }

  disconnect() {
    if (this.stompClient) {
      this.stompClient.disconnect();
      this.isConnected = false;
    }
  }
}

export default new ChatService();
```

### 3. Implementacja komponentu React

```jsx
import React, { useState, useEffect, useRef } from 'react';
import ChatService from './ChatService';

const Chat = ({ user, token }) => {
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [connected, setConnected] = useState(false);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const messagesEndRef = useRef(null);

  // Pobieranie historii wiadomości
  const fetchMessages = async (pageNum = 0, append = false) => {
    try {
      setLoading(true);
      const response = await fetch(
        `http://localhost:8080/api/chat/messages?page=${pageNum}&size=20`,
        {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        }
      );
      
      const data = await response.json();
      
      if (append) {
        setMessages(prev => [...data.content.reverse(), ...prev]);
      } else {
        setMessages(data.content.reverse());
      }
      
      setHasMore(!data.last);
      setLoading(false);
    } catch (error) {
      console.error('Error fetching messages:', error);
      setLoading(false);
    }
  };

  // Połączenie z WebSocket
  useEffect(() => {
    const onMessageReceived = (message) => {
      if (message.type === 'CHAT') {
        setMessages(prev => [...prev, {
          id: Date.now(), // tymczasowe ID
          content: message.content,
          timestamp: message.timestamp,
          sender: {
            email: message.senderEmail,
            firstName: message.senderName.split(' ')[0],
            lastName: message.senderName.split(' ')[1] || '',
          }
        }]);
      } else if (message.type === 'JOIN') {
        // Obsługa wiadomości o dołączeniu użytkownika
        console.log(message.content);
      } else if (message.type === 'ERROR') {
        console.error('Chat error:', message.content);
      }
    };

    const onConnected = () => {
      setConnected(true);
      ChatService.addUser(`${user.firstName} ${user.lastName}`);
    };

    const onError = (error) => {
      setConnected(false);
      console.error('WebSocket connection error:', error);
    };

    ChatService.connect(token, onMessageReceived, onConnected, onError);
    fetchMessages();

    return () => {
      ChatService.disconnect();
    };
  }, [token, user]);

  // Automatyczne przewijanie do najnowszej wiadomości
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Wysyłanie wiadomości
  const sendMessage = (e) => {
    e.preventDefault();
    if (newMessage.trim() && connected) {
      ChatService.sendMessage(newMessage);
      setNewMessage('');
    }
  };

  // Ładowanie starszych wiadomości
  const loadMoreMessages = () => {
    if (!loading && hasMore) {
      const nextPage = page + 1;
      setPage(nextPage);
      fetchMessages(nextPage, true);
    }
  };

  return (
    <div className="chat-container">
      <div className="chat-header">
        <h3>Chat globalny</h3>
        <div className={`connection-status ${connected ? 'connected' : 'disconnected'}`}>
          {connected ? '🟢 Połączono' : '🔴 Rozłączono'}
        </div>
      </div>

      <div className="messages-container">
        {hasMore && (
          <button onClick={loadMoreMessages} disabled={loading}>
            {loading ? 'Ładowanie...' : 'Załaduj starsze wiadomości'}
          </button>
        )}
        
        {messages.map((message, index) => (
          <div key={message.id || index} className="message">
            <div className="message-header">
              <span className="sender-name">
                {message.sender.firstName} {message.sender.lastName}
              </span>
              <span className="timestamp">
                {new Date(message.timestamp).toLocaleString()}
              </span>
            </div>
            <div className="message-content">{message.content}</div>
          </div>
        ))}
        <div ref={messagesEndRef} />
      </div>

      <form onSubmit={sendMessage} className="message-form">
        <input
          type="text"
          value={newMessage}
          onChange={(e) => setNewMessage(e.target.value)}
          placeholder="Napisz wiadomość..."
          maxLength={1000}
          disabled={!connected}
        />
        <button type="submit" disabled={!connected || !newMessage.trim()}>
          Wyślij
        </button>
      </form>
    </div>
  );
};

export default Chat;
```

### 4. Stylowanie CSS (opcjonalne)

```css
.chat-container {
  display: flex;
  flex-direction: column;
  height: 500px;
  border: 1px solid #ddd;
  border-radius: 8px;
  overflow: hidden;
}

.chat-header {
  background: #f5f5f5;
  padding: 10px;
  border-bottom: 1px solid #ddd;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.connection-status.connected {
  color: green;
}

.connection-status.disconnected {
  color: red;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.message {
  margin-bottom: 15px;
  padding: 8px;
  border-radius: 4px;
  background: #f9f9f9;
}

.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
  font-size: 0.9em;
  color: #666;
}

.sender-name {
  font-weight: bold;
}

.timestamp {
  font-size: 0.8em;
}

.message-content {
  font-size: 1em;
  line-height: 1.4;
}

.message-form {
  display: flex;
  padding: 10px;
  border-top: 1px solid #ddd;
  background: #f5f5f5;
}

.message-form input {
  flex: 1;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  margin-right: 10px;
}

.message-form button {
  padding: 8px 16px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.message-form button:disabled {
  background: #ccc;
  cursor: not-allowed;
}
```

## Typy wiadomości WebSocket

### Wysyłane do serwera:

1. **Wysyłanie wiadomości:**
```json
{
  "content": "Treść wiadomości",
  "type": "CHAT"
}
```

2. **Dołączenie do czatu:**
```json
{
  "senderName": "Jan Kowalski",
  "type": "JOIN"
}
```

### Otrzymywane z serwera:

1. **Wiadomość czatu:**
```json
{
  "type": "CHAT",
  "content": "Treść wiadomości",
  "senderEmail": "jan@example.com",
  "senderName": "Jan Kowalski",
  "timestamp": "2025-06-23T10:30:00"
}
```

2. **Powiadomienie o dołączeniu:**
```json
{
  "type": "JOIN",
  "content": "Jan Kowalski joined the chat!",
  "senderEmail": "jan@example.com",
  "senderName": "Jan Kowalski",
  "timestamp": "2025-06-23T10:30:00"
}
```

3. **Wiadomość błędu:**
```json
{
  "type": "ERROR",
  "content": "User not authenticated",
  "timestamp": "2025-06-23T10:30:00"
}
```

## Uwagi dotyczące bezpieczeństwa

1. **Uwierzytelnianie:** Token JWT musi być przekazany w nagłówku `Authorization` przy połączeniu WebSocket
2. **Walidacja:** Wszystkie wiadomości są walidowane po stronie serwera
3. **Długość wiadomości:** Maksymalna długość wiadomości to 1000 znaków
4. **CORS:** Skonfigurowane dla localhost:3000 i localhost:5173

## Testowanie

Możesz przetestować system używając:
1. REST API do pobierania historii wiadomości
2. WebSocket do wysyłania wiadomości w czasie rzeczywistym
3. Uwierzytelnianie poprzez /api/auth/login
