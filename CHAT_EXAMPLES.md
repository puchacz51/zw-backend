# System Głównego Czatu - Dodatkowe Przykłady

## Hook React do zarządzania czatem

### useChat Hook

```javascript
// hooks/useChat.js
import { useState, useEffect, useCallback, useRef } from 'react';
import ChatService from '../services/ChatService';

const useChat = (token, user) => {
  const [messages, setMessages] = useState([]);
  const [connected, setConnected] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  
  const messagesRef = useRef(messages);
  messagesRef.current = messages;

  // Pobieranie wiadomości z API
  const fetchMessages = useCallback(async (pageNum = 0, append = false) => {
    if (!token) return;
    
    setLoading(true);
    setError(null);
    
    try {
      const response = await fetch(
        `http://localhost:8080/api/chat/messages?page=${pageNum}&size=20`,
        {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        }
      );
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      const data = await response.json();
      
      if (append) {
        setMessages(prev => [...data.content.reverse(), ...prev]);
      } else {
        setMessages(data.content.reverse());
      }
      
      setHasMore(!data.last);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
      
    } catch (err) {
      console.error('Error fetching messages:', err);
      setError('Nie udało się pobrać wiadomości');
    } finally {
      setLoading(false);
    }
  }, [token]);

  // Wysyłanie wiadomości przez REST API
  const sendMessageRest = useCallback(async (content) => {
    if (!token || !content.trim()) return null;
    
    try {
      const response = await fetch('http://localhost:8080/api/chat/send', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ content })
      });
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      return await response.json();
    } catch (err) {
      console.error('Error sending message:', err);
      setError('Nie udało się wysłać wiadomości');
      return null;
    }
  }, [token]);

  // Wysyłanie wiadomości przez WebSocket
  const sendMessage = useCallback((content) => {
    if (connected && content.trim()) {
      ChatService.sendMessage(content);
      return true;
    }
    return false;
  }, [connected]);

  // Ładowanie kolejnych wiadomości
  const loadMoreMessages = useCallback(() => {
    if (hasMore && !loading) {
      const nextPage = page + 1;
      setPage(nextPage);
      fetchMessages(nextPage, true);
    }
  }, [hasMore, loading, page, fetchMessages]);

  // Obsługa wiadomości z WebSocket
  const handleWebSocketMessage = useCallback((message) => {
    if (message.type === 'CHAT') {
      const newMessage = {
        id: Date.now(), // Tymczasowe ID
        content: message.content,
        timestamp: message.timestamp,
        sender: {
          email: message.senderEmail,
          firstName: message.senderName?.split(' ')[0] || '',
          lastName: message.senderName?.split(' ')[1] || ''
        }
      };
      
      setMessages(prev => {
        // Sprawdź czy wiadomość już nie istnieje
        const exists = prev.some(msg => 
          msg.content === newMessage.content && 
          msg.sender.email === newMessage.sender.email &&
          Math.abs(new Date(msg.timestamp) - new Date(newMessage.timestamp)) < 1000
        );
        
        if (!exists) {
          return [...prev, newMessage];
        }
        return prev;
      });
    } else if (message.type === 'JOIN') {
      console.log('User joined:', message.senderName);
    } else if (message.type === 'ERROR') {
      setError(message.content);
    }
  }, []);

  // Inicjalizacja połączenia WebSocket
  useEffect(() => {
    if (!token || !user) return;

    // Pobierz początkowe wiadomości
    fetchMessages();

    // Połącz z WebSocket
    ChatService.connect(
      token,
      handleWebSocketMessage,
      () => {
        setConnected(true);
        setError(null);
        // Powiadom o dołączeniu
        if (user.firstName && user.lastName) {
          ChatService.addUser(`${user.firstName} ${user.lastName}`);
        }
      },
      (error) => {
        console.error('WebSocket error:', error);
        setConnected(false);
        setError('Połączenie z czatem zostało przerwane');
      }
    );

    return () => {
      ChatService.disconnect();
      setConnected(false);
    };
  }, [token, user, fetchMessages, handleWebSocketMessage]);

  // Reset przy zmianie użytkownika
  useEffect(() => {
    setMessages([]);
    setPage(0);
    setHasMore(true);
    setError(null);
  }, [user?.id]);

  return {
    messages,
    connected,
    loading,
    error,
    hasMore,
    totalPages,
    totalElements,
    sendMessage,
    sendMessageRest,
    loadMoreMessages,
    refetchMessages: () => fetchMessages(0, false)
  };
};

export default useChat;
```

## Ulepszona wersja komponentu Chat

```jsx
// components/Chat.jsx
import React, { useState, useRef, useEffect } from 'react';
import useChat from '../hooks/useChat';

const Chat = ({ token, user, className = '' }) => {
  const [newMessage, setNewMessage] = useState('');
  const [isAtBottom, setIsAtBottom] = useState(true);
  const messagesEndRef = useRef(null);
  const messagesContainerRef = useRef(null);

  const {
    messages,
    connected,
    loading,
    error,
    hasMore,
    totalElements,
    sendMessage,
    loadMoreMessages
  } = useChat(token, user);

  // Sprawdź czy użytkownik jest na dole czatu
  const handleScroll = () => {
    const container = messagesContainerRef.current;
    if (container) {
      const { scrollTop, scrollHeight, clientHeight } = container;
      const atBottom = scrollHeight - scrollTop - clientHeight < 100;
      setIsAtBottom(atBottom);
    }
  };

  // Przewijanie do dołu
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  // Auto-scroll tylko gdy użytkownik jest na dole
  useEffect(() => {
    if (isAtBottom) {
      scrollToBottom();
    }
  }, [messages, isAtBottom]);

  const handleSendMessage = () => {
    if (newMessage.trim() && connected) {
      const success = sendMessage(newMessage);
      if (success) {
        setNewMessage('');
      }
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  const formatTimestamp = (timestamp) => {
    const date = new Date(timestamp);
    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const messageDate = new Date(date.getFullYear(), date.getMonth(), date.getDate());
    
    if (messageDate.getTime() === today.getTime()) {
      // Dzisiaj - pokaż tylko godzinę
      return date.toLocaleTimeString('pl-PL', {
        hour: '2-digit',
        minute: '2-digit'
      });
    } else {
      // Inny dzień - pokaż datę i godzinę
      return date.toLocaleString('pl-PL', {
        day: '2-digit',
        month: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    }
  };

  const isMyMessage = (message) => {
    return message.sender.email === user?.email;
  };

  return (
    <div className={`chat-container ${className}`}>
      {/* Header */}
      <div className="chat-header">
        <div className="chat-title">
          <h3>Główny Czat</h3>
          <span className="message-count">
            {totalElements > 0 && `${totalElements} wiadomości`}
          </span>
        </div>
        <div className={`connection-status ${connected ? 'connected' : 'disconnected'}`}>
          <div className="status-indicator"></div>
          {connected ? 'Połączono' : 'Rozłączono'}
        </div>
      </div>

      {/* Error */}
      {error && (
        <div className="chat-error">
          <span>❌ {error}</span>
          <button onClick={() => window.location.reload()}>
            Odśwież
          </button>
        </div>
      )}

      {/* Messages */}
      <div 
        className="chat-messages"
        ref={messagesContainerRef}
        onScroll={handleScroll}
      >
        {hasMore && (
          <button 
            onClick={loadMoreMessages} 
            disabled={loading}
            className="load-more-btn"
          >
            {loading ? (
              <>
                <div className="spinner"></div>
                Ładowanie...
              </>
            ) : (
              'Załaduj starsze wiadomości'
            )}
          </button>
        )}

        {messages.map((message, index) => {
          const isMine = isMyMessage(message);
          const showAvatar = index === 0 || 
            messages[index - 1].sender.email !== message.sender.email;
          
          return (
            <div 
              key={message.id || index} 
              className={`message ${isMine ? 'message-mine' : 'message-other'}`}
            >
              {!isMine && showAvatar && (
                <div className="message-avatar">
                  {message.sender.avatarUrl ? (
                    <img 
                      src={message.sender.avatarUrl} 
                      alt={`${message.sender.firstName} ${message.sender.lastName}`}
                    />
                  ) : (
                    <div className="avatar-placeholder">
                      {message.sender.firstName?.[0]?.toUpperCase()}
                      {message.sender.lastName?.[0]?.toUpperCase()}
                    </div>
                  )}
                </div>
              )}
              
              <div className="message-content">
                {!isMine && showAvatar && (
                  <div className="message-sender">
                    {message.sender.firstName} {message.sender.lastName}
                  </div>
                )}
                
                <div className="message-bubble">
                  <div className="message-text">
                    {message.content}
                  </div>
                  <div className="message-time">
                    {formatTimestamp(message.timestamp)}
                  </div>
                </div>
              </div>
            </div>
          );
        })}
        
        <div ref={messagesEndRef} />
        
        {/* Scroll to bottom button */}
        {!isAtBottom && (
          <button 
            className="scroll-to-bottom-btn"
            onClick={scrollToBottom}
            title="Przewiń do dołu"
          >
            ⬇️
          </button>
        )}
      </div>

      {/* Input */}
      <div className="chat-input">
        <div className="input-container">
          <textarea
            value={newMessage}
            onChange={(e) => setNewMessage(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder={connected ? "Wpisz wiadomość..." : "Łączenie z czatem..."}
            disabled={!connected}
            rows={1}
            maxLength={1000}
          />
          <button 
            onClick={handleSendMessage}
            disabled={!connected || !newMessage.trim()}
            className="send-button"
          >
            📤
          </button>
        </div>
        <div className="input-info">
          <span className="char-count">
            {newMessage.length}/1000
          </span>
          <span className="tip">
            Enter = wyślij, Shift+Enter = nowa linia
          </span>
        </div>
      </div>
    </div>
  );
};

export default Chat;
```

## Zaawansowane style CSS

```css
/* styles/Chat.css */
.chat-container {
  display: flex;
  flex-direction: column;
  height: 600px;
  max-width: 800px;
  margin: 0 auto;
  border: 1px solid #e1e5e9;
  border-radius: 12px;
  overflow: hidden;
  background: #ffffff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

/* Header */
.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-bottom: 1px solid #e1e5e9;
}

.chat-title h3 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
}

.message-count {
  font-size: 0.875rem;
  opacity: 0.8;
  margin-top: 0.25rem;
}

.connection-status {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  padding: 0.5rem 1rem;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.2);
}

.status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #28a745;
  animation: pulse 2s infinite;
}

.connection-status.disconnected .status-indicator {
  background: #dc3545;
}

@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.5; }
  100% { opacity: 1; }
}

/* Error */
.chat-error {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1.5rem;
  background: #f8d7da;
  color: #721c24;
  border-bottom: 1px solid #f5c6cb;
}

.chat-error button {
  padding: 0.25rem 0.75rem;
  background: #721c24;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.875rem;
}

/* Messages */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  background: #f8f9fa;
  position: relative;
}

.load-more-btn {
  width: 100%;
  padding: 0.75rem;
  margin-bottom: 1rem;
  background: #6c757d;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  transition: background-color 0.2s;
}

.load-more-btn:hover:not(:disabled) {
  background: #5a6269;
}

.load-more-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid transparent;
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* Message */
.message {
  display: flex;
  margin-bottom: 1rem;
  max-width: 100%;
}

.message-mine {
  justify-content: flex-end;
}

.message-other {
  justify-content: flex-start;
}

.message-avatar {
  width: 40px;
  height: 40px;
  margin-right: 0.75rem;
  flex-shrink: 0;
}

.message-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 0.875rem;
}

.message-content {
  max-width: 70%;
  min-width: 100px;
}

.message-mine .message-content {
  max-width: 70%;
}

.message-sender {
  font-size: 0.875rem;
  font-weight: 600;
  color: #495057;
  margin-bottom: 0.25rem;
}

.message-bubble {
  padding: 0.75rem 1rem;
  border-radius: 18px;
  position: relative;
  word-wrap: break-word;
}

.message-other .message-bubble {
  background: white;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.message-mine .message-bubble {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-bottom-right-radius: 4px;
}

.message-text {
  line-height: 1.4;
  margin-bottom: 0.25rem;
}

.message-time {
  font-size: 0.75rem;
  opacity: 0.7;
  text-align: right;
}

.message-other .message-time {
  color: #6c757d;
}

.scroll-to-bottom-btn {
  position: absolute;
  bottom: 1rem;
  right: 1rem;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #007bff;
  color: white;
  border: none;
  cursor: pointer;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Input */
.chat-input {
  padding: 1rem 1.5rem;
  background: white;
  border-top: 1px solid #e1e5e9;
}

.input-container {
  display: flex;
  gap: 0.75rem;
  align-items: flex-end;
}

.input-container textarea {
  flex: 1;
  min-height: 40px;
  max-height: 120px;
  padding: 0.75rem;
  border: 1px solid #ced4da;
  border-radius: 20px;
  resize: none;
  font-family: inherit;
  font-size: 0.875rem;
  line-height: 1.4;
  transition: border-color 0.2s;
}

.input-container textarea:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.25);
}

.input-container textarea:disabled {
  background: #f8f9fa;
  color: #6c757d;
}

.send-button {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: #007bff;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s;
}

.send-button:hover:not(:disabled) {
  background: #0056b3;
}

.send-button:disabled {
  background: #6c757d;
  cursor: not-allowed;
}

.input-info {
  display: flex;
  justify-content: space-between;
  margin-top: 0.5rem;
  font-size: 0.75rem;
  color: #6c757d;
}

.char-count {
  font-weight: 500;
}

/* Responsive */
@media (max-width: 768px) {
  .chat-container {
    height: 100vh;
    border-radius: 0;
    max-width: none;
  }
  
  .message-content {
    max-width: 85%;
  }
  
  .message-mine .message-content {
    max-width: 85%;
  }
  
  .chat-header {
    padding: 1rem;
  }
  
  .chat-input {
    padding: 1rem;
  }
}

/* Dark mode support */
@media (prefers-color-scheme: dark) {
  .chat-container {
    background: #2d3748;
    border-color: #4a5568;
  }
  
  .chat-messages {
    background: #1a202c;
  }
  
  .message-other .message-bubble {
    background: #4a5568;
    color: #e2e8f0;
  }
  
  .message-sender {
    color: #a0aec0;
  }
  
  .chat-input {
    background: #2d3748;
    border-color: #4a5568;
  }
  
  .input-container textarea {
    background: #1a202c;
    border-color: #4a5568;
    color: #e2e8f0;
  }
  
  .input-info {
    color: #a0aec0;
  }
}
```

## Przykład użycia w aplikacji

```jsx
// App.jsx
import React from 'react';
import Chat from './components/Chat';
import useAuth from './hooks/useAuth'; // Załóżmy że masz hook do auth

function App() {
  const { user, token, isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return <div>Proszę się zalogować</div>;
  }

  return (
    <div className="App">
      <header>
        <h1>Moja Aplikacja</h1>
      </header>
      
      <main>
        <Chat 
          token={token} 
          user={user}
          className="main-chat"
        />
      </main>
    </div>
  );
}

export default App;
```
