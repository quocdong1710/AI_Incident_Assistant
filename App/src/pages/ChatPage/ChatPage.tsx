// ===== Chat Page (chatting) =====
import { useState, useRef, useEffect, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Sidebar from '../../components/Sidebar/Sidebar';
import { useAuth } from '../../context/AuthContext';
import {
  getAppById,
  getChatParticipantsForService,
  getChatMessagesForClient,
  getClientListForService,
  getUserRoleInService,
  incidentXBot,
} from '../../data/mockData';
import type { ChatMessage, ChatParticipant } from '../../types';
import styles from './ChatPage.module.css';

export default function ChatPage() {
  const { serviceId, appId } = useParams<{ serviceId: string; appId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const result = getAppById(serviceId || '', appId || '');
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // User's role in this service
  const userRole = getUserRoleInService(user?.id || '', serviceId || '');
  const isStaffOrAdmin = userRole === 'staff' || userRole === 'admin';

  // All clients available for this service (for switching)
  const allClients = useMemo(
    () => getClientListForService(serviceId || ''),
    [serviceId]
  );

  // Active client being chatted with (only relevant for staff/admin)
  const [activeClientId, setActiveClientId] = useState<string | null>(
    allClients.length > 0 ? allClients[0].id : null
  );

  // Build participants from real data
  const participants = useMemo(
    () => getChatParticipantsForService(serviceId || '', activeClientId || undefined),
    [serviceId, activeClientId]
  );

  const allParticipants = useMemo(() => {
    return [...participants.clients, ...participants.staff, participants.bot];
  }, [participants]);

  // Messages for the current client conversation
  const initialMessages = useMemo(
    () => getChatMessagesForClient(serviceId || '', appId || '', activeClientId || ''),
    [serviceId, appId, activeClientId]
  );

  const [messages, setMessages] = useState<ChatMessage[]>(initialMessages);
  const [input, setInput] = useState('');

  // Sidebar & Resizing State
  const [showIncidentDetails, setShowIncidentDetails] = useState(false);
  const [incidentsData, setIncidentsData] = useState<any[]>([]);
  const [isLoadingIncidents, setIsLoadingIncidents] = useState(false);
  const [sidebarWidth, setSidebarWidth] = useState(320);
  const [isResizing, setIsResizing] = useState(false);

  // Incident Detail State
  const [selectedIncident, setSelectedIncident] = useState<any | null>(null);
  const [isLoadingDetail, setIsLoadingDetail] = useState(false);

  // Reset messages when switching client
  useEffect(() => {
    setMessages(initialMessages);
  }, [initialMessages]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Sidebar Resizing Logic
  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => {
      if (!isResizing) return;
      // Calculate new width from right edge
      const newWidth = window.innerWidth - e.clientX;
      if (newWidth > 280 && newWidth < 800) {
        setSidebarWidth(newWidth);
      }
    };
    const handleMouseUp = () => setIsResizing(false);

    if (isResizing) {
      window.addEventListener('mousemove', handleMouseMove);
      window.addEventListener('mouseup', handleMouseUp);
    }
    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };
  }, [isResizing]);

  const handleIncidentClick = async (key: string) => {
    setIsLoadingDetail(true);
    const API_URL = (import.meta.env.VITE_API_URL as string) || 'http://localhost:8080/api';
    try {
      const res = await fetch(`${API_URL}/incidents/${key}`);
      if (res.ok) {
        const data = await res.json();
        setSelectedIncident(data);
      }
    } catch (err) {
      console.error('Fetch incident detail error:', err);
    } finally {
      setIsLoadingDetail(false);
    }
  };

  if (!result) {
    return (
      <div className={styles.page}>
        <Sidebar />
        <main className={styles.main}>
          <p style={{ padding: 48 }}>App not found.</p>
        </main>
      </div>
    );
  }

  const { service, app } = result;

  // Current user as a participant (for sending messages)
  const currentUserParticipant: ChatParticipant = {
    id: user?.id || 'unknown',
    name: user?.name || 'You',
    role: userRole || 'Staff',
    avatar: user?.avatar || '',
    type: isStaffOrAdmin ? 'staff' : 'customer',
    isOnline: true,
  };

  // Real API for Bot Commands and Triage
  const botApi = async (messageText: string): Promise<{ content: string; suggestions?: any[] } | null> => {
    const API_URL = (import.meta.env.VITE_API_URL as string) || 'http://localhost:8080/api';
    const parts = messageText.trim().split(' ');
    const command = parts[0];

    // Case 1: /status command - Fetch incident list
    if (command === '/status') {
      try {
        const res = await fetch(`${API_URL}/incidents`);
        if (!res.ok) throw new Error('Failed to fetch');
        const incidents: any[] = await res.json();

        const openIncidents = incidents.filter(i => i.status !== 'Resolved' && i.status !== 'Closed');
        if (openIncidents.length === 0) return { content: '📋 Hiện không có incident nào đang mở.' };

        let report = `📋 Incident đang mở (${openIncidents.length}):\n\n`;
        openIncidents.slice(0, 5).forEach(inc => {
          report += `• ${inc.jiraIssueKey} [${inc.severity}] ${inc.title}\n`;
        });
        if (openIncidents.length > 5) report += `\n... và ${openIncidents.length - 5} incident khác.`;
        return { content: report };
      } catch (err) {
        console.error('Fetch status error:', err);
        return { content: '❌ Lỗi khi lấy danh sách incident từ server.' };
      }
    }

    // Case 2: /detail - Everyone (Table View)
    if (command === '/detail') {
      try {
        const res = await fetch(`${API_URL}/incidents`);
        if (!res.ok) throw new Error('Failed to fetch');
        const incidents: any[] = await res.json();

        let table = `📋 **Bảng chi tiết Incident**\n\n`;
        table += `| Key | Title | Status | Severity | Assigned |\n`;
        table += `| :--- | :--- | :--- | :--- | :--- |\n`;

        incidents.forEach(inc => {
          table += `| **${inc.jiraIssueKey}** | ${inc.title} | ${inc.status} | ${inc.severity} | ${inc.assignedTo || '---'} |\n`;
        });

        return { content: table };
      } catch (err) {
        return { content: '❌ Lỗi khi lấy danh sách chi tiết incident.' };
      }
    }

    // Case 3: /severity [issueKey] [level] - Staff or Admin
    if (command === '/severity') {
      if (userRole !== 'staff' && userRole !== 'admin') {
        return { content: '🚫 Bạn không có quyền thay đổi độ ưu tiên của incident.' };
      }
      const issueKey = parts[1];
      const newSeverity = parts[2];
      if (!issueKey || !newSeverity) return { content: '❌ Cú pháp: /severity [issueKey] [P1|P2|P3|P4]' };

      try {
        const res = await fetch(`${API_URL}/commands/severity?issueKey=${issueKey}&severity=${newSeverity}`, {
          method: 'POST'
        });
        if (!res.ok) throw new Error('Failed');
        const inc = await res.json();
        return { content: `✅ Đã cập nhật mức độ nghiêm trọng của ${issueKey} thành **${inc.severity}**.` };
      } catch (err) {
        return { content: `❌ Không thể cập nhật mức độ nghiêm trọng cho ${issueKey}.` };
      }
    }

    // Case 4: /suggest [issueKey] - ONLY Admin
    if (command === '/suggest') {
      if (userRole !== 'admin') {
        return { content: '🚫 Chỉ quản trị viên mới có thể yêu cầu gợi ý nhân sự điều động.' };
      }
      const issueKey = parts[1];
      if (!issueKey) return { content: '❌ Vui lòng cung cấp Issue Key. VD: /suggest SUP-101' };

      try {
        // AUTOMATION: Fetch incident details first to get severity for tagging logic
        let severity = undefined;
        try {
          const incRes = await fetch(`${API_URL}/incidents/${issueKey}`);
          if (incRes.ok) {
            const incData = await incRes.json();
            severity = incData.severity;
          }
        } catch (e) {
          console.error('Fetch incident for severity failed', e);
        }

        const res = await fetch(`${API_URL}/commands/suggest/${issueKey}`);
        if (!res.ok) throw new Error('Failed to fetch suggestions');
        const suggestions: any[] = await res.json();

        return {
          content: `🔍 Team dispatch suggestions for **${issueKey}**:`,
          suggestions,
          severity
        };
      } catch (err) {
        console.error('Fetch suggestions error:', err);
        return { content: `❌ Không thể lấy gợi ý nhân sự cho ${issueKey}.` };
      }
    }

    // Case 5: /sla [period] - ONLY Admin
    if (command === '/sla') {
      if (userRole !== 'admin') {
        return { content: '🚫 Chỉ quản trị viên mới có quyền xem báo cáo SLA.' };
      }
      const period = parts[1] || 'week';
      try {
        const res = await fetch(`${API_URL}/commands/report/sla?period=${period}`);
        if (!res.ok) throw new Error('Failed');
        const report = await res.json();

        return {
          content: `📊 **SLA Report (${report.period})**\n\n` +
            `• Total Incidents: ${report.totalIncidents}\n` +
            `• SLA Breaches: ${report.breachCount}\n` +
            `• Compliance Rate: ${((1 - report.breachCount / (report.totalIncidents || 1)) * 100).toFixed(1)}%\n\n` +
            (report.breaches.length > 0 ? `⚠️ Các trường hợp vi phạm mới nhất:\n` + report.breaches.slice(0, 3).map((b: any) => `- ${b.jiraIssueKey} (Breached at: ${new Date(b.breachAt).toLocaleDateString()})`).join('\n') : `✅ Không có vi phạm SLA nào trong kỳ.`)
        };
      } catch (err) {
        return { content: '❌ Lỗi khi lấy báo cáo SLA.' };
      }
    }

    // Case 6: /help command
    if (command === '/help') {
      const isStaff = userRole === 'staff' || userRole === 'admin';
      const isAdmin = userRole === 'admin';

      let helpContent = `Lệnh hỗ trợ:\n` +
        `• **/status** – Danh sách incident đang mở\n` +
        `• **/bug [mô tả]** – Báo cáo lỗi`;

      if (isStaff) {
        helpContent += `\n• **/severity [key] [level]** – Cập nhật độ nghiêm trọng`;
      }

      if (isAdmin) {
        helpContent += `\n• **/suggest [key]** – Gợi ý nhân sự điều động\n` +
          `• **/sla [period]** – Báo cáo vi phạm SLA`;
      }

      return { content: helpContent };
    }

    // Case 4: /bug or any other message (for AI triage)
    try {
      const payload = {
        messageId: `msg-web-${Date.now()}`,
        platform: 'web_demo',
        groupId: appId || 'general',
        groupName: app?.name || 'General',
        senderId: user?.id || 'unknown',
        senderName: user?.name || 'User',
        text: messageText,
        receivedAt: new Date().toISOString()
      };

      const res = await fetch(`${API_URL}/webhooks/simulate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (!res.ok) throw new Error('Webhook error');
      const result = await res.json();

      // Only return message if it's a bug, uncertain, or it was an explicit command
      if (result.status === 'created') {
        // AUTOMATION: Fetch suggestions immediately for new bugs
        let suggestions = undefined;
        try {
          const suggestRes = await fetch(`${API_URL}/commands/suggest/${result.jiraIssueKey}`);
          if (suggestRes.ok) {
            suggestions = await suggestRes.json();
          }
        } catch (sErr) {
          console.error('Auto-suggestion error:', sErr);
        }

        let severity = result.severity;
        if (!severity && result.message) {
          const match = result.message.match(/Severity:\s*(P\d)/i);
          if (match) severity = match[1];
        }

        return {
          content: result.message,
          suggestions,
          severity
        };
      }

      if (result.status === 'merged' || result.status === 'needs_confirmation' || command === '/bug') {
        return { content: result.message };
      }
      return null;
    } catch (err) {
      console.error('Bot API error:', err);
      if (command === '/bug') {
        return { content: '❌ Không thể kết nối với backend để tạo ticket. Vui lòng thử lại sau.' };
      }
      return null;
    }
  };

  const handleSend = async () => {
    if (!input.trim()) return;
    const userMessageContent = input.trim();
    const newMsg: ChatMessage = {
      id: `msg-${Date.now()}`,
      sender: currentUserParticipant,
      content: userMessageContent,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      type: isStaffOrAdmin ? 'staff' : 'customer',
    };
    setMessages(prev => [...prev, newMsg]);
    setInput('');

    // AUTOMATION: If client tags someone, simulate a "no reply" notification after a delay
    if (userMessageContent.includes('@') && !isStaffOrAdmin) {
      setTimeout(() => {
        setMessages(prev => {
          // Only show if the last message is still from the customer (simplified check)
          const lastMsg = prev[prev.length - 1];
          if (lastMsg && lastMsg.sender.type === 'customer') {
            return [...prev, {
              id: `msg-bot-notify-${Date.now()}`,
              sender: incidentXBot,
              content: "🤖 Bot: Nhận thấy nhân sự chưa phản hồi nhắc tên của bạn. Tôi đã gửi thông báo khẩn cấp đến Quản trị viên hệ thống để được hỗ trợ kịp thời.",
              timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
              type: 'bot',
            }];
          }
          return prev;
        });
      }, 8000); // 8 second simulated delay for demo
    }

    // Call Bot API
    const botResponse = await botApi(userMessageContent);

    if (botResponse) {
      let finalContent = botResponse.content;

      // AUTOMATION: Tag suggested staff members based on severity rules
      if (botResponse.suggestions && botResponse.suggestions.length > 0 && botResponse.severity) {
        const severity = botResponse.severity.toUpperCase();
        const sortedSuggestions = [...botResponse.suggestions].sort((a, b) => Number(a.score) - Number(b.score));
        let selectedStaff: any[] = [];

        if (severity === 'P3') {
          // Tag 1 staff with lowest score
          selectedStaff = [sortedSuggestions[0]];
        } else if (severity === 'P2') {
          // Tag 2 staffs with lowest score
          selectedStaff = sortedSuggestions.slice(0, 2);
        } else if (severity === 'P1') {
          // Tag 2 staffs with highest score
          const highestScores = [...botResponse.suggestions].sort((a, b) => Number(b.score) - Number(a.score));
          selectedStaff = highestScores.slice(0, 2);
        }

        if (selectedStaff.length > 0) {
          const allTags = selectedStaff.filter(s => s).map(s => `@${s.username}`).join(' ');
          if (allTags) {
            finalContent = `🔔 ${allTags} vui lòng kiểm tra incident này.\n\n${finalContent}`;
          }
        }
      }

      const botReply: ChatMessage = {
        id: `msg-bot-${Date.now()}`,
        sender: incidentXBot,
        content: finalContent,
        suggestions: botResponse.suggestions,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        type: 'bot',
      };
      setMessages(prev => [...prev, botReply]);
    }
  };

  const handleSwitchClient = (clientId: string) => {
    setActiveClientId(clientId);
  };

  const getInitials = (name: string) => {
    const parts = name.trim().split(' ');
    if (parts.length >= 2) return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    return parts[0].substring(0, 2).toUpperCase();
  };

  const activeClient = allClients.find(c => c.id === activeClientId);

  return (
    <div className={styles.page}>
      <Sidebar />
      <main className={`${styles.main} page-transition`}>
        <button className={styles.backBtn} onClick={() => navigate(`/service/${service.id}`)}>
          <span className="material-symbols-outlined" style={{ fontSize: 18 }}>arrow_back</span>
          {service.name} / {app.name}
        </button>

        <div className={styles.chatContainer}>
          {/* Chat Area */}
          <div className={styles.chatArea}>
            {/* Header */}
            <div className={styles.chatHeader}>
              <div className={styles.chatHeaderInfo}>
                <div className={styles.chatTitleRow}>
                  <h2 className={styles.chatTitle}>
                    {activeClient ? `Chat — ${activeClient.name}` : `${app.name} Incident Channel`}
                  </h2>
                  {activeClient && <span className={styles.criticalBadge}>ACTIVE</span>}
                </div>
                <p className={styles.chatSubtitle}>
                  {activeClient
                    ? `Client: ${activeClient.name} · ${app.name}`
                    : `${participants.staff.length} staff online · ${app.name}`
                  }
                </p>
              </div>
              <div className={styles.chatActions}>
                <button className={`${styles.chatActionBtn} glass-panel`}>
                  <span className="material-symbols-outlined">phone</span>
                </button>
                <button className={`${styles.chatActionBtn} glass-panel`}>
                  <span className="material-symbols-outlined">videocam</span>
                </button>
                <button className={`${styles.chatActionBtn} glass-panel`}>
                  <span className="material-symbols-outlined">more_vert</span>
                </button>
              </div>
            </div>

            {/* Messages */}
            <div className={styles.messages}>
              {messages.map((msg) => {
                if (msg.type === 'system') {
                  return (
                    <div key={msg.id} className={styles.systemMessage}>
                      <div className={styles.systemPill}>
                        <span className="material-symbols-outlined">terminal</span>
                        <span>{msg.content}</span>
                      </div>
                    </div>
                  );
                }

                // User's messages on the right, others on the left
                const isRight = msg.sender.id === user?.id;
                const isSelf = isRight;

                return (
                  <div
                    key={msg.id}
                    className={`${styles.messageRow} ${isRight ? styles.messageRowRight : styles.messageRowLeft}`}
                  >
                    <div className={`${styles.messageAvatar} ${msg.type === 'bot' ? styles.messageAvatarBot :
                      msg.type === 'staff' ? styles.messageAvatarStaff : ''
                      }`}>
                      {msg.type === 'bot' ? (
                        <span className="material-symbols-outlined" style={{ color: '#2dd4bf', fontVariationSettings: "'FILL' 1" }}>smart_toy</span>
                      ) : msg.sender.avatar ? (
                        <img src={msg.sender.avatar} alt={msg.sender.name} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                      ) : (
                        getInitials(msg.sender.name)
                      )}
                    </div>
                    <div className={`${styles.messageBody} ${isRight ? styles.messageBodyRight : ''}`}>
                      <div className={`${styles.messageMeta} ${isRight ? styles.messageMetaRight : ''}`}>
                        {isRight && <span className={styles.messageTime}>{msg.timestamp}</span>}
                        <span className={`${styles.messageSender} ${msg.type === 'bot' ? styles.messageSenderBot : ''}`}>
                          {isSelf ? `You (${msg.sender.name})` : msg.sender.name}
                          {msg.type === 'staff' && !isSelf ? ` · ${msg.sender.role}` : ''}
                        </span>
                        {!isRight && <span className={styles.messageTime}>{msg.timestamp}</span>}
                      </div>
                      <div className={`${styles.messageBubble} ${msg.type === 'customer' ? styles.messageBubbleCustomer :
                        msg.type === 'bot' ? styles.messageBubbleBot :
                          styles.messageBubbleStaff
                        } ${isRight ? styles.bubbleRight : styles.bubbleLeft}`}>
                        <p style={{ whiteSpace: 'pre-wrap' }}>{msg.content}</p>

                        {/* Suggestion Cards */}
                        {msg.suggestions && msg.suggestions.length > 0 && (
                          <div className={styles.suggestionsContainer}>
                            <div className={styles.suggestionsHeader}>Team dispatch suggestion</div>
                            {msg.suggestions.map((s, idx) => (
                              <div key={idx} className={styles.suggestionCard}>
                                <div className={styles.suggestionInfo}>
                                  <div className={styles.suggestionName}>{s.displayName}</div>
                                  <div className={styles.suggestionUser}>{s.username}</div>
                                  <div className={styles.suggestionReason}>{s.reason}</div>
                                </div>
                                <div className={styles.suggestionScore}>
                                  <div className={styles.scoreCircle}>{s.score}</div>
                                </div>
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
              <div ref={messagesEndRef} />
            </div>

            {/* Input */}
            <div className={styles.chatInputArea}>
              {/* Quick Actions Row */}
              <div className={styles.quickActionsRow}>
                <button
                  className={styles.detailActionBtn}
                  onClick={async () => {
                    if (showIncidentDetails) {
                      setShowIncidentDetails(false);
                      return;
                    }

                    setIsLoadingIncidents(true);
                    const API_URL = (import.meta.env.VITE_API_URL as string) || 'http://localhost:8080/api';
                    try {
                      const res = await fetch(`${API_URL}/incidents`);
                      if (res.ok) {
                        const data = await res.json();
                        setIncidentsData(data);
                        setShowIncidentDetails(true);
                      }
                    } catch (err) {
                      console.error('Fetch incidents error:', err);
                    } finally {
                      setIsLoadingIncidents(false);
                    }
                  }}
                >
                  <span className="material-symbols-outlined" style={{ fontSize: 18 }}>
                    {showIncidentDetails ? 'group' : 'table_view'}
                  </span>
                  {isLoadingIncidents ? 'Loading...' : showIncidentDetails ? 'Show Participants' : 'Incidents detail'}
                </button>
              </div>

              <div className={styles.chatInputWrap}>
                <div className={styles.chatInputBtns}>
                  <button className={styles.chatInputBtn}>
                    <span className="material-symbols-outlined">add_circle</span>
                  </button>
                  <button className={styles.chatInputBtn}>
                    <span className="material-symbols-outlined">image</span>
                  </button>
                  <button className={styles.chatInputBtn}>
                    <span className="material-symbols-outlined">attach_file</span>
                  </button>
                </div>
                <textarea
                  id="chat-input"
                  className={styles.chatTextarea}
                  placeholder="Reply to incident..."
                  rows={1}
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' && !e.shiftKey) {
                      e.preventDefault();
                      handleSend();
                    }
                  }}
                />
                <button id="chat-send" className={styles.sendBtn} onClick={handleSend}>
                  <span className="material-symbols-outlined" style={{ fontVariationSettings: "'FILL' 1" }}>send</span>
                </button>
              </div>
            </div>
          </div>

          {/* Resize Handle */}
          <div
            className={`${styles.resizeHandle} ${isResizing ? styles.resizeHandleActive : ''}`}
            onMouseDown={() => setIsResizing(true)}
          />

          {/* ── Participant Sidebar ── */}
          <div className={styles.participantSidebar} style={{ width: sidebarWidth }}>
            <div className={styles.participantHeader}>
              <div className={styles.sidebarHeaderTop}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  {selectedIncident && (
                    <button
                      className={styles.sidebarBackBtn}
                      onClick={() => setSelectedIncident(null)}
                    >
                      <span className="material-symbols-outlined" style={{ fontSize: 20 }}>arrow_back</span>
                    </button>
                  )}
                  <h3 className={styles.participantTitle}>
                    {selectedIncident ? 'Incident Detail' : showIncidentDetails ? 'Incident Details' : 'Participants'}
                  </h3>
                </div>
                {(showIncidentDetails || selectedIncident) && (
                  <button
                    className={styles.sidebarCloseBtn}
                    onClick={() => {
                      setShowIncidentDetails(false);
                      setSelectedIncident(null);
                    }}
                  >
                    <span className="material-symbols-outlined">close</span>
                  </button>
                )}
              </div>
              <p className={styles.participantCount}>
                {selectedIncident
                  ? `Viewing ${selectedIncident.jiraIssueKey}`
                  : showIncidentDetails
                    ? `${incidentsData.length} Total Incidents`
                    : `${allParticipants.length} Active in Channel`
                }
              </p>
            </div>

            <div className={styles.sidebarContent}>
              {selectedIncident ? (
                <div className={styles.incidentDetailView}>
                  <div className={styles.detailHeader}>
                    <div className={styles.detailTitleRow}>
                      <span className={styles.detailKey}>{selectedIncident.jiraIssueKey}</span>
                      <span className={`${styles.sevBadge} ${styles[`sev${selectedIncident.severity}`]}`}>
                        {selectedIncident.severity}
                      </span>
                    </div>
                    <h4 className={styles.detailTitleText}>{selectedIncident.title}</h4>
                  </div>

                  <div className={styles.detailStats}>
                    <div className={styles.statItem}>
                      <span className={styles.statLabel}>Status</span>
                      <span className={`${styles.statusBadge} ${styles[`status${selectedIncident.status.replace(/\s+/g, '')}`]}`}>
                        {selectedIncident.status}
                      </span>
                    </div>
                    <div className={styles.statItem}>
                      <span className={styles.statLabel}>Reporter</span>
                      <span className={styles.statValue}>{selectedIncident.reporter || 'N/A'}</span>
                    </div>
                    <div className={styles.statItem}>
                      <span className={styles.statLabel}>Assigned To</span>
                      <span className={styles.statValue}>{selectedIncident.assignedTo || 'Unassigned'}</span>
                    </div>
                  </div>

                  <div className={styles.detailSection}>
                    <h5 className={styles.sectionTitle}>Description</h5>
                    <p className={styles.sectionContent}>
                      {selectedIncident.description || 'No description provided.'}
                    </p>
                  </div>

                  {selectedIncident.jiraUrl && (
                    <a
                      href={selectedIncident.jiraUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className={styles.jiraLink}
                    >
                      <span className="material-symbols-outlined">open_in_new</span>
                      View in Jira
                    </a>
                  )}

                  <div className={styles.detailActions}>
                    <button className={styles.actionBtnPrimary}>
                      Update Status
                    </button>
                    <button className={styles.actionBtnSecondary}>
                      Assign Me
                    </button>
                  </div>
                </div>
              ) : showIncidentDetails ? (
                <div className={styles.incidentTableContainer}>
                  <table className={styles.incidentTable}>
                    <thead>
                      <tr>
                        <th>Key</th>
                        <th>Status</th>
                        <th>Severity</th>
                      </tr>
                    </thead>
                    <tbody>
                      {incidentsData.map((inc, idx) => (
                        <tr
                          key={idx}
                          className={styles.incidentRow}
                          onClick={() => handleIncidentClick(inc.jiraIssueKey)}
                        >
                          <td className={styles.incKey}>{inc.jiraIssueKey}</td>
                          <td>
                            <span className={`${styles.statusBadge} ${styles[`status${inc.status.replace(/\s+/g, '')}`]}`}>
                              {inc.status}
                            </span>
                          </td>
                          <td>
                            <span className={`${styles.sevBadge} ${styles[`sev${inc.severity}`]}`}>
                              {inc.severity}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                  <div className={styles.incidentDetailsList}>
                    {incidentsData.map((inc, idx) => (
                      <div
                        key={idx}
                        className={styles.incidentDetailItem}
                        onClick={() => handleIncidentClick(inc.jiraIssueKey)}
                      >
                        <div className={styles.incItemHeader}>
                          <span className={styles.incItemKey}>{inc.jiraIssueKey}</span>
                          <span className={styles.incItemSev}>{inc.severity}</span>
                        </div>
                        <p className={styles.incItemTitle}>{inc.title}</p>
                        <div className={styles.incItemFooter}>
                          <span className={styles.incItemStatus}>{inc.status}</span>
                          <span className={styles.incItemUser}>{inc.assignedTo || 'Unassigned'}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ) : (
                <div className={styles.participantList}>
                  {/* Customer / Active Client */}
                  {participants.clients.length > 0 && (
                    <div className={styles.participantGroup}>
                      <p className={`${styles.participantGroupLabel} ${styles.participantGroupLabelCustomer}`}>Client</p>
                      {participants.clients.map((p) => (
                        <div key={p.id} className={styles.participantItem}>
                          <div className={styles.participantItemInfo}>
                            <div className={styles.participantAvatar}>
                              <div className={styles.participantAvatarImg}>
                                {p.avatar ? (
                                  <img src={p.avatar} alt={p.name} style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: '50%' }} />
                                ) : (
                                  getInitials(p.name)
                                )}
                              </div>
                              {p.isOnline && <span className={styles.participantOnline} />}
                            </div>
                            <div>
                              <p className={styles.participantName}>{p.name}</p>
                              <p className={`${styles.participantRole} ${styles.participantRoleCustomer}`}>{p.role}</p>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}

                  {/* Staff / Admin Team */}
                  <div className={styles.participantGroup}>
                    <p className={`${styles.participantGroupLabel} ${styles.participantGroupLabelTeam}`}>Service Team</p>
                    {participants.staff.map((p) => (
                      <div key={p.id} className={styles.participantItem}>
                        <div className={styles.participantItemInfo}>
                          <div className={styles.participantAvatar}>
                            <div className={`${styles.participantAvatarImg} ${styles.participantAvatarTeam}`}>
                              {p.avatar ? (
                                <img src={p.avatar} alt={p.name} style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: '50%' }} />
                              ) : (
                                getInitials(p.name)
                              )}
                            </div>
                            {p.isOnline && <span className={styles.participantOnline} />}
                          </div>
                          <div>
                            <p className={styles.participantName}>
                              {p.id === user?.id ? `${p.name} (You)` : p.name}
                            </p>
                            <p className={`${styles.participantRole} ${styles.participantRoleTeam}`}>{p.role}</p>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>

                  {/* Bot */}
                  <div className={styles.participantGroup}>
                    <p className={`${styles.participantGroupLabel} ${styles.participantGroupLabelBot}`}>Automations</p>
                    <div className={`${styles.participantItem} ${styles.participantItemBot}`}>
                      <div className={styles.participantItemInfo}>
                        <div className={styles.participantAvatar}>
                          <div className={`${styles.participantAvatarImg} ${styles.participantAvatarBot}`}>
                            <span className="material-symbols-outlined" style={{ fontSize: 14, color: '#2dd4bf' }}>smart_toy</span>
                          </div>
                        </div>
                        <div>
                          <p className={styles.participantName}>{participants.bot.name}</p>
                          <p className={`${styles.participantRole} ${styles.participantRoleBot}`}>{participants.bot.role}</p>
                        </div>
                      </div>
                      <span className={styles.participantPulse} />
                    </div>
                  </div>
                </div>
              )}
            </div>

            {/* ── Switch Client Section (staff/admin only) ── */}
            {isStaffOrAdmin && allClients.length > 0 && (
              <div className={styles.clientSwitcher}>
                <div className={styles.clientSwitcherHeader}>
                  <span className="material-symbols-outlined" style={{ fontSize: 16, color: '#44e2cd' }}>swap_horiz</span>
                  <span className={styles.clientSwitcherTitle}>Switch Client</span>
                  <span className={styles.clientSwitcherCount}>{allClients.length}</span>
                </div>
                <div className={styles.clientSwitcherList}>
                  {allClients.map((client) => (
                    <button
                      key={client.id}
                      className={`${styles.clientSwitcherItem} ${activeClientId === client.id ? styles.clientSwitcherItemActive : ''
                        }`}
                      onClick={() => handleSwitchClient(client.id)}
                    >
                      <div className={styles.clientSwitcherInfo}>
                        <div className={styles.clientSwitcherAvatar}>
                          {client.avatar ? (
                            <img src={client.avatar} alt={client.name} style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: '50%' }} />
                          ) : (
                            getInitials(client.name)
                          )}
                        </div>
                        <div>
                          <p className={styles.clientSwitcherName}>{client.name}</p>
                          <p className={styles.clientSwitcherRole}>{client.role}</p>
                        </div>
                      </div>
                      {activeClientId === client.id && (
                        <span className={styles.clientSwitcherActive}>
                          <span className="material-symbols-outlined" style={{ fontSize: 14 }}>chat</span>
                        </span>
                      )}
                      {client.isOnline && activeClientId !== client.id && (
                        <span className={styles.clientSwitcherOnline} />
                      )}
                    </button>
                  ))}
                </div>
              </div>
            )}

            <button className={styles.inviteBtn}>
              <span className="material-symbols-outlined" style={{ fontSize: 14 }}>person_add</span>
              Invite Teammate
            </button>
          </div>
        </div>
      </main>
    </div>
  );
}
