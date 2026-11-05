import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';
import { ROUTES } from '@/constants/routes';
import type { Role } from '@/types/enums';

// UX-only gate. The backend (method-level @PreAuthorize / project ownership
// checks) remains the actual authorization authority for every request.
export function RoleRoute({ allow }: { allow: Role[] }) {
  const { user } = useAuth();
  if (!user || !allow.includes(user.role)) {
    return <Navigate to={ROUTES.UNAUTHORIZED} replace />;
  }
  return <Outlet />;
}
