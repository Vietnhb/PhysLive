import { Routes, Route } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { AppLayout } from "./components/layout/AppLayout";
import { RouteGuard } from "./components/layout/RouteGuard";

// Pages
import { Dashboard } from "./pages/Dashboard";
import { ProblemSubmitPage } from "./pages/teacher/ProblemSubmitPage";
import { TeacherAssignmentPage } from "./pages/teacher/TeacherAssignmentPage";
import { SimulationPlayerPage } from "./pages/player/SimulationPlayerPage";
import { LibraryPage } from "./pages/library/LibraryPage";
import { StudentAssignmentsPage } from "./pages/student/StudentAssignmentsPage";
import { SchemaReviewPage } from "./pages/reviewer/SchemaReviewPage";
import { ProfilePage } from "./pages/profile/ProfilePage";
import Admin from "./pages/Admin";
import Login from "./pages/Login";
import Signup from "./pages/Signup";
import { Home } from "./pages/Home";

function App() {
  return (
    <AuthProvider>
      <Routes>
        {/* Auth routes ngoài layout */}
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<Signup />} />

        {/* Main App Layout */}
        <Route path="/" element={<AppLayout />}>
          {/* Landing / Trang chủ PhysLive */}
          <Route index element={<Home />} />
          <Route path="home" element={<Home />} />
          <Route path="dashboard" element={<Dashboard />} />

          {/* Teacher flow (H03 + H04) */}
          <Route
            path="teacher/submit"
            element={
              <RouteGuard allowedRoles={["TEACHER", "ADMIN"]}>
                <ProblemSubmitPage />
              </RouteGuard>
            }
          />

          {/* Interactive Player (H05) */}
          <Route path="player" element={<SimulationPlayerPage />} />

          {/* Library & Taxonomy (H06 + H10) */}
          <Route path="library" element={<LibraryPage />} />
          <Route
            path="assignments"
            element={
              <RouteGuard allowedRoles={["TEACHER", "ADMIN"]}>
                <TeacherAssignmentPage />
              </RouteGuard>
            }
          />

          {/* Student flow (H07) */}
          <Route
            path="student/assignments"
            element={
              <RouteGuard allowedRoles={["STUDENT", "ADMIN", "TEACHER"]}>
                <StudentAssignmentsPage />
              </RouteGuard>
            }
          />

          {/* Reviewer flow */}
          <Route
            path="reviewer/schemas"
            element={
              <RouteGuard allowedRoles={["REVIEWER", "ADMIN"]}>
                <SchemaReviewPage />
              </RouteGuard>
            }
          />

          {/* Profile & Settings (H02) */}
          <Route
            path="profile"
            element={
              <RouteGuard>
                <ProfilePage />
              </RouteGuard>
            }
          />

          {/* Admin flow */}
          <Route
            path="admin"
            element={
              <RouteGuard allowedRoles={["ADMIN"]}>
                <Admin />
              </RouteGuard>
            }
          />
        </Route>
      </Routes>
    </AuthProvider>
  );
}

export default App;