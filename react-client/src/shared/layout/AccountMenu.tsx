import { useEffect, useRef, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { AnimatePresence, motion, useReducedMotion } from "motion/react";
import LearningIcon from "../ui/LearningIcon";
import { useSessionStore } from "../auth/sessionStore";
import { getRoleLabel } from "../auth/roles";
import { clearToken } from "../lib/token";
import { ThemeSegmented } from "../theme/ThemeToggle";
import "./AccountMenu.css";

type AccountMenuVariant = "navbar" | "workspace" | "sidebar";

export default function AccountMenu({ variant }: Readonly<{ variant: AccountMenuVariant }>) {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const reduced = useReducedMotion();
  const user = useSessionStore(state => state.user);
  const [openPath, setOpenPath] = useState<string | null>(null);
  const [appearanceOpen, setAppearanceOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);
  const open = openPath === pathname;
  const initials = user?.fullName?.trim().slice(0, 1).toUpperCase() || "U";

  useEffect(() => {
    if (!open) return;
    const closeOnOutsidePress = (event: PointerEvent) => {
      if (!rootRef.current?.contains(event.target as Node)) setOpenPath(null);
    };
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpenPath(null);
    };
    document.addEventListener("pointerdown", closeOnOutsidePress);
    document.addEventListener("keydown", closeOnEscape);
    return () => {
      document.removeEventListener("pointerdown", closeOnOutsidePress);
      document.removeEventListener("keydown", closeOnEscape);
    };
  }, [open]);

  if (!user) return null;

  const close = () => setOpenPath(null);
  const logout = () => {
    clearToken();
    useSessionStore.getState().setUser(null);
    navigate("/login", { replace: true });
  };

  return (
    <div ref={rootRef} className={`account-menu account-menu--${variant}`}>
      <button
        type="button"
        className="account-menu__trigger"
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label={`Tài khoản ${user.fullName ?? ""}`}
        title={user.fullName ?? "Tài khoản"}
        onClick={() => setOpenPath(open ? null : pathname)}
      >
        <span className="account-menu__avatar" aria-hidden="true">
          {user.avatarUrl ? <img src={user.avatarUrl} alt="" /> : initials}
        </span>
        {variant !== "workspace" && (
          <span className="account-menu__trigger-copy">
            <strong>{user.fullName || "Tài khoản"}</strong>
            {variant === "sidebar" && <small>{getRoleLabel(user.role)}</small>}
          </span>
        )}
        {variant === "sidebar" && <LearningIcon name={open ? "chevronDown" : "chevronRight"} />}
      </button>

      <AnimatePresence>
        {open && (
          <motion.div
            role="menu"
            aria-label="Tài khoản và cài đặt"
            className="account-menu__panel"
            initial={reduced ? { opacity: 0 } : { opacity: 0, y: -6, scale: 0.97 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={reduced ? { opacity: 0 } : { opacity: 0, y: -4, scale: 0.98 }}
            transition={{ duration: 0.16, ease: [0.22, 1, 0.36, 1] }}
          >
            <div className="account-menu__identity" title={user.email}>
              <span className="account-menu__avatar" aria-hidden="true">
                {user.avatarUrl ? <img src={user.avatarUrl} alt="" /> : initials}
              </span>
              <span className="account-menu__identity-copy">
                <strong>{user.fullName || "Tài khoản"}</strong>
                <small>{getRoleLabel(user.role)}</small>
              </span>
            </div>

            <Link role="menuitem" className="account-menu__profile-link" to="/profile#profile-details" onClick={close}>
              <span className="account-menu__item-icon"><LearningIcon name="users" /></span>
              <span>Hồ sơ cá nhân</span>
              <LearningIcon name="chevronRight" />
            </Link>

            <div className="account-menu__divider" />

            <Link role="menuitem" className="account-menu__item" to="/profile#profile-security" onClick={close}>
              <span className="account-menu__item-icon"><LearningIcon name="settings" /></span>
              <span>Cài đặt và bảo mật</span>
              <LearningIcon name="chevronRight" />
            </Link>

            <button
              type="button"
              role="menuitem"
              className="account-menu__item"
              aria-expanded={appearanceOpen}
              onClick={() => setAppearanceOpen(value => !value)}
            >
              <span className="account-menu__item-icon"><LearningIcon name="moon" /></span>
              <span>Giao diện và trợ năng</span>
              <LearningIcon name={appearanceOpen ? "chevronDown" : "chevronRight"} />
            </button>
            {appearanceOpen && <div className="account-menu__appearance"><ThemeSegmented className="account-menu__theme-control" /></div>}

            <div className="account-menu__divider" />

            <button type="button" role="menuitem" className="account-menu__item account-menu__item--logout" onClick={logout}>
              <span className="account-menu__item-icon"><LearningIcon name="logout" /></span>
              <span>Đăng xuất</span>
            </button>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
