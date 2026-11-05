import { Outlet } from 'react-router-dom';
import { motion } from 'framer-motion';
import { ShieldCheck } from 'lucide-react';

export function AuthLayout() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4">
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25 }}
        className="w-full max-w-sm"
      >
        <div className="mb-8 flex flex-col items-center gap-2 text-center">
          <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-primary text-primary-foreground">
            <ShieldCheck className="h-6 w-6" />
          </div>
          <h1 className="text-xl font-bold tracking-tight">ASSQP</h1>
          <p className="text-sm text-muted-foreground">
            AI-Driven Software Quality Assurance &amp; Security Platform
          </p>
        </div>
        <div className="rounded-lg border border-border bg-card p-6 shadow-sm">
          <Outlet />
        </div>
      </motion.div>
    </div>
  );
}
