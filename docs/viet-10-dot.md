# Việt — 10 đợt đưa Staff từ preview sang bản chính thức

## Quy tắc

- `../physLive_preview` là nguồn chuẩn, chỉ đọc. Không sửa, commit hoặc push vào repo preview.
- Mốc tham chiếu: preview HEAD `cbf2856`, gồm cả thay đổi local hiện có khi đối chiếu. Thay đổi local chưa commit không được coi là bản phát hành đã kiểm thử.
- Mỗi đợt có một branch, commit ngắn và kiểm thử riêng. Không lấy toàn bộ preview đè lên main.
- Tạo branch đợt tiếp theo từ kết quả đợt trước đã được kiểm tra/tích hợp, tránh dùng lại main cũ.
- Chỉ thực hiện đợt 1 trong ngày 13/10/2026. Các đợt 2–10 là kế hoạch, chưa triển khai và chưa tạo branch rỗng.
- Không thay đổi phần của Hùng, Minh, Kiệt ngoài dependency tối thiểu; ghi rõ phần cần phối hợp.

## Các branch và phạm vi

1. **1/10 — `feat/viet-01-backend-compile`**: sửa 5 lỗi compile, bổ sung mapping/DTO và kiểm thử hồi quy. Commit: `fix(viet-01): sua 5 loi compile backend`.
2. **2/10 — `feat/viet-02-staff-permissions`**: chuyển Teacher sang Staff, quyền TEACH/tổ trưởng, route guard và kiểm tra quyền backend; phối hợp Hùng về Auth/RBAC.
3. **3/10 — `feat/viet-03-problem-understanding`**: nhập text/ảnh, OCR review, understand, xác nhận/chỉnh sửa theo contract preview. Thay lớp tương thích đề bài cũ.
4. **4/10 — `feat/viet-04-simulation-runtime`**: generate/revise/compute, contract đã ký, runtime vật lý và validation; phối hợp Kiệt về schema/dữ liệu chuẩn.
5. **5/10 — `feat/viet-05-simulation-workspace`**: workspace/player, tham số, đồ thị, save/reopen, sửa hiển thị và chẩn đoán render theo preview.
6. **6/10 — `feat/viet-06-library-sharing`**: thư viện cá nhân/thư mục, di chuyển, clone, chia sẻ, cộng đồng và tương tác; phối hợp Kiệt về kiểm duyệt public.
7. **7/10 — `feat/viet-07-assignments-grading`**: giao bài, xem bài nộp, chấm điểm, mở lại bài và báo cáo; thống nhất API Student với Minh.
8. **8/10 — `feat/viet-08-department-workspace`**: tổ trưởng phân công, ghi danh/chuyển học sinh, xem bài giao toàn trường, kiểm duyệt nội bộ; dùng dữ liệu trường/lớp của Minh.
9. **9/10 — `feat/viet-09-export-support-realtime`**: export, phản ánh/sự cố, trạng thái xử lý và sự kiện cập nhật. Kiểm tra riêng giới hạn phát lại/offline.
10. **10/10 — `feat/viet-10-mobile-release`**: Staff Android, tích hợp Student với Minh, kiểm thử liên role và bàn giao; phối hợp Hùng về build/ký. Preview mobile chưa phải bằng chứng hoàn thành toàn bộ luồng, cần ghi rõ phần còn thiếu.

## Đợt 1 — phạm vi thay đổi

- Khôi phục `LibraryItem.folder` và hai query đếm/kiểm tra mục còn hoạt động theo preview.
- Bổ sung `Specification.contractVersion`; mapper trả đủ contract, schema và kết quả validation.
- Khôi phục API sửa đặc tả/xác nhận mà controller trên main đang gọi. Đây là lớp tương thích cho frontend cũ, không phải pipeline preview đã chuyển xong.
- Khi sửa đặc tả, tạo revision chưa validation để không ghi đè dữ kiện của simulation/replay cũ; giữ các ambiguity còn mở.
- Xác nhận dùng service ghi quyết định hiện có; chưa chuyển cơ chế hiểu/chỉnh lại đề bằng AI của preview (đợt 3).
- Migration V5 chỉ bổ sung cột/index, không xóa dữ liệu. Cần schema hiện hữu từ các bản trước; không phải script khởi tạo database trống.
- Integration test dùng H2 do Hibernate tạo schema, tắt Flyway riêng trong các test này. Migration PostgreSQL cần kiểm tra trên staging trước khi triển khai.

## Nghiệm thu mỗi đợt

Đợt 1: `mvn -B test` thành công, 198 test pass, không fail/error/skip. Chưa kiểm thử migration trên PostgreSQL hoặc toàn bộ luồng UI.

- Code và test nằm trong đúng branch; comment giải thích ngắn gọn phần cần thiết.
- Backend compile thành công; kiểm thử liên quan và kiểm thử hồi quy không thất bại.
- Không bỏ kiểm tra quyền hoặc trả dữ liệu giả để làm test pass.
- Ghi rõ chức năng đã chuyển, phần tương thích tạm và phần còn thiếu.
- Không tự merge các đợt chưa nghiệm thu vào main.
