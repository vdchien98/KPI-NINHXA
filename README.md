# Tổng hợp báo cáo công việc - Phường Ninh Xá

Hệ thống tổng hợp báo cáo công việc Phường Ninh Xá, TP Bắc Ninh.

## 🚀 Công nghệ sử dụng

- **Frontend**: Next.js 14, React, TypeScript, Tailwind CSS, shadcn/ui
- **Backend**: Java Spring Boot 3.2, JDK 21
- **Database**: MySQL 8.0 (Docker)

## 📋 Yêu cầu hệ thống

- Node.js 18+ 
- Java JDK 21
- Maven 3.9+
- Docker & Docker Compose

## 🛠️ Cài đặt và Chạy

### 1. Khởi động MySQL Database

```bash
# Khởi động MySQL container (port 9498)
docker-compose up -d

# Kiểm tra container đang chạy
docker ps

# Xem logs
docker-compose logs -f mysql
```

**Thông tin kết nối MySQL:**
- Host: localhost
- Port: 9498
- Database: ninhxa_report
- Username: ninhxa_user
- Password: ninhxa_pass123
- Root Password: root123456

### 2. Chạy Backend (Spring Boot)

```bash
cd backend

# Build project
mvn clean install -DskipTests

# Chạy ứng dụng
mvn spring-boot:run
```

Backend sẽ chạy tại: `http://localhost:9499/api`

### 3. Chạy Frontend (Next.js)

```bash
cd frontend

# Cài đặt dependencies
npm install

# Chạy development server
npm run dev
```

Frontend sẽ chạy tại: `http://localhost:3001`

## 🔐 Tài khoản đăng nhập

- **Email**: admin@bacninh.gov.vn
- **Mật khẩu**: admin123

## 📁 Cấu trúc dự án

```
reportNinhXa/
├── backend/                 # Spring Boot Backend
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── vn/gov/bacninh/ninhxareport/
│   │       │       ├── controller/     # REST Controllers
│   │       │       ├── dto/            # Data Transfer Objects
│   │       │       ├── entity/         # JPA Entities
│   │       │       ├── repository/     # JPA Repositories
│   │       │       ├── security/       # Security Config & JWT
│   │       │       └── service/        # Business Logic
│   │       └── resources/
│   │           └── application.yml     # Config file
│   └── pom.xml
├── frontend/                # Next.js Frontend
│   ├── src/
│   │   ├── app/
│   │   │   ├── admin/       # Admin pages
│   │   │   ├── login/       # Login page
│   │   │   └── layout.tsx   # Root layout
│   │   ├── components/
│   │   │   └── ui/          # shadcn/ui components
│   │   └── lib/
│   │       ├── api.ts       # API client
│   │       ├── store.ts     # Zustand store
│   │       └── utils.ts     # Utilities
│   └── package.json
├── database/
│   └── init.sql             # Database initialization
├── docker-compose.yml       # Docker config
└── README.md
```

## ✨ Tính năng

### Đã hoàn thành:
- ✅ Trang đăng nhập với JWT authentication
- ✅ Giao diện Admin với theme đỏ vàng (Cờ Việt Nam)
- ✅ Quản lý Role (CRUD, phân cấp)
- ✅ Quản lý Người dùng (CRUD, phân quyền)
- ✅ Quản lý Cơ quan (CRUD)
- ✅ Quản lý Phòng ban (CRUD)
- ✅ Responsive design (Mobile-friendly)

## 🎨 Theme

- **Màu chủ đạo**: Trắng, Đen
- **Điểm nhấn**: 
  - Đỏ (#DA251D) - Màu cờ đỏ
  - Vàng (#FFCD00) - Màu sao vàng

## 📝 API Endpoints

### Authentication
- `POST /api/auth/login` - Đăng nhập
- `GET /api/auth/me` - Lấy thông tin user hiện tại

### Admin - Roles
- `GET /api/admin/roles` - Lấy danh sách roles
- `GET /api/admin/roles/tree` - Lấy cây roles
- `POST /api/admin/roles` - Tạo role mới
- `PUT /api/admin/roles/{id}` - Cập nhật role
- `DELETE /api/admin/roles/{id}` - Xóa role

### Admin - Users
- `GET /api/admin/users` - Lấy danh sách users
- `POST /api/admin/users` - Tạo user mới
- `PUT /api/admin/users/{id}` - Cập nhật user
- `DELETE /api/admin/users/{id}` - Xóa user

### Admin - Organizations
- `GET /api/admin/organizations` - Lấy danh sách cơ quan
- `POST /api/admin/organizations` - Tạo cơ quan mới
- `PUT /api/admin/organizations/{id}` - Cập nhật cơ quan
- `DELETE /api/admin/organizations/{id}` - Xóa cơ quan

### Admin - Departments
- `GET /api/admin/departments` - Lấy danh sách phòng ban
- `GET /api/admin/departments/by-organization/{id}` - Lấy phòng ban theo cơ quan
- `POST /api/admin/departments` - Tạo phòng ban mới
- `PUT /api/admin/departments/{id}` - Cập nhật phòng ban
- `DELETE /api/admin/departments/{id}` - Xóa phòng ban

## 📞 Liên hệ

UBND Phường Ninh Xá - TP Bắc Ninh - Tỉnh Bắc Ninh

