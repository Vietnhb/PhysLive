import "../../styles/operations.css";

export function LoadState({ loading, error, refresh }: Readonly<{ loading: boolean; error?: string; refresh: () => void }>) {
  return <>{loading && <output className="ops-loading">Đang tải dữ liệu…</output>}{error && <div className="ops-alert" role="alert">{error} <button className="secondary" onClick={refresh}>Thử lại</button></div>}</>;
}

