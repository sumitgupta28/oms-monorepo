import React, { useState, useRef, useEffect } from "react";
import { useAgentStream, Message } from "../../hooks/useAgentStream";
import { useAuth } from "../../auth/AuthContext";

function ToolCallChip({ name }: { name: string }) {
  return (
    <span className="inline-flex items-center gap-1.5 text-xs px-2 py-1 rounded-md bg-teal-50 text-teal-700 border border-teal-100 mb-1">
      <span className="w-1.5 h-1.5 rounded-full bg-teal-500"/>
      {name}
    </span>
  );
}

function MessageBubble({ msg }: { msg: Message }) {
  const isUser = msg.role === "user";
  return (
    <div className={`flex flex-col ${isUser ? "items-end" : "items-start"} gap-1`}>
      {!isUser && msg.toolCalls?.map((tc, i) => <ToolCallChip key={i} name={tc}/>)}
      <div className={`max-w-lg px-4 py-2.5 rounded-2xl text-sm leading-relaxed
        ${isUser ? "bg-purple-700 text-white rounded-br-sm" : "bg-gray-100 text-gray-900 rounded-bl-sm"}`}>
        {msg.content || <span className="text-gray-400 animate-pulse">●</span>}
      </div>
    </div>
  );
}

export default function ChatPage() {
  const { user }                          = useAuth();
  const { messages, streaming, sendMessage } = useAgentStream();
  const [input,     setInput]             = useState("");
  const sessionId                         = useRef("session-" + Date.now());
  const bottomRef                         = useRef<HTMLDivElement>(null);

  useEffect(() => { bottomRef.current?.scrollIntoView({ behavior: "smooth" }); }, [messages]);

  const handleSend = () => {
    if (!input.trim() || streaming) return;
    sendMessage(input.trim(), sessionId.current);
    setInput("");
  };

  const quickActions = [
    "What products do you have?",
    "Show my recent orders",
    "Find me a gaming laptop under $1000",
  ];

  return (
    <div className="flex h-[calc(100vh-57px)]">
      <div className="flex-1 flex flex-col max-w-3xl mx-auto w-full px-4 py-4">
        <div className="flex-1 overflow-y-auto space-y-4 pb-4">
          {messages.length === 0 && (
            <div className="text-center py-12">
              <div className="text-4xl mb-3">🤖</div>
              <h2 className="text-lg font-medium text-gray-900">Hi {user?.firstName}!</h2>
              <p className="text-gray-500 text-sm mt-1">I can help you order products, track shipments, and more.</p>
              <div className="flex flex-wrap justify-center gap-2 mt-6">
                {quickActions.map(q=>(
                  <button key={q} onClick={()=>sendMessage(q,sessionId.current)}
                    className="text-xs px-3 py-2 rounded-full border border-gray-200 text-gray-600 hover:bg-gray-50">
                    {q}
                  </button>
                ))}
              </div>
            </div>
          )}
          {messages.map((msg, i) => <MessageBubble key={i} msg={msg}/>)}
          <div ref={bottomRef}/>
        </div>
        <div className="flex gap-3 pt-3 border-t border-gray-100">
          <textarea value={input} onChange={e=>setInput(e.target.value)}
            onKeyDown={e=>{if(e.key==="Enter"&&!e.shiftKey){e.preventDefault();handleSend();}}}
            placeholder="Ask anything about your orders…"
            rows={1}
            className="flex-1 px-4 py-2.5 rounded-xl border border-gray-200 text-sm resize-none focus:outline-none focus:border-purple-400 bg-white"/>
          <button onClick={handleSend} disabled={!input.trim()||streaming}
            className="px-5 py-2.5 rounded-xl bg-purple-700 text-white text-sm font-medium hover:bg-purple-800 disabled:opacity-50">
            {streaming ? "…" : "Send"}
          </button>
        </div>
      </div>
    </div>
  );
}
