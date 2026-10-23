import { Card, Skeleton } from "@radix-ui/themes";
import { TableLoadingSkeleton } from "../../../shared/ui/LoadingSkeleton";
import s from "./SchoolLoading.module.css";

export function ClassTableLoading({ headers, rows = 6 }: Readonly<{ headers: string[]; rows?: number }>) {
  return <TableLoadingSkeleton headers={headers} rows={rows} label="Đang tải danh sách lớp học…" />;
}

export function ClassDetailLoading() {
  return <Card asChild size="3"><section className={`admin-panel ${s.loadingRegion}`} role="status" aria-live="polite" aria-busy="true">
    <span className={s.srOnly}>Đang tải chi tiết lớp học…</span>
    <div className={s.detailHeading} aria-hidden="true"><Skeleton width="180px" height="24px" /><Skeleton width="260px" height="16px" /></div>
    <div className={s.cards} aria-hidden="true">{Array.from({ length: 4 }, (_, index) => <div key={index}><Skeleton width="72px" height="14px" /><Skeleton width="48px" height="26px" /></div>)}</div>
    <div className={s.section} aria-hidden="true"><Skeleton width="120px" height="20px" /><div className={s.people}>{Array.from({ length: 2 }, (_, index) => <div key={index}><Skeleton width="34px" height="34px" /><span><Skeleton width="150px" height="16px" /><Skeleton width="210px" height="13px" /></span></div>)}</div></div>
    <div className={s.section} aria-hidden="true"><Skeleton width="110px" height="20px" /><ClassTableLoading headers={["STT", "Họ và tên", "Email"]} rows={5} /></div>
  </section></Card>;
}
