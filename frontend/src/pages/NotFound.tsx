import { useNavigate } from 'react-router-dom';
import PageHeader from '../components/ui/PageHeader';

export default function NotFound() {
  const navigate = useNavigate();

  return (
    <div>
      <PageHeader
        title="Page not found"
        subtitle="That address is not a page in AgentOps CRM. Nothing was changed."
      />
      <div className="flex flex-wrap gap-3">
        <button type="button" className="btn-primary" onClick={() => navigate('/dashboard')}>
          Return to Dashboard
        </button>
        <button type="button" className="btn-secondary" onClick={() => navigate(-1)}>
          Go back
        </button>
      </div>
    </div>
  );
}
