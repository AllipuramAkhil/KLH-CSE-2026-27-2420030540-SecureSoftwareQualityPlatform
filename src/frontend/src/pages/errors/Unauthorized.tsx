import { Link } from 'react-router-dom';
import { ShieldOff } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { ROUTES } from '@/constants/routes';

export default function Unauthorized() {
  return (
    <div className="flex h-screen flex-col items-center justify-center gap-3 text-center px-4">
      <ShieldOff className="h-12 w-12 text-danger" />
      <h1 className="text-2xl font-bold">Access restricted</h1>
      <p className="max-w-sm text-sm text-muted-foreground">
        Your role doesn't have access to this section of ASSQP.
      </p>
      <Link to={ROUTES.DASHBOARD}>
        <Button className="mt-2">Back to dashboard</Button>
      </Link>
    </div>
  );
}
