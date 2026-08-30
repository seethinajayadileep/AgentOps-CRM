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
          className="mb-4 inline-flex items-center gap-2 text-sm text-slate hover:text-ink"
        >
          <ArrowLeft size={18} />
          Back to Business
        </button>
        <div className="flex items-center gap-3">
          <span className="flex h-11 w-11 items-center justify-center rounded-sm bg-ink ">
            <Sparkles size={22} className="text-snow" />
          </span>
          <div>
            <h1 className="flex items-center gap-2 text-2xl font-bold text-ink">
              Support Chat <Badge color="purple">AI Agent</Badge>
            </h1>
            <p className="text-sm text-slate">
              AI answers from the business knowledge base and can collect leads when customers are interested.
            </p>
          </div>
        </div>
      </div>

      {error && (
        <div className="mb-4 flex items-start justify-between rounded-sm border border-frost bg-mist p-4 text-ink">
          <span>{error}</span>
          <button onClick={() => setError(null)} className="ml-4 font-bold text-ink hover:text-ink">
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
              <span className="mb-4 flex h-16 w-16 items-center justify-center rounded-sm bg-mist">
                <Bot className="h-8 w-8 text-slate" />
              </span>
              <h2 className="mb-2 text-xl font-semibold text-ink">Start a conversation</h2>
              <p className="max-w-md text-slate">
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
                        ? 'bg-mist text-ink'
                        : 'bg-ink text-snow'
                    }`}
                  >
                    {isUser ? <User size={16} /> : <Bot size={16} />}
                  </span>
                  <div
                    className={`rounded-sm p-4 ${
                      isUser
                        ? 'bg-gradient-to-br from-slate to-silver text-ink'
                        : 'border border-frost bg-mist text-ink '
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
                          <p className="mt-1 text-xs italic text-slate">{message.evaluation.reason}</p>
                        )}
                      </div>
                    )}

                    {!isUser && !hideMeta && message.sources && message.sources.length > 0 && (
                      <div className="mt-3 border-t border-frost pt-3">
                        <p className="mb-2 text-xs font-semibold text-slate">Sources</p>
                        <div className="flex flex-wrap gap-2">
                          {message.sources.map((source, idx) => (
                            <a
                              key={idx}
                              href={source}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="inline-flex max-w-[260px] items-center gap-1 rounded-full border border-frost bg-mist px-2.5 py-1 text-xs text-ink hover:bg-frost"
                            >
                              <Globe size={11} />
                              <span className="truncate">{source}</span>
                            </a>
                          ))}
                        </div>
                      </div>
                    )}

                    <p className={`mt-2 text-xs ${isUser ? 'text-slate' : 'text-slate'}`}>
                      {message.timestamp.toLocaleTimeString()}
                    </p>
                  </div>
                </div>
              </div>
            );
          })}

          {loading && (
            <div className="flex justify-start">
              <div className="flex items-center gap-2 rounded-sm border border-frost bg-mist p-4">
                <Bot size={16} className="text-slate" />
                <div className="flex gap-1">
                  <div className="h-2 w-2 animate-bounce rounded-full bg-slate" style={{ animationDelay: '0ms' }} />
                  <div className="h-2 w-2 animate-bounce rounded-full bg-slate" style={{ animationDelay: '150ms' }} />
                  <div className="h-2 w-2 animate-bounce rounded-full bg-slate" style={{ animationDelay: '300ms' }} />
                </div>
                <span className="text-sm text-slate">Thinking…</span>
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* Sticky Input */}
        <div className="border-t border-frost bg-snow p-4 ">
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
          <p className="mt-2 text-xs text-slate">
            The AI answers using only the business knowledge base and can collect leads when customers are interested.
          </p>
        </div>
      </div>
    </div>
  );
}
