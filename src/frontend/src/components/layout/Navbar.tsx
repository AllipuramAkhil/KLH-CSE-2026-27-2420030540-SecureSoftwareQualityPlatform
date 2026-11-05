import { useState } from 'react';
import { Menu, LogOut, User as UserIcon } from 'lucide-react';
import { AnimatePresence, motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { ThemeToggle } from './ThemeToggle';
import { NotificationBell } from './NotificationBell';
import { useAuth } from '@/hooks/useAuth';
import { initials } from '@/utils/formatters';
import { ROUTES } from '@/constants/routes';

export function Navbar({ onMenuClick }: { onMenuClick: () => void }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);

  return (
    <header className="flex h-14 shrink-0 items-center justify-between border-b border-border bg-card px-4">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon" className="md:hidden" onClick={onMenuClick} aria-label="Open menu">
          <Menu className="h-5 w-5" />
        </Button>
      </div>

      <div className="flex items-center gap-2">
        <ThemeToggle />
        <NotificationBell />

        <div className="relative ml-1">
          <button
            onClick={() => setMenuOpen((o) => !o)}
            className="flex items-center gap-2 rounded-md p-1 pr-2 hover:bg-muted"
          >
            <div className="flex h-7 w-7 items-center justify-center rounded-full bg-primary text-xs font-bold text-primary-foreground">
              {user ? initials(user.fullName) : '?'}
            </div>
            <span className="hidden text-sm font-medium sm:inline">{user?.fullName}</span>
          </button>

          <AnimatePresence>
            {menuOpen && (
              <>
                <div className="fixed inset-0 z-40" onClick={() => setMenuOpen(false)} />
                <motion.div
                  initial={{ opacity: 0, y: -8 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -8 }}
                  transition={{ duration: 0.15 }}
                  className="absolute right-0 z-50 mt-2 w-52 rounded-lg border border-border bg-card p-1 shadow-xl"
                >
                  <div className="px-2 py-1.5 text-xs text-muted-foreground truncate">{user?.email}</div>
                  <button
                    onClick={() => {
                      setMenuOpen(false);
                      navigate(ROUTES.PROFILE);
                    }}
                    className="flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-sm hover:bg-muted"
                  >
                    <UserIcon className="h-4 w-4" /> Profile
                  </button>
                  <button
                    onClick={() => {
                      setMenuOpen(false);
                      logout();
                      navigate(ROUTES.LOGIN);
                    }}
                    className="flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-sm text-danger hover:bg-danger/10"
                  >
                    <LogOut className="h-4 w-4" /> Log out
                  </button>
                </motion.div>
              </>
            )}
          </AnimatePresence>
        </div>
      </div>
    </header>
  );
}
