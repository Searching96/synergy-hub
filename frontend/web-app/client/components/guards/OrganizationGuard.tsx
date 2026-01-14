import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { organizationService } from '@/services/organization.service';
import { Loader2 } from 'lucide-react';

interface OrganizationGuardProps {
  children: React.ReactNode;
}

export function OrganizationGuard({ children }: OrganizationGuardProps) {
  const [isLoading, setIsLoading] = useState(true);
  const [hasOrganization, setHasOrganization] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    checkOrganization();
  }, []);

  const checkOrganization = async () => {
    try {
      // Check if user has an organization
      const response = await organizationService.checkUserOrganization();
      
      if (response.success && response.data?.organizationId) {
        setHasOrganization(true);
      } else {
        // No organization, redirect to welcome page
        navigate('/welcome', { replace: true });
      }
    } catch (error) {
      console.error('Failed to check organization:', error);
      // If API call fails, redirect to welcome (safe default)
      navigate('/welcome', { replace: true });
    } finally {
      setIsLoading(false);
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <Loader2 className="h-8 w-8 animate-spin text-blue-600 mx-auto mb-4" />
          <p className="text-gray-600">Checking organization...</p>
        </div>
      </div>
    );
  }

  if (!hasOrganization) {
    return null; // Will redirect to welcome
  }

  return <>{children}</>;
}