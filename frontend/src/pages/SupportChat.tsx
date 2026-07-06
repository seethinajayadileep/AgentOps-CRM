import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Send, Bot, User, Sparkles, Globe } from 'lucide-react';
import { askQuestion, AskResponse, EvaluationSummary } from '../api/chat';
import Badge from '../components/ui/Badge';

interface Message {
  role: 'user' | 'assistant';
  content: string;
  sources?: string[];
  confidenceScore?: number;
  evaluation?: EvaluationSummary | null;
  leadDetected?: boolean;
  timestamp: Date;
}

/**
 * Support Chat test page
 * Feature ID: F-005
 */
export default function SupportChat() {
  const { businessId } = useParams<{ businessId: string }>();
  const navigate = useNavigate();

  const [question, setQuestion] = useState('');
  const [messages, setMessages] = useState<Message[]>([]);
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSendMessage = async () => {
    if (!question.trim() || !businessId) return;

    const userMessage: Message = {
      role: 'user',
      content: question,
      timestamp: new Date(),
    };

    setMessages((prev) => [...prev, userMessage]);
    setQuestion('');
    setLoading(true);
    setError(null);

    try {
      const response: AskResponse = await askQuestion({
        businessId: businessId,
        conversationId,
        question: question.trim(),
      });

      if (!conversationId) {
        setConversationId(response.conversationId);
      }

      const assistantMessage: Message = {
        role: 'assistant',
        content: response.answer,
        sources: response.sources,
        confidenceScore: response.confidenceScore,
        evaluation: response.evaluation ?? null,
        leadDetected: response.leadDetected,
        timestamp: new Date(),
      };

      setMessages((prev) => [...prev, assistantMessage]);
    } catch (err: any) {
      console.error('Error asking question:', err);
      setError(err.response?.data?.message || 'Failed to get response. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleKeyPress = (event: React.KeyboardEvent) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      handleSendMessage();
    }
  };

  const handleBack = () => {
    navigate(`/businesses/${businessId}`);
  };

  const getConfidenceColor = (score: number) => {
    if (score > 70) return 'green';
    if (score > 40) return 'amber';
    return 'red';
  };

  const getRiskColor = (risk: string) => {
    switch (risk) {
      case 'LOW':
        return 'green';
      case 'MEDIUM':
        return 'amber';
      case 'HIGH':
        return 'red';
      default:
        return 'gray';
    }
  };

  return (
    <div className="mx-auto flex max-w-4xl flex-col">
      {/* Header */}
      <div className="mb-6">
        <button
          onClick={handleBack}
          className="mb-4 inline-flex items-center gap-2 text-sm text-zinc-400 hover:text-zinc-100"
        >
          <ArrowLeft size={18} />
          Back to Business
        </button>
        <div className="flex items-center gap-3">
          <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-gradient-to-br from-[#8B5CF6] to-[#3B82F6] shadow-[0_0_20px_rgba(139,92,246,0.3)]">
            <Sparkles size={22} className="text-white" />
          </span>
          <div>
            <h1 className="flex items-center gap-2 text-2xl font-bold text-white">
              Support Chat <Badge color="purple">AI Agent</Badge>
            </h1>
            <p className="text-sm text-zinc-400">
              AI answers from the business knowledge base and can collect leads when customers are interested.
            </p>
          </div>
        </div>
      </div>

      {error && (
        <div className="mb-4 flex items-start justify-between rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-red-300">
          <span>{error}</span>
          <button onClick={() => setError(null)} className="ml-4 font-bold text-red-300 hover:text-red-200">
            ×
          </button>
        </div>
      )}

      {/* Chat Card */}
      <div className="glass-card flex h-[68vh] flex-col overflow-hidden">
        {/* Messages */}
        <div className="flex-1 space-y-4 overflow-y-auto p-4 sm:p-6">
          {messages.length === 0 && (
            <div className="flex h-full flex-col items-center justify-center text-center">
              <span className="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-white/5">
                <Bot className="h-8 w-8 text-primary-400" />
              </span>
              <h2 className="mb-2 text-xl font-semibold text-zinc-200">Start a conversation</h2>
              <p className="max-w-md text-zinc-500">
                Ask any question about the business and the AI agent will answer using only the knowledge base
                information.
              </p>
            </div>
          )}

          {messages.map((message, index) => {
            const isUser = message.role === 'user';
            // Hide confidence/sources for lead-capture messages when backend flags them.
            const hideMeta = !isUser && message.leadDetected === true;
            return (
              <div key={index} className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
                <div className={`flex max-w-[80%] gap-2 ${isUser ? 'flex-row-reverse' : 'flex-row'}`}>
                  <span
                    className={`mt-1 flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-lg ${
                      isUser
                        ? 'bg-blue-500/20 text-blue-300'
                        : 'bg-gradient-to-br from-[#8B5CF6] to-[#3B82F6] text-white'
                    }`}
                  >
                    {isUser ? <User size={16} /> : <Bot size={16} />}
                  </span>
                  <div
                    className={`rounded-2xl p-4 ${
                      isUser
                        ? 'bg-gradient-to-br from-[#3B82F6] to-[#2563EB] text-white'
                        : 'border border-white/[0.08] bg-white/[0.04] text-zinc-100 backdrop-blur-md'
                    }`}
                  >
                    <div className="mb-1 flex items-center gap-2">
                      <span className="text-xs font-semibold opacity-90">
                        {isUser ? 'You' : 'AI Agent'}
                      </span>
                    </div>

                    <p className="whitespace-pre-wrap leading-relaxed">{message.content}</p>

                    {!isUser && !hideMeta && message.confidenceScore !== undefined && (
                      <div className="mt-3">
                        <Badge color={getConfidenceColor(message.confidenceScore)}>
                          Confidence: {message.confidenceScore}%
                        </Badge>
                      </div>
                    )}

                    {!isUser && !hideMeta && message.evaluation && (
                      <div className="mt-2">
                        <Badge color={getRiskColor(message.evaluation.hallucinationRisk)}>
                          {message.evaluation.safeToSend ? 'Safe' : 'Blocked'} ·{' '}
                          {message.evaluation.hallucinationRisk} risk
                        </Badge>
                        {message.evaluation.reason && (
                          <p className="mt-1 text-xs italic text-zinc-400">{message.evaluation.reason}</p>
                        )}
                      </div>
                    )}

                    {!isUser && !hideMeta && message.sources && message.sources.length > 0 && (
                      <div className="mt-3 border-t border-white/[0.08] pt-3">
                        <p className="mb-2 text-xs font-semibold text-zinc-400">Sources</p>
                        <div className="flex flex-wrap gap-2">
                          {message.sources.map((source, idx) => (
                            <a
                              key={idx}
                              href={source}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="inline-flex max-w-[260px] items-center gap-1 rounded-full border border-white/[0.1] bg-white/[0.04] px-2.5 py-1 text-xs text-blue-300 hover:bg-white/[0.08]"
                            >
                              <Globe size={11} />
                              <span className="truncate">{source}</span>
                            </a>
                          ))}
                        </div>
                      </div>
                    )}

                    <p className={`mt-2 text-xs ${isUser ? 'text-blue-100/80' : 'text-zinc-500'}`}>
                      {message.timestamp.toLocaleTimeString()}
                    </p>
                  </div>
                </div>
              </div>
            );
          })}

          {loading && (
            <div className="flex justify-start">
              <div className="flex items-center gap-2 rounded-2xl border border-white/[0.08] bg-white/[0.04] p-4">
                <Bot size={16} className="text-primary-400" />
                <div className="flex gap-1">
                  <div className="h-2 w-2 animate-bounce rounded-full bg-zinc-500" style={{ animationDelay: '0ms' }} />
                  <div className="h-2 w-2 animate-bounce rounded-full bg-zinc-500" style={{ animationDelay: '150ms' }} />
                  <div className="h-2 w-2 animate-bounce rounded-full bg-zinc-500" style={{ animationDelay: '300ms' }} />
                </div>
                <span className="text-sm text-zinc-400">Thinking…</span>
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* Sticky Input */}
        <div className="border-t border-white/[0.08] bg-black/30 p-4 backdrop-blur-md">
          <div className="flex gap-2">
            <textarea
              className="input-dark flex-1 resize-none"
              placeholder="Ask a question…"
              value={question}
              onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => setQuestion(e.target.value)}
              onKeyPress={handleKeyPress}
              disabled={loading}
              rows={2}
            />
            <button onClick={handleSendMessage} disabled={!question.trim() || loading} className="btn-primary">
              <Send size={16} />
              Send
            </button>
          </div>
          <p className="mt-2 text-xs text-zinc-500">
            The AI answers using only the business knowledge base and can collect leads when customers are interested.
          </p>
        </div>
      </div>
    </div>
  );
}
