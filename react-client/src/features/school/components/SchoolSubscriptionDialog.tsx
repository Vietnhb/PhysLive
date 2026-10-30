import { useEffect, useState } from "react";
import { Badge, Button, Dialog, Flex } from "@radix-ui/themes";
import { schoolSubscription, type SchoolSubscription } from "../api/schoolApi";
import { apiMessage } from "../../../shared/lib/apiError";
import { CardLoadingSkeleton } from "../../../shared/ui/LoadingSkeleton";
import modal from "../../../shared/ui/ModalScroll.module.css";
import s from "./SchoolSubscriptionDialog.module.css";
import { SUBSCRIPTION_STATUS } from "../model/schoolSubscription";

type Props = { schoolId: string | null; onClose: () => void };
const dateLabel = (date?: string | null) => date ? date.split("-").reverse().join("/") : "Chưa thiết lập";
const quotaLabel = (used: number, quota?: number | null) => `${used.toLocaleString("vi-VN")} / ${quota == null ? "Không giới hạn" : quota.toLocaleString("vi-VN")}`;

export default function SchoolSubscriptionDialog({ schoolId, onClose }: Readonly<Props>) {
  const [data, setData] = useState<SchoolSubscription | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    if (!schoolId) return;
    let active = true;
    void schoolSubscription(schoolId).then(result => { if (active) setData(result); })
      .catch(err => { if (active) setError(apiMessage(err, "Không thể tải thông tin gói của trường.")); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [schoolId, revision]);
  return <Dialog.Root open={Boolean(schoolId)} onOpenChange={open => { if (!open) onClose(); }}>
    <Dialog.Content className={modal.dialog} maxWidth="720px">
      <div className={modal.body}>
        <Dialog.Title>Gói sử dụng của trường</Dialog.Title>
        <Dialog.Description size="2" mb="5">Thông tin gói hiện tại, tài khoản quản lý và hạn mức dùng chung của trường.</Dialog.Description>
        {loading ? <CardLoadingSkeleton cards={2} label="Đang tải thông tin gói…" /> : error ? <div role="alert"><p>{error}</p><Button variant="soft" onClick={() => { setLoading(true); setError(""); setRevision(value => value + 1); }}>Thử lại</Button></div> : data && <>
          <h3 className={s.schoolName}>{data.school.name}</h3>
          <dl className={s.details}>
            <div><dt>Gói hiện tại</dt><dd>{data.planName || data.school.planCode || "Chưa gắn gói"}</dd></div>
            <div><dt>Mã gói</dt><dd>{data.school.planCode || "—"}</dd></div>
            <div><dt>Trạng thái gói</dt><dd><Badge color={SUBSCRIPTION_STATUS[data.school.subscriptionStatus ?? "UNKNOWN"].color}>{SUBSCRIPTION_STATUS[data.school.subscriptionStatus ?? "UNKNOWN"].label}</Badge></dd></div>
            <div><dt>Ngày bắt đầu</dt><dd>{dateLabel(data.school.licenseStart)}</dd></div>
            <div><dt>Ngày hết hạn</dt><dd>{dateLabel(data.school.licenseEnd)}</dd></div>
            <div><dt>Giá đã ghi nhận / năm</dt><dd>{data.school.annualPriceVnd == null ? "Chưa ghi nhận" : `${data.school.annualPriceVnd.toLocaleString("vi-VN")} ₫`}</dd></div>
            <div><dt>Gói tiếp theo</dt><dd>{data.school.nextPlanCode || "Chưa có"}</dd></div>
            <div><dt>Học sinh đang hoạt động / hạn mức</dt><dd>{quotaLabel(data.studentsUsed, data.school.studentQuota)}</dd></div>
            <div><dt>Token AI tháng {data.usageMonth.slice(0, 7).split("-").reverse().join("/")} / hạn mức</dt><dd>{quotaLabel(data.tokensUsed, data.school.monthlyTokenQuota)}</dd></div>
          </dl>
          <h3 className={s.schoolName}>Tài khoản quản lý trường</h3>
          {data.managers.length === 0 ? <p>Chưa có tài khoản quản lý trường.</p> : <ul className={s.accounts}>{data.managers.map(manager => <li key={manager.id}><div><strong>{manager.fullName}</strong><span>{manager.email}</span></div><Badge color={manager.active ? "green" : "gray"}>{manager.active ? "Hoạt động" : "Tạm khóa"}</Badge></li>)}</ul>}
        </>}
        <Flex justify="end" mt="5"><Dialog.Close><Button variant="soft" color="gray">Đóng</Button></Dialog.Close></Flex>
      </div>
    </Dialog.Content>
  </Dialog.Root>;
}
