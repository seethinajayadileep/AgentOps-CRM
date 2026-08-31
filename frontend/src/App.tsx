import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import Layout from './components/Layout';
import MarketingLayout from './components/marketing/MarketingLayout';
import AuthLayout from './components/auth/AuthLayout';
import ProtectedRoute from './components/auth/ProtectedRoute';
import GuestRoute from './components/auth/GuestRoute';
import Landing from './pages/marketing/Landing';
import Features from './pages/marketing/Features';
import HowItWorks from './pages/marketing/HowItWorks';
import Pricing from './pages/marketing/Pricing';
import Contact from './pages/marketing/Contact';
import Login from './pages/auth/Login';
import Signup from './pages/auth/Signup';
import ForgotPassword from './pages/auth/ForgotPassword';
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
import NotFound from './pages/NotFound';

function App() {
  return (
    <AuthProvider>
      <BrowserRouter
        future={{
          v7_startTransition: true,
          v7_relativeSplatPath: true,
        }}
      >
        <Routes>
          <Route element={<MarketingLayout />}>
            <Route path="/" element={<Landing />} />
            <Route path="features" element={<Features />} />
            <Route path="how-it-works" element={<HowItWorks />} />
            <Route path="pricing" element={<Pricing />} />
            <Route path="contact" element={<Contact />} />
          </Route>

          <Route element={<AuthLayout />}>
            <Route
              path="login"
              element={
                <GuestRoute>
                  <Login />
                </GuestRoute>
              }
            />
            <Route
              path="signup"
              element={
                <GuestRoute>
                  <Signup />
                </GuestRoute>
              }
            />
            <Route
              path="forgot-password"
              element={
                <GuestRoute>
                  <ForgotPassword />
                </GuestRoute>
              }
            />
          </Route>

          <Route
            element={
              <ProtectedRoute>
                <Layout />
              </ProtectedRoute>
            }
          >
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

          <Route path="home" element={<Navigate to="/" replace />} />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
