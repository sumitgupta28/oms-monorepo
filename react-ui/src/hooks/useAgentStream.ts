import { useState, useRef, useCallback } from "react";

export interface Message { role: "user" | "assistant"; content: string; toolCalls?: string[]; }

export function useAgentStream() {
  const [messages, setMessages]   = useState<Message[]>([]);
  const [streaming, setStreaming] = useState(false);
  const [toolCalls, setToolCalls] = useState<string[]>([]);
  const esRef = useRef<EventSource | null>(null);

  const sendMessage = useCallback((text: string, sessionId: string) => {
    const token = localStorage.getItem("access_token");
    if (!token) return;

    setMessages(prev => [...prev, { role: "user", content: text }]);
    setMessages(prev => [...prev, { role: "assistant", content: "", toolCalls: [] }]);
    setStreaming(true);
    setToolCalls([]);

    const url = `${import.meta.env.VITE_GATEWAY_URL}/api/v1/chat/stream?message=${encodeURIComponent(text)}&sessionId=${sessionId}&access_token=${token}`;
    const es = new EventSource(url);
    esRef.current = es;

    es.onmessage = (event) => {
      setMessages(prev => {
        const updated = [...prev];
        const last = { ...updated[updated.length - 1] };
        last.content += event.data;
        updated[updated.length - 1] = last;
        return updated;
      });
    };

    es.addEventListener("tool-call", (event: MessageEvent) => {
      setToolCalls(prev => [...prev, event.data]);
      setMessages(prev => {
        const updated = [...prev];
        const last = { ...updated[updated.length - 1] };
        last.toolCalls = [...(last.toolCalls || []), event.data];
        updated[updated.length - 1] = last;
        return updated;
      });
    });

    es.addEventListener("done", () => { es.close(); setStreaming(false); });
    es.onerror = () => { es.close(); setStreaming(false); };
  }, []);

  const clearMessages = useCallback(() => setMessages([]), []);

  return { messages, streaming, toolCalls, sendMessage, clearMessages };
}
