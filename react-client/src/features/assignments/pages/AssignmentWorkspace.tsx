import { lazy, Suspense } from "react";
import { Link } from "react-router-dom";
import { Theme } from "@radix-ui/themes";
import { useEffectiveTheme } from "../../../shared/theme/themeStore";
import LearningHeader from "../../../shared/layout/LearningHeader";
import { useSessionStore } from "../../../shared/auth/sessionStore";
import { canTeach } from "../../../shared/auth/permissions";
import "../../simulation/styles/learning.css";

const Assignments = lazy(() => import("./Assignments"));

export default function AssignmentWorkspace() {
  const user = useSessionStore((state) => state.user);
  const appearance = useEffectiveTheme();
  return (
    <div className="learning-app">
      <LearningHeader />
      {canTeach(user) ? (
        <Suspense fallback={<main className="route-loading" aria-busy="true" />}>
          {/* the assignment studio is built with Radix Themes; the rest of the learning shell is plain CSS */}
          <Theme appearance={appearance} accentColor="indigo" grayColor="slate" radius="large" scaling="100%" hasBackground={false} className="assignment-theme">
            <Assignments workspaceLayout />
          </Theme>
        </Suspense>
      ) : (
        <main className="learn-empty">
          <h1>Giao bài cho học sinh</h1>
          <p>Đăng nhập bằng tài khoản giáo viên để tạo bài giao.</p>
          <Link to="/login">Đăng nhập</Link>
        </main>
      )}
    </div>
  );
}