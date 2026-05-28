
package app.config;

import org.springframework.context.annotation.Configuration; 
// @Configuration: Đánh dấu lớp này là một lớp cấu hình Spring, cho phép định nghĩa bean hoặc thiết lập cấu hình.

import org.springframework.data.jpa.repository.config.EnableJpaAuditing; 
// @EnableJpaAuditing: Bật tính năng JPA Auditing, cho phép tự động ghi nhận các trường như @CreatedDate và @LastModifiedDate trong entity.

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
    // Lớp cấu hình này dùng để kích hoạt JPA Auditing.
    // Khi bật, các annotation như @CreatedDate và @LastModifiedDate trong entity sẽ tự động được cập nhật.
    // Điều này hữu ích cho việc theo dõi thời gian tạo và chỉnh sửa bản ghi trong cơ sở dữ liệu.
}