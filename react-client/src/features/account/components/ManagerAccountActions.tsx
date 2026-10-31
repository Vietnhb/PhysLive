import { useRef, useState, type FormEvent } from "react";
import { AlertDialog, Button, Callout, Dialog, Flex, Text, TextField } from "@radix-ui/themes";
import { PlusIcon } from "@radix-ui/react-icons";
import type { User } from "../../../shared/auth/types";
import { useSessionStore } from "../../../shared/auth/sessionStore";
import { apiMessage } from "../../../shared/lib/apiError";
import { FormField } from "../../../shared/ui/ManagementUI";
import modal from "../../../shared/ui/ModalScroll.module.css";
import { createManagedUser, setManagedUserActive, updateManagedUser } from "../api/userApi";
import { AccountProfileFields, ResetManagedPasswordDialog } from "./ManagedAccountFields";
import { accountDetails, accountDetailsPayload, blankAccountDetails } from "../model/accountDetailsModel";

export default function ManagerAccountActions({ user, onSaved }: Readonly<{ user?: User; onSaved: (user: User, message: string) => void }>) {
  const actorRole = useSessionStore(state => state.user?.role);
  const [mode, setMode] = useState<"form" | "status" | null>(null);
  const [resetUser, setResetUser] = useState<User | null>(null);
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [profile, setProfile] = useState(blankAccountDetails);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const lock = useRef(false);
  if (actorRole !== "ADMIN" || (user && user.role !== "MANAGER")) return null;

  const startForm = () => {
    setName(user?.fullName ?? ""); setEmail(user?.email ?? ""); setPassword("");
    setProfile(user ? accountDetails(user) : blankAccountDetails); setError(""); setMode("form");
  };
  const save = async (event: FormEvent) => {
    event.preventDefault();
    if (lock.current || !name.trim()) return;
    lock.current = true; setBusy(true); setError("");
    try {
      const payload = { fullName: name.trim(), role: "MANAGER", ...accountDetailsPayload(profile, "MANAGER") };
      const saved = user ? await updateManagedUser(user.id, payload)
        : await createManagedUser({ ...payload, email: email.trim(), password });
      setMode(null); setPassword(""); onSaved(saved, user ? "Đã cập nhật tài khoản Manager." : "Đã tạo tài khoản Manager.");
    } catch (cause) { setError(apiMessage(cause, "Không thể lưu tài khoản Manager.")); }
    finally { lock.current = false; setBusy(false); }
  };
  const toggle = async () => {
    if (!user || lock.current) return;
    lock.current = true; setBusy(true); setError("");
    try {
      const saved = await setManagedUserActive(user.id, user.active === false);
      setMode(null); onSaved(saved, saved.active === false ? "Đã tạm khóa tài khoản Manager." : "Đã mở khóa tài khoản Manager.");
    } catch (cause) { setError(apiMessage(cause, "Không thể thay đổi trạng thái tài khoản Manager.")); }
    finally { lock.current = false; setBusy(false); }
  };

  return <>
    {user ? <Flex gap="3" wrap="wrap" mt="4" aria-label="Quản lý tài khoản Manager">
      <Button variant="surface" disabled={busy} onClick={startForm}>Sửa hồ sơ</Button>
      <Button variant="surface" disabled={busy} onClick={() => setResetUser(user)}>Đặt lại mật khẩu</Button>
      <Button variant="soft" color={user.active === false ? "green" : "red"} disabled={busy} onClick={() => { setError(""); setMode("status"); }}>{user.active === false ? "Mở khóa" : "Tạm khóa"}</Button>
    </Flex> : <Button disabled={busy} onClick={startForm}><PlusIcon />Tạo tài khoản Manager</Button>}
    <Dialog.Root open={mode === "form"} onOpenChange={open => { if (!open && !lock.current) setMode(null); }}>
      <Dialog.Content className={modal.dialog} maxWidth="620px" onEscapeKeyDown={event => { if (lock.current) event.preventDefault(); }} onInteractOutside={event => { if (lock.current) event.preventDefault(); }}><div className={modal.body}>
        <Dialog.Title>{user ? "Cập nhật tài khoản Manager" : "Tạo tài khoản Manager"}</Dialog.Title>
        <Dialog.Description size="2" mb="4">Tài khoản quản lý vận hành nền tảng, không thuộc trường học.</Dialog.Description>
        {error && <Callout.Root color="red" role="alert" mb="4"><Callout.Text>{error}</Callout.Text></Callout.Root>}
        <form className="admin-form-grid" onSubmit={save}>
          <FormField label="Email" htmlFor="manager-account-email"><TextField.Root id="manager-account-email" type="email" required size="3" disabled={busy || !!user} value={email} onChange={event => setEmail(event.target.value)} /></FormField>
          <FormField label="Họ và tên" htmlFor="manager-account-name"><TextField.Root id="manager-account-name" required maxLength={120} size="3" disabled={busy} value={name} onChange={event => setName(event.target.value)} /></FormField>
          <AccountProfileFields idPrefix="manager-account" value={profile} name={name} disabled={busy} onChange={value => setProfile(current => ({ ...current, ...value }))} />
          {!user && <><FormField label="Mật khẩu ban đầu" htmlFor="manager-account-password"><TextField.Root id="manager-account-password" type="password" autoComplete="new-password" minLength={8} maxLength={120} required size="3" disabled={busy} value={password} onChange={event => setPassword(event.target.value)} /></FormField><Text size="2" color="gray" style={{ gridColumn: "1 / -1" }}>Manager phải đổi mật khẩu khi đăng nhập lần đầu.</Text></>}
          <div className="admin-form-actions"><Button type="button" variant="soft" color="gray" disabled={busy} onClick={() => setMode(null)}>Hủy</Button><Button type="submit" loading={busy} disabled={busy || !name.trim()}>Lưu tài khoản</Button></div>
        </form>
      </div></Dialog.Content>
    </Dialog.Root>
    <AlertDialog.Root open={mode === "status"} onOpenChange={open => { if (!open && !lock.current) setMode(null); }}><AlertDialog.Content maxWidth="460px" onEscapeKeyDown={event => { if (lock.current) event.preventDefault(); }}>
      <AlertDialog.Title>{user?.active === false ? "Mở khóa tài khoản Manager?" : "Tạm khóa tài khoản Manager?"}</AlertDialog.Title>
      <AlertDialog.Description size="2">{user?.email} — {user?.active === false ? "Tài khoản sẽ được phép đăng nhập trở lại." : "Tài khoản sẽ không thể đăng nhập cho đến khi được mở khóa."}</AlertDialog.Description>
      {error && <Callout.Root color="red" role="alert" mt="4"><Callout.Text>{error}</Callout.Text></Callout.Root>}
      <Flex gap="3" justify="end" mt="4"><AlertDialog.Cancel><Button variant="soft" color="gray" disabled={busy}>Hủy</Button></AlertDialog.Cancel><Button loading={busy} disabled={busy} color={user?.active === false ? "green" : "red"} onClick={() => void toggle()}>{user?.active === false ? "Mở khóa" : "Tạm khóa"}</Button></Flex>
    </AlertDialog.Content></AlertDialog.Root>
    <ResetManagedPasswordDialog user={resetUser} onClose={() => setResetUser(null)} onUpdated={updated => { onSaved(updated, `Đã đặt lại mật khẩu cho ${updated.email}.`); }} />
  </>;
}
