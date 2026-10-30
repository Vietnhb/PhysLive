import { useEffect, useRef, useState } from "react";
import LearningIcon from "../../../shared/ui/LearningIcon";
import { Badge, Select, Table } from "@radix-ui/themes";
import { validationRuns, type ValidationRun } from "../api/operationsApi";
import { apiMessage } from "../../../shared/lib/apiError";
import { PageHeader, Panel, Loading, ErrorNotice, Button } from "../../../shared/ui/ManagementUI";
import tableScroll from "../../../shared/ui/TableScroll.module.css";

export function ValidationView() {
  const [items, setItems] = useState<ValidationRun[]>([]); const [loading, setLoading] = useState(true); const [error, setError] = useState(""); const load = async () => { setLoading(true); try { setItems(await validationRuns()); setError(""); } catch (e) { setError(apiMessage(e, "Không thể tải các lần kiểm định.")); } finally { setLoading(false); } }; useEffect(() => { void load(); }, []);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const tableRef = useRef<HTMLDivElement>(null);
  const pageCount = Math.max(1, Math.ceil(items.length / pageSize));
  const currentPage = Math.min(page, pageCount);
  const pageStart = (currentPage - 1) * pageSize;
  const visibleItems = items.slice(pageStart, pageStart + pageSize);
  const changePage = (next: number) => {
    setPage(Math.max(1, Math.min(next, pageCount)));
    tableRef.current?.scrollTo({ top: 0 });
  };

  return <div className="admin-content">
    <PageHeader title="Kiểm định mô phỏng" description="Nhật ký kết quả kiểm tra do hệ thống ghi nhận khi chạy mô phỏng; không phải trạng thái kiểm duyệt nội dung." action={<Button disabled={loading} onClick={() => void load()}><LearningIcon name="refresh" /> Làm mới</Button>} />
    {error && <ErrorNotice error={error} onRetry={() => void load()} />}
    {loading ? <Loading text="Đang tải các lần kiểm định…" headers={["Thời gian", "Chủ đề", "Schema", "Bộ giải", "Trạng thái", "Lỗi"]} /> : <Panel title={`${items.length} lần chạy`} description="Theo dõi kết quả đã lưu và thông báo lỗi của từng lần chạy. Manager có quyền xem nhật ký này.">
      <Table.Root ref={tableRef} size="2" variant="ghost" className={`admin-table ${tableScroll.viewport}`}>
        <Table.Header><Table.Row>
          <Table.ColumnHeaderCell>Thời gian</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Chủ đề</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Schema</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Bộ giải</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Trạng thái</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Lỗi</Table.ColumnHeaderCell>
        </Table.Row></Table.Header>
        <Table.Body>{visibleItems.length === 0 ? <Table.Row><Table.Cell colSpan={6} className="admin-empty-table">Chưa có lần kiểm định.</Table.Cell></Table.Row> : visibleItems.map(item => <Table.Row key={item.id}>
          <Table.Cell>{new Date(item.createdAt).toLocaleString("vi-VN")}</Table.Cell>
          <Table.Cell>{item.topic}</Table.Cell>
          <Table.Cell>{item.schemaId} <small>{item.schemaVersion}</small></Table.Cell>
          <Table.Cell>{item.solverVersion}</Table.Cell>
          <Table.Cell><Badge color={item.passed ? "cyan" : "red"} size="2">{item.passed ? "Đạt" : "Không đạt"}</Badge></Table.Cell>
          <Table.Cell className={item.errorMessage ? "admin-error-cell" : undefined}>{item.errorMessage || "—"}</Table.Cell>
        </Table.Row>)}</Table.Body>
      </Table.Root>
      <div className="admin-table-pagination">
        <span aria-live="polite">{items.length === 0 ? "0 lần chạy" : `Hiển thị ${pageStart + 1}–${Math.min(pageStart + pageSize, items.length)} / ${items.length} lần chạy`}</span>
        <div>
          <label>Số dòng/trang<Select.Root size="2" value={String(pageSize)} onValueChange={value => { setPageSize(Number(value)); setPage(1); tableRef.current?.scrollTo({ top: 0 }); }}><Select.Trigger aria-label="Số lần chạy trên mỗi trang" /><Select.Content>{[10, 25, 50].map(size => <Select.Item key={size} value={String(size)}>{size}</Select.Item>)}</Select.Content></Select.Root></label>
          <Button disabled={currentPage <= 1} onClick={() => changePage(currentPage - 1)}>Trước</Button>
          <span>Trang {currentPage}/{pageCount}</span>
          <Button disabled={currentPage >= pageCount} onClick={() => changePage(currentPage + 1)}>Sau</Button>
        </div>
      </div>
    </Panel>}
  </div>;
}
