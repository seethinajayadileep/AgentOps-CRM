import { useState, useEffect } from 'react';
import { useNavigate, Link, useParams } from 'react-router-dom';
import { ArrowLeft, Save } from 'lucide-react';
import { businessApi } from '../api/business';
import type { CreateBusinessRequest, UpdateBusinessRequest, ApiResponse, Business } from '../types/index';
import PageHeader from '../components/ui/PageHeader';
import Card from '../components/ui/Card';

/**
 * Add/Edit business form page.
 *
 * @version 0.3.0
 * Feature: F-002
 */
export default function AddBusiness() {
  const navigate = useNavigate();
  const { id } = useParams<{ id?: string }>();
  const isEditMode = !!id;

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const [formData, setFormData] = useState<CreateBusinessRequest>({
    name: '',
    websiteUrl: '',
    industry: '',
    description: '',
    contactEmail: '',
    contactPhone: '',
  });

  const [pageTitle, setPageTitle] = useState('Add New Business');

  useEffect(() => {
    if (isEditMode && id) {
      fetchBusiness();
    }
  }, [isEditMode, id]);

  const fetchBusiness = async () => {
    setLoading(true);
    setError(null);

    try {
      const response: ApiResponse<Business> = await businessApi.getBusinessById(id!);
      if (response.success && response.data) {
        setFormData({
          name: response.data.name,
          websiteUrl: response.data.websiteUrl,
          industry: response.data.industry || '',
          description: response.data.description || '',
          contactEmail: response.data.contactEmail || '',
          contactPhone: response.data.contactPhone || '',
        });
        setPageTitle('Edit Business');
      } else {
        setError(response.error || 'Failed to load business');
      }
    } catch (err: any) {
      setError(err.message || 'Network error occurred');
    } finally {
      setLoading(false);
    }
  };

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.name.trim()) {
      newErrors.name = 'Name is required';
    }
    if (!formData.websiteUrl.trim()) {
      newErrors.websiteUrl = 'Website URL is required';
    } else if (!/^https?:\/\/.+/.test(formData.websiteUrl)) {
      newErrors.websiteUrl = 'Website URL must start with http:// or https://';
    }
    if (formData.contactEmail && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.contactEmail)) {
      newErrors.contactEmail = 'Invalid email format';
    }
    // Only validate phone format if a non-empty phone is provided
    if (formData.contactPhone && formData.contactPhone.trim()) {
      if (!/^\+?[0-9\-\s\(\)]{7,20}$/.test(formData.contactPhone)) {
        newErrors.contactPhone = 'Invalid phone format';
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validate()) {
      return;
    }

    setLoading(true);
    setError(null);
    setErrors({});

    try {
      let response: ApiResponse<Business>;

      // Normalize blank phone to undefined/empty string
      const normalizeBlank = (value?: string) => (value && value.trim()) || '';

      if (isEditMode) {
        const updateData: UpdateBusinessRequest = {
          name: formData.name,
          websiteUrl: formData.websiteUrl,
          industry: formData.industry || undefined,
          description: formData.description || undefined,
          contactEmail: formData.contactEmail || undefined,
          contactPhone: normalizeBlank(formData.contactPhone) || undefined,
        };
        response = await businessApi.updateBusiness(id!, updateData);
      } else {
        const createData: CreateBusinessRequest = {
          ...formData,
          contactPhone: normalizeBlank(formData.contactPhone),
        };
        response = await businessApi.createBusiness(createData);
      }

      if (response.success) {
        setSuccess(true);
        setTimeout(() => navigate('/businesses'), 1500);
      } else {
        setError(response.error || 'Failed to save business');
      }
    } catch (err: any) {
      if (err.isValidationError && err.validationErrors) {
        const fieldMapping: Record<string, string> = {
          name: 'name',
          websiteUrl: 'websiteUrl',
          industry: 'industry',
          description: 'description',
          contactEmail: 'contactEmail',
          contactPhone: 'contactPhone',
        };

        const validationErrors: Record<string, string> = {};
        Object.entries(err.validationErrors).forEach(([field, message]) => {
          const frontendField = fieldMapping[field] || field;
          validationErrors[frontendField] = message as string;
        });

        setErrors(validationErrors);
        setError('Please fix the validation errors below');
      } else {
        setError(err.message || 'Failed to save business');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (field: keyof CreateBusinessRequest, value: string) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors((prev) => ({ ...prev, [field]: '' }));
    }
  };

  const inputClass = (field: string) =>
    `input-dark ${errors[field] ? 'border-red-500/60 focus:ring-red-500/50' : ''}`;

  return (
    <div className="mx-auto max-w-3xl">
      <PageHeader
        title={pageTitle}
        subtitle={isEditMode ? 'Update business information' : 'Onboard a business for AI-powered customer interactions'}
        back={
          <Link to="/businesses" className="inline-flex items-center gap-2 text-sm text-zinc-400 hover:text-zinc-100">
            <ArrowLeft size={18} />
            <span>Back to Businesses</span>
          </Link>
        }
      />

      {success && (
        <div className="mb-6 rounded-xl border border-[#22C55E]/30 bg-[#22C55E]/10 p-4 text-[#4ade80]">
          {isEditMode ? 'Business updated successfully' : 'Business created successfully'}! Redirecting…
        </div>
      )}

      {error && (
        <div className="mb-6 rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-red-300">{error}</div>
      )}

      {!success && (
        <Card className="p-6">
          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label htmlFor="name" className="label-dark">
                Business Name <span className="text-red-400">*</span>
              </label>
              <input
                id="name"
                type="text"
                value={formData.name}
                onChange={(e) => handleChange('name', e.target.value)}
                placeholder="e.g., Acme Corporation"
                className={inputClass('name')}
              />
              {errors.name && <p className="mt-1 text-sm text-red-400">{errors.name}</p>}
            </div>

            <div>
              <label htmlFor="websiteUrl" className="label-dark">
                Website URL <span className="text-red-400">*</span>
              </label>
              <input
                id="websiteUrl"
                type="url"
                value={formData.websiteUrl}
                onChange={(e) => handleChange('websiteUrl', e.target.value)}
                placeholder="https://example.com"
                className={inputClass('websiteUrl')}
              />
              {errors.websiteUrl && <p className="mt-1 text-sm text-red-400">{errors.websiteUrl}</p>}
            </div>

            <div>
              <label htmlFor="industry" className="label-dark">
                Industry
              </label>
              <input
                id="industry"
                type="text"
                value={formData.industry}
                onChange={(e) => handleChange('industry', e.target.value)}
                placeholder="e.g., Technology, Healthcare, Finance"
                className={inputClass('industry')}
              />
              {errors.industry && <p className="mt-1 text-sm text-red-400">{errors.industry}</p>}
            </div>

            <div>
              <label htmlFor="description" className="label-dark">
                Description
              </label>
              <textarea
                id="description"
                value={formData.description}
                onChange={(e) => handleChange('description', e.target.value)}
                placeholder="Brief description of the business"
                rows={3}
                className={`${inputClass('description')} resize-none`}
              />
              {errors.description && <p className="mt-1 text-sm text-red-400">{errors.description}</p>}
            </div>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div>
                <label htmlFor="contactEmail" className="label-dark">
                  Contact Email
                </label>
                <input
                  id="contactEmail"
                  type="email"
                  value={formData.contactEmail}
                  onChange={(e) => handleChange('contactEmail', e.target.value)}
                  placeholder="contact@example.com"
                  className={inputClass('contactEmail')}
                />
                {errors.contactEmail && <p className="mt-1 text-sm text-red-400">{errors.contactEmail}</p>}
              </div>

              <div>
                <label htmlFor="contactPhone" className="label-dark">
                  Contact Phone
                </label>
                <input
                  id="contactPhone"
                  type="tel"
                  value={formData.contactPhone}
                  onChange={(e) => handleChange('contactPhone', e.target.value)}
                  placeholder="+1 (555) 123-4567"
                  className={inputClass('contactPhone')}
                />
                {errors.contactPhone && <p className="mt-1 text-sm text-red-400">{errors.contactPhone}</p>}
              </div>
            </div>

            <div className="flex items-center gap-3 border-t border-white/[0.06] pt-5">
              <button type="button" onClick={() => navigate('/businesses')} className="btn-secondary">
                Cancel
              </button>
              <button type="submit" disabled={loading} className="btn-primary">
                <Save size={18} />
                {loading ? 'Saving…' : isEditMode ? 'Update Business' : 'Create Business'}
              </button>
            </div>
          </form>
        </Card>
      )}
    </div>
  );
}
