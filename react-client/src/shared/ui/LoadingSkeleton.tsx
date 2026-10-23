import { Skeleton, Table } from "@radix-ui/themes";
import type { CSSProperties } from "react";
import styles from "./LoadingSkeleton.module.css";

type LoadingProps = Readonly<{ label?: string }>;

function LoadingStatus({ label = "Đang tải dữ liệu…", children }: LoadingProps & Readonly<{ children: React.ReactNode }>) {
  return <div className={styles.status} role="status" aria-live="polite" aria-busy="true">
    <span className={styles.srOnly}>{label}</span>
    <div aria-hidden="true">{children}</div>
  </div>;
}

export function TableLoadingSkeleton({
  headers,
  rows = 6,
  label = "Đang tải danh sách…",
}: LoadingProps & Readonly<{ headers: readonly string[]; rows?: number }>) {
  return <LoadingStatus label={label}>
    <div className={styles.tableWrap}>
      <Table.Root size="2" variant="surface" className={styles.table}>
        <Table.Header><Table.Row>{headers.map(header => <Table.ColumnHeaderCell key={header}>{header}</Table.ColumnHeaderCell>)}</Table.Row></Table.Header>
        <Table.Body>{Array.from({ length: rows }, (_, row) => <Table.Row key={row}>
          {headers.map((header, column) => <Table.Cell key={header}>
            <Skeleton className={styles.cellSkeleton} width={column === 0 ? "120px" : column === headers.length - 1 ? "64px" : "82px"} height="18px" />
          </Table.Cell>)}
        </Table.Row>)}</Table.Body>
      </Table.Root>
    </div>
  </LoadingStatus>;
}

export function DashboardLoadingSkeleton({ cards = 4, label = "Đang tải tổng quan…" }: LoadingProps & Readonly<{ cards?: number }>) {
  const columns = Math.min(cards, 5);
  return <LoadingStatus label={label}>
    <div className={styles.dashboard}>
      <div className={styles.stats} style={{ "--skeleton-columns": columns } as CSSProperties}>
        {Array.from({ length: cards }, (_, index) => <div className={styles.stat} key={index}>
          <Skeleton width="42%" height="15px" />
          <Skeleton width="34%" height="32px" />
          <Skeleton width="58%" height="12px" />
        </div>)}
      </div>
      <div className={styles.section}>
        <Skeleton width="180px" height="22px" />
        <div className={styles.sectionLines}>{Array.from({ length: 5 }, (_, index) => <Skeleton key={index} width={`${92 - index * 5}%`} height="18px" />)}</div>
      </div>
    </div>
  </LoadingStatus>;
}

export function CardLoadingSkeleton({ cards = 3, label = "Đang tải nội dung…" }: LoadingProps & Readonly<{ cards?: number }>) {
  const columns = Math.min(cards, 4);
  return <LoadingStatus label={label}>
    <div className={styles.cards} style={{ "--skeleton-columns": columns } as CSSProperties}>
      {Array.from({ length: cards }, (_, index) => <div className={styles.card} key={index}>
        <Skeleton width="62%" height="20px" />
        <Skeleton width="88%" height="14px" />
        <Skeleton width="74%" height="14px" />
        <Skeleton width="96px" height="30px" />
      </div>)}
    </div>
  </LoadingStatus>;
}
