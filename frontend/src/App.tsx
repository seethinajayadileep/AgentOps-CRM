import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import Businesses from './pages/Businesses';
import AddBusiness from './pages/AddBusiness';
import BusinessDetail from './pages/BusinessDetail';
import LeadsPage from './pages/LeadsPage';
import LeadDetailPage from './pages/LeadDetailPage';
import Conversations from './pages/Conversations';
import VoiceCalls from './pages/VoiceCalls';
import ApprovalsPage from './pages/ApprovalsPage';
import AgentLogs from './pages/AgentLogs';
import Settings from './pages/Settings';
import SupportChat from './pages/SupportChat';
import LeadFinder from './pages/LeadFinder';
import LeadFinderResults from './pages/LeadFinderResults';

/**
 * Main App component with routing configuration.
 *
 * @version 0.2.0
 */
function App() {
  return (
    <BrowserRouter
      future={{
        v7_startTransition: true,
        v7_relativeSplatPath: true,
      }}
    >
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<Dashboard />} />
          <Route path="businesses" element={<Businesses />} />
          <Route path="businesses/new" element={<AddBusiness />} />
          <Route path="businesses/:id" element={<BusinessDetail />} />
          <Route path="businesses/:id/edit" element={<AddBusiness />} />
          <Route path="businesses/:businessId/chat" element={<SupportChat />} />
          <Route path="leads" element={<LeadsPage />} />
          <Route path="leads/:id" element={<LeadDetailPage />} />
          <Route path="lead-finder" element={<LeadFinder />} />
          <Route path="lead-finder/:id" element={<LeadFinderResults />} />
          <Route path="conversations" element={<Conversations />} />
          <Route path="voice-calls" element={<VoiceCalls />} />
          <Route path="approvals" element={<ApprovalsPage />} />
          <Route path="agent-logs" element={<AgentLogs />} />
          <Route path="settings" element={<Settings />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;