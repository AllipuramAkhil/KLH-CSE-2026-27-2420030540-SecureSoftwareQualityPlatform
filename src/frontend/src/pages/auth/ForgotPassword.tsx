import { Link } from 'react-router-dom';
import { MailQuestion } from 'lucide-react';
import { ROUTES } from '@/constants/routes';

// The current ASSQP backend does not expose a password-reset endpoint.
// This page is a placeholder so the route exists without silently faking
// a working reset flow — it clearly signals that the feature is not yet
// wired to a real API and points the user back to sign in.
export default function ForgotPassword() {
  return (
    <div className="text-center">
      <MailQuestion className="mx-auto mb-3 h-8 w-8 text-muted-foreground" />
      <h2 className="mb-2 text-base font-semibold">Password reset not available</h2>
      <p className="mb-5 text-sm text-muted-foreground">
        The ASSQP backend does not currently expose a password-reset endpoint. Please contact an administrator to
        reset your password.
      </p>
      <Link to={ROUTES.LOGIN} className="text-sm font-medium text-primary hover:underline">
        Back to sign in
      </Link>
    </div>
  );
}
