import "../../styles/operations.css";

import { CardLoadingSkeleton, TableLoadingSkeleton } from "./LoadingSkeleton";

export function LoadState({ loading, error, refresh, headers }: Readonly<{ loading: boolean; error?: string; refresh: () => void; headers?: readonly string[] }>) {
  return <>{loading && (headers
    ? <TableLoadingSkeleton headers={headers} label="Đang tải dữ liệu…" />
    : <CardLoadingSkeleton cards={3} label="Đang tải dữ liệu…" />)}{error && <div className="ops-alert" role="alert">{error} <button className="secondary" onClick={refresh}>Thử lại</button></div>}</>;
}

