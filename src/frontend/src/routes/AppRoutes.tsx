import { Routes, Route, Navigate } from 'react-router-dom';
import { AuthLayout } from '@/layouts/AuthLayout';
import { DashboardLayout } from '@/layouts/DashboardLayout';
import { ProjectLayout } from '@/layouts/ProjectLayout';
import { ProtectedRoute } from './ProtectedRoute';
import { RoleRoute } from './RoleRoute';
import { ROUTES } from '@/constants/routes';

import Login from '@/pages/auth/Login';
import Register from '@/pages/auth/Register';
import ForgotPassword from '@/pages/auth/ForgotPassword';

import Dashboard from '@/pages/dashboard/Dashboard';

import Projects from '@/pages/projects/Projects';
import CreateProject from '@/pages/projects/CreateProject';
import ProjectDetails from '@/pages/projects/ProjectDetails';
import EditProject from '@/pages/projects/EditProject';

import QualityAnalysis from '@/pages/quality/QualityAnalysis';
import SecurityAnalysis from '@/pages/security/SecurityAnalysis';
import VulnerabilityDetails from '@/pages/security/VulnerabilityDetails';
import TestCases from '@/pages/testing/TestCases';
import CreateTestCase from '@/pages/testing/CreateTestCase';
import TestExecution from '@/pages/testing/TestExecution';
import ExecutionHistory from '@/pages/testing/ExecutionHistory';
import AdaptiveTesting from '@/pages/adaptive/AdaptiveTesting';
import RiskAssessment from '@/pages/risk/RiskAssessment';
import MlOps from '@/pages/mlops/MlOps';
import AiAnalysis from '@/pages/ai/AiAnalysis';
import Reports from '@/pages/reports/Reports';
import CiCd from '@/pages/cicd/CiCd';

import Notifications from '@/pages/notifications/Notifications';
import Users from '@/pages/users/Users';
import Profile from '@/pages/profile/Profile';

import NotFound from '@/pages/errors/NotFound';
import Unauthorized from '@/pages/errors/Unauthorized';

export function AppRoutes() {
  return (
    <Routes>
      {/* Public */}
      <Route element={<AuthLayout />}>
        <Route path={ROUTES.LOGIN} element={<Login />} />
        <Route path={ROUTES.REGISTER} element={<Register />} />
        <Route path={ROUTES.FORGOT_PASSWORD} element={<ForgotPassword />} />
      </Route>

      <Route path={ROUTES.UNAUTHORIZED} element={<Unauthorized />} />

      {/* Protected */}
      <Route element={<ProtectedRoute />}>
        <Route element={<DashboardLayout />}>
          <Route path="/" element={<Navigate to={ROUTES.DASHBOARD} replace />} />
          <Route path={ROUTES.DASHBOARD} element={<Dashboard />} />

          <Route path={ROUTES.PROJECTS} element={<Projects />} />
          <Route path={ROUTES.PROJECT_NEW} element={<CreateProject />} />
          <Route path="/projects/:id/edit" element={<EditProject />} />

          <Route path="/vulnerabilities/:id" element={<VulnerabilityDetails />} />

          <Route path="/projects/:id" element={<ProjectLayout />}>
            <Route index element={<ProjectDetails />} />
            <Route path="quality" element={<QualityAnalysis />} />
            <Route path="security" element={<SecurityAnalysis />} />
            <Route path="tests" element={<TestCases />} />
            <Route path="tests/new" element={<CreateTestCase />} />
            <Route path="tests/:testId/executions" element={<TestExecution />} />
            <Route path="executions" element={<ExecutionHistory />} />
            <Route path="adaptive-testing" element={<AdaptiveTesting />} />
            <Route path="risk" element={<RiskAssessment />} />
            <Route path="mlops" element={<MlOps />} />
            <Route path="ai" element={<AiAnalysis />} />
            <Route path="reports" element={<Reports />} />
            <Route path="cicd" element={<CiCd />} />
          </Route>

          <Route path="/notifications" element={<Notifications />} />
          <Route path={ROUTES.PROFILE} element={<Profile />} />

          <Route element={<RoleRoute allow={['ADMIN']} />}>
            <Route path={ROUTES.USERS} element={<Users />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}
