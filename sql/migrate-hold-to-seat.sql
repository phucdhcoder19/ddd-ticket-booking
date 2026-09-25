-- ─────────────────────────────────────────────────────────────────────────
-- CHUYỂN GIỮ CHỖ TỪ "ĐẾM SỐ LƯỢNG" SANG "THEO TỪNG GHẾ"
--
--   docker exec -i pre-event-mysql mysql -uroot -proot1234 train_ticket < sql/migrate-hold-to-seat.sql
--
-- Chỉ cần chạy trên CSDL ĐÃ CÓ TỪ TRƯỚC. Máy mới tinh thì bỏ qua file này:
-- ddl-auto: update sẽ tự tạo đúng cấu trúc mới.
--
-- Vì sao phải chạy tay: ddl-auto: update chỉ biết THÊM cột, không bao giờ
-- xoá cột cũ hay nới ràng buộc. Bảng ticket_hold cũ có ticket_id NOT NULL,
-- mà bản mới không còn ghi cột đó nữa -> mọi lượt giữ chỗ sẽ chết ngay ở
-- câu INSERT với lỗi "Field 'ticket_id' doesn't have a default value".
-- ─────────────────────────────────────────────────────────────────────────

-- Xoá luôn cả bảng thay vì ALTER từng cột: giữ chỗ là dữ liệu SỐNG 10 PHÚT.
-- Không có gì trong đó đáng giữ lại, và những lượt giữ cũ thì trỏ vào mô
-- hình kho cũ nên cũng không còn ý nghĩa.
DROP TABLE IF EXISTS ticket_hold;

-- Đơn hàng thì KHÔNG được xoá — đó là chứng từ thu tiền.
-- Chỉ nới ticket_id thành NULL để đơn đặt theo ghế ghi được.
-- (Bỏ qua lỗi nếu bảng chưa tồn tại: máy mới chưa chạy app lần nào.)
ALTER TABLE ticket_order MODIFY ticket_id BIGINT NULL;
