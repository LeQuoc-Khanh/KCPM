"use client";

import React, { useEffect, useMemo, useState } from "react";
import api from "@/services/api";
import { Search, Lock, Unlock } from "lucide-react";
import toast from "react-hot-toast";
import { useConfirm } from "@/context/ConfirmDialogContext";
import { useSearchParams, useRouter } from "next/navigation";

type UserStatus = "ACTIVE" | "BANNED" | "PENDING_VERIFICATION";
type UserRole = "ADMIN" | "CANDIDATE" |"CANDIDATE_VIP" | "RECRUITER" |"RECRUITER_VIP" | string;
type UpdateUserRolePayload = { userRole: UserRole };

type CreateUserPayload = {
  fullName: string;
  email: string;
  userRole: UserRole; // "ADMIN" | "CANDIDATE" | "RECRUITER" | ...
  password?: string;  // optional, nếu rỗng backend tự sinh
};

type CreateUserResponse = {
  user: UserData;
  generatedPassword: string | null;
};

interface UserData {
  id: number;
  fullName: string;
  email: string;
  userRole: UserRole; // khớp AdminUserResponse: role
  status: UserStatus;
  createdAt: string; // LocalDateTime -> string ISO
}

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // page index
  size: number;
  first: boolean;
  last: boolean;
}

function formatDate(input: string) {
  // input dạng "2024-01-01T10:00:00"
  if (!input) return "";
  return input.replace("T", " ").slice(0, 16);
}

function statusLabel(status: UserStatus) {
  switch (status) {
    case "ACTIVE":
      return "Hoạt động";
    case "BANNED":
      return "Đã khóa";
    case "PENDING_VERIFICATION":
      return "Chờ xác thực";
    default:
      return status;
  }
}

function roleLabel(role: string) {
  switch (role) {
    case "ADMIN":
      return "Administrator";
    case "RECRUITER":
      return "Recruiter";
    case "RECRUITER_VIP":
      return "Recruiter VIP";
    case "CANDIDATE":
      return "Candidate";
    case "CANDIDATE_VIP":
      return "Candidate VIP";
    default:
      return role;
  }
}

function AddUserModal({
  open,
  onClose,
  onSubmit,
  loading,
  generatedPassword,
}: {
  open: boolean;
  onClose: () => void;
  onSubmit: (payload: CreateUserPayload) => void;
  loading: boolean;
  generatedPassword: string | null;
}) {
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [userRole, setUserRole] = useState<UserRole>("CANDIDATE");
  const [autoPassword, setAutoPassword] = useState(true);
  const [password, setPassword] = useState("");

  useEffect(() => {
    if (!open) return;
    // reset form mỗi lần mở
    setFullName("");
    setEmail("");
    setUserRole("CANDIDATE");
    setAutoPassword(true);
    setPassword("");
  }, [open]);

  if (!open) return null;

  const canSubmit =
    fullName.trim().length > 0 &&
    email.trim().length > 0 &&
    (autoPassword || password.trim().length >= 6);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 p-4">
      <div className="w-full max-w-lg rounded-2xl bg-white shadow-xl border">
        <div className="flex items-center justify-between px-6 py-4 border-b">
          <h2 className="text-lg font-semibold text-gray-800">Thêm người dùng</h2>
          <button
            onClick={onClose}
            className="px-2 py-1 rounded-lg hover:bg-gray-100 text-gray-600"
          >
            ✕
          </button>
        </div>

        <div className="px-6 py-4 space-y-4">
          <div>
            <label className="text-sm font-medium text-gray-700">Họ tên</label>
            <input
              className="mt-1 w-full rounded-lg border px-3 py-2 outline-none focus:ring-2 focus:ring-blue-500"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              placeholder="Nguyễn Văn A"
            />
          </div>

          <div>
            <label className="text-sm font-medium text-gray-700">Email</label>
            <input
              className="mt-1 w-full rounded-lg border px-3 py-2 outline-none focus:ring-2 focus:ring-blue-500"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="a@gmail.com"
            />
          </div>

          <div>
            <label className="text-sm font-medium text-gray-700">Vai trò</label>
            <select
              className="mt-1 w-full rounded-lg border px-3 py-2 outline-none focus:ring-2 focus:ring-blue-500"
              value={userRole}
              onChange={(e) => setUserRole(e.target.value)}
            >
              <option value="CANDIDATE">CANDIDATE</option>
              <option value="CANDIDATE_VIP">CANDIDATE_VIP</option>
              <option value="RECRUITER">RECRUITER</option>
              <option value="RECRUITER_VIP">RECRUITER_VIP</option>
              <option value="ADMIN">ADMIN</option>
            </select>
          </div>

          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              checked={autoPassword}
              onChange={(e) => setAutoPassword(e.target.checked)}
            />
            <span className="text-sm text-gray-700">Tự sinh mật khẩu tạm</span>
          </div>

          {!autoPassword && (
            <div>
              <label className="text-sm font-medium text-gray-700">Mật khẩu</label>
              <input
                type="password"
                className="mt-1 w-full rounded-lg border px-3 py-2 outline-none focus:ring-2 focus:ring-blue-500"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Tối thiểu 6 ký tự"
              />
            </div>
          )}

          {generatedPassword && (
            <div className="rounded-lg bg-green-50 border border-green-200 p-3 text-sm text-green-800">
              <div className="font-medium">Mật khẩu tạm (copy gửi cho user):</div>
              <div className="mt-2 flex items-center justify-between gap-2">
                <code className="break-all">{generatedPassword}</code>
                <button
                  onClick={async () => {
                    await navigator.clipboard.writeText(generatedPassword);
                    toast.success("Đã copy mật khẩu tạm");
                  }}
                  className="shrink-0 px-3 py-1.5 rounded-lg border bg-white hover:bg-gray-50"
                >
                  Copy
                </button>
              </div>
            </div>
          )}
        </div>

        <div className="flex justify-end gap-2 px-6 py-4 border-t">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-lg border text-sm hover:bg-gray-50"
            disabled={loading}
          >
            Đóng
          </button>
          <button
            onClick={() =>
              onSubmit({
                fullName: fullName.trim(),
                email: email.trim(),
                userRole,
                password: autoPassword ? "" : password,
              })
            }
            disabled={!canSubmit || loading}
            className="px-4 py-2 rounded-lg bg-blue-600 text-white text-sm disabled:opacity-50"
          >
            {loading ? "Đang tạo..." : "Tạo người dùng"}
          </button>
        </div>
      </div>
    </div>
  );
}

function ChangeRoleModal({
  open,
  user,
  onClose,
  onSave,
  loading,
}: {
  open: boolean;
  user: UserData | null;
  onClose: () => void;
  onSave: (newRole: UserRole) => void;
  loading: boolean;
}) {
  const [role, setRole] = useState<UserRole>("CANDIDATE");

  useEffect(() => {
    if (open && user) setRole(user.userRole as UserRole);
  }, [open, user]);

  if (!open || !user) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 p-4">
      <div className="w-full max-w-md rounded-2xl bg-white shadow-xl border">
        <div className="flex items-center justify-between px-6 py-4 border-b">
          <h2 className="text-lg font-semibold">Đổi vai trò</h2>
          <button onClick={onClose} className="px-2 py-1 rounded-lg hover:bg-gray-100">✕</button>
        </div>

        <div className="px-6 py-4 space-y-3">
          <div className="text-sm text-gray-700">
            <div className="font-medium">{user.fullName}</div>
            <div className="text-gray-500">{user.email}</div>
          </div>

          <div>
            <label className="text-sm font-medium text-gray-700">Vai trò mới</label>
            <select
              className="mt-1 w-full rounded-lg border px-3 py-2 outline-none focus:ring-2 focus:ring-blue-500"
              value={role}
              onChange={(e) => setRole(e.target.value as UserRole)}
              disabled={loading}
            >
              <option value="CANDIDATE">CANDIDATE</option>
              <option value="CANDIDATE_VIP">CANDIDATE_VIP</option>
              <option value="RECRUITER">RECRUITER</option>
              <option value="RECRUITER_VIP">RECRUITER_VIP</option>
              {/* nếu bạn muốn cho đổi ADMIN thì mở option này */}
              {/* <option value="ADMIN">ADMIN</option> */}
            </select>
          </div>
        </div>

        <div className="flex justify-end gap-2 px-6 py-4 border-t">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-lg border text-sm hover:bg-gray-50"
            disabled={loading}
          >
            Hủy
          </button>
          <button
            onClick={() => onSave(role)}
            className="px-4 py-2 rounded-lg bg-blue-600 text-white text-sm disabled:opacity-50"
            disabled={loading || role === (user.userRole as UserRole)}
          >
            {loading ? "Đang lưu..." : "Lưu"}
          </button>
        </div>
      </div>
    </div>
  );
}

export default function UserManagementPage() {
  const [users, setUsers] = useState<UserData[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [searchTerm, setSearchTerm] = useState<string>("");

  const [page, setPage] = useState<number>(0);
  const [size, setSize] = useState<number>(10);

  const [totalPages, setTotalPages] = useState<number>(0);
  const [totalElements, setTotalElements] = useState<number>(0);
  const confirm = useConfirm();

  const canPrev = page > 0;
  const canNext = page + 1 < totalPages;

  const [openAdd, setOpenAdd] = useState(false);
  const [creating, setCreating] = useState(false);
  const [generatedPassword, setGeneratedPassword] = useState<string | null>(null);

  // modal đổi vai trò
  const [openRoleModal, setOpenRoleModal] = useState(false);
  const [selectedUser, setSelectedUser] = useState<UserData | null>(null);
  const [roleSaving, setRoleSaving] = useState(false);

  // lấy tham số role từ URL
  const searchParams = useSearchParams();
  const router = useRouter();
  const roleParam = searchParams.get("role"); // ví dụ "CANDIDATE_VIP"

  // tạo người dùng
  const createUser = async (payload: CreateUserPayload) => {
    const res = await api.post<CreateUserResponse>("/admin/users", payload);
    return res.data;
  };

  // thay đổi vai trò người dùng
  const updateUserRole = async (userId: number, payload: UpdateUserRolePayload) => {
  const res = await api.put(`/admin/users/${userId}/role`, payload);
  return res.data;
};

  const fetchUsers = async (opts?: {
    page?: number;
    size?: number;
    keyword?: string;
  }) => {
    try {
      setLoading(true);

      const p = opts?.page ?? page;
      const s = opts?.size ?? size;
      const keyword = opts?.keyword ?? searchTerm;

      const res = await api.get<PageResponse<UserData>>("/admin/users", {
        params: {
          keyword: keyword?.trim() || undefined,
          role: roleParam || undefined,
          page: p,
          size: s,
          sort: "createdAt,desc",
        },
      });

      const data = res.data;
      setUsers(data.content ?? []);
      setTotalPages(data.totalPages ?? 0);
      setTotalElements(data.totalElements ?? 0);
      setPage(data.number ?? p);
      setSize(data.size ?? s);
    } catch (err) {
      console.error("fetchUsers error:", err);
      setUsers([]);
      setTotalPages(0);
      setTotalElements(0);
    } finally {
      setLoading(false);
    }
  };

  // load lần đầu
  useEffect(() => {
    fetchUsers({ page: 0 });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [roleParam]);

  // debounce search
  useEffect(() => {
    const t = setTimeout(() => {
      fetchUsers({ page: 0, keyword: searchTerm });
    }, 300);

    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchTerm]);

  const handleToggleStatus = async (u: UserData) => {
    const isConfirmed = await confirm({
      title: "Xác nhận thay đổi",
      message: `Bạn có chắc muốn thay đổi trạng thái của người dùng ${u.fullName}?`,
      isDanger: u.status === "ACTIVE", // Màu đỏ nếu đang khóa
      confirmLabel: u.status === "ACTIVE" ? "Khóa ngay" : "Mở khóa",
    });
    if (!isConfirmed) return;

    try {
      // ACTIVE -> lock (BANNED), BANNED -> unlock (ACTIVE)
      if (u.status === "ACTIVE") {
        await api.put(`/admin/users/${u.id}/lock`);
      } else if (u.status === "BANNED") {
        await api.put(`/admin/users/${u.id}/unlock`);
      } else {
        toast.error("User đang chờ xác thực, không thể thay đổi trạng thái.");
        return;
      }

      await fetchUsers(); // refresh list
    } catch (err) {
      console.error("toggle status error:", err);
      toast.error("Thay đổi trạng thái thất bại.");
    }
  };

  // className cho badge vai trò
  const roleBadgeClass = useMemo(
    () => (role: string) => {
      // VIP: nổi bật hơn
      if (role === "RECRUITER_VIP") return "bg-fuchsia-100 text-fuchsia-800";
      if (role === "CANDIDATE_VIP") return "bg-indigo-100 text-indigo-800";

      // Thường
      if (role === "RECRUITER") return "bg-purple-100 text-purple-700";
      if (role === "ADMIN") return "bg-amber-100 text-amber-800";
      if (role === "CANDIDATE") return "bg-blue-100 text-blue-700";

      // fallback
      return "bg-gray-100 text-gray-700";
    },
    [],
  );

  const statusBadgeClass = useMemo(
    () => (status: UserStatus) =>
      status === "ACTIVE"
        ? "bg-green-100 text-green-700"
        : status === "PENDING_VERIFICATION"
          ? "bg-yellow-100 text-yellow-800"
          : "bg-red-100 text-red-700",
    [],
  );

  // xử lý tạo người dùng
  const handleCreateUser = async (payload: CreateUserPayload) => {
    try {
      setCreating(true);
      setGeneratedPassword(null);

      const data = await createUser(payload);

      toast.success("Tạo người dùng thành công!");
      setGeneratedPassword(data.generatedPassword ?? null);

      // refresh list: quay về trang 0 để thấy user mới
      await fetchUsers({ page: 0 });

      // Nếu bạn muốn đóng modal ngay khi admin tự nhập mật khẩu:
      // nếu không có generatedPassword thì đóng luôn
      if (!data.generatedPassword) {
        setOpenAdd(false);
      }
    } catch (err: any) {
      console.error("create user error:", err);
      toast.error(err?.response?.data?.message || "Tạo người dùng thất bại.");
    } finally {
      setCreating(false);
    }
  };

  // xử lý đổi vai trò
  const handleSaveRole = async (newRole: UserRole) => {
  if (!selectedUser) return;

  const ok = await confirm({
    title: "Xác nhận đổi vai trò",
    message: `Bạn có chắc muốn đổi vai trò của ${selectedUser.fullName} từ ${selectedUser.userRole} → ${newRole}?`,
    isDanger: false,
    confirmLabel: "Đổi vai trò",
  });
  if (!ok) return;

  try {
    setRoleSaving(true);
    await updateUserRole(selectedUser.id, { userRole: newRole });
    toast.success("Đổi vai trò thành công!");
    setOpenRoleModal(false);
    setSelectedUser(null);
    await fetchUsers(); // refresh list
  } catch (err: any) {
    console.error("update role error:", err);
    toast.error(err?.response?.data?.message || "Đổi vai trò thất bại.");
  } finally {
    setRoleSaving(false);
  }
};

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 md:flex-row md:justify-between md:items-center">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">
            Quản lý người dùng
          </h1>
          <p className="text-sm text-gray-500">
            Tổng: <span className="font-medium">{totalElements}</span>
          </p>
        </div>

        <div className="flex items-center gap-2">
          <div className="relative">
            <input
              type="text"
              placeholder="Tìm kiếm tên hoặc email..."
              className="pl-10 pr-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 outline-none w-72"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
            <Search className="w-5 h-5 text-gray-400 absolute left-3 top-2.5" />
          </div>

          <button
            onClick={() => {
            setGeneratedPassword(null);
            setOpenAdd(true);
          }}
          className="px-4 py-2 rounded-lg bg-blue-600 text-white text-sm hover:bg-blue-700"
          >
            + Thêm người dùng
          </button>
        </div>
      </div>

      {/* Banner lọc theo role */}
      {roleParam && (
        <div className="flex items-center justify-between rounded-xl border bg-blue-50 px-4 py-2 text-sm text-blue-800">
          <div>
            Đang lọc theo vai trò: <span className="font-semibold">{roleLabel(roleParam)}</span>
          </div>
          <button
            type="button"
            className="font-medium text-blue-700 hover:underline"
            onClick={() => router.push("/admin/users")}
          >
            Bỏ lọc
          </button>
        </div>
      )}

      {/* Danh sách người dùng */}
      <div className="bg-white rounded-xl shadow-sm border overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead className="bg-gray-50 border-b">
              <tr>
                <th className="px-6 py-4 text-sm font-medium text-gray-500">
                  Người dùng
                </th>
                <th className="px-6 py-4 text-sm font-medium text-gray-500">
                  Vai trò
                </th>
                <th className="px-6 py-4 text-sm font-medium text-gray-500">
                  Trạng thái
                </th>
                <th className="px-6 py-4 text-sm font-medium text-gray-500">
                  Ngày tham gia
                </th>
                <th className="px-6 py-4 text-sm font-medium text-gray-500 text-right">
                  Hành động
                </th>
              </tr>
            </thead>

            <tbody className="divide-y">
              {users.map((user) => (
                <tr key={user.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4">
                    <div>
                      <p className="font-medium text-gray-900">
                        {user.fullName}
                      </p>
                      <p className="text-sm text-gray-500">{user.email}</p>
                    </div>
                  </td>

                  <td className="px-6 py-4">
                    <span
                      className={`px-2 py-1 rounded-full text-xs font-semibold ${roleBadgeClass(user.userRole)}`}
                    >
                      {roleLabel(user.userRole)}
                    </span>
                  </td>

                  <td className="px-6 py-4">
                    <span
                      className={`px-2 py-1 rounded-full text-xs font-semibold ${statusBadgeClass(user.status)}`}
                    >
                      {statusLabel(user.status)}
                    </span>
                  </td>

                  <td className="px-6 py-4 text-sm text-gray-500">
                    {formatDate(user.createdAt)}
                  </td>

                  <td className="px-6 py-4 text-right">
                    <div className="inline-flex items-center gap-2 justify-end">
                      <button
                        onClick={() => {
                          setSelectedUser(user);
                          setOpenRoleModal(true);
                        }}
                        className="px-3 py-2 border rounded-lg text-sm hover:bg-gray-50"
                        disabled={user.userRole === "ADMIN"}
                        title="Đổi vai trò"
                      >
                        Đổi vai trò
                      </button>

                      <button
                        onClick={() => handleToggleStatus(user)}
                        className="p-2 hover:bg-gray-200 rounded-full transition text-gray-500 disabled:opacity-50"
                        title={
                          user.status === "ACTIVE"
                            ? "Khóa tài khoản"
                            : user.status === "BANNED"
                              ? "Mở khóa"
                              : "Không hỗ trợ"
                        }
                        disabled={user.userRole === "ADMIN"} // tránh khóa admin từ UI
                      >
                        {user.status === "ACTIVE" ? (
                          <Lock className="w-4 h-4" />
                        ) : user.status === "BANNED" ? (
                          <Unlock className="w-4 h-4 text-green-600" />
                        ) : (
                          <Lock className="w-4 h-4 opacity-40" />
                        )}
                      </button>
                    </div>  
                  </td>  
                </tr>
              ))}

              {!loading && users.length === 0 && (
                <tr>
                  <td
                    colSpan={5}
                    className="px-6 py-8 text-center text-gray-500"
                  >
                    Không có người dùng nào.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="flex items-center justify-between px-6 py-4 border-t bg-white">
          <div className="text-sm text-gray-500">
            Trang{" "}
            <span className="font-medium">
              {totalPages === 0 ? 0 : page + 1}
            </span>{" "}
            / <span className="font-medium">{totalPages}</span>
          </div>

          <div className="flex items-center gap-2">
            <button
              className="px-3 py-2 border rounded-lg text-sm disabled:opacity-50"
              disabled={!canPrev || loading}
              onClick={() => fetchUsers({ page: page - 1 })}
            >
              Trước
            </button>
            <button
              className="px-3 py-2 border rounded-lg text-sm disabled:opacity-50"
              disabled={!canNext || loading}
              onClick={() => fetchUsers({ page: page + 1 })}
            >
              Sau
            </button>
          </div>
        </div>

        {loading && (
          <div className="px-6 py-3 text-center text-gray-500">Đang tải...</div>
        )}
      </div>
      {/*modal thêm người dùng*/}
      <AddUserModal
        open={openAdd}
        onClose={() => setOpenAdd(false)}
        onSubmit={handleCreateUser}
        loading={creating}
        generatedPassword={generatedPassword}
      />
      {/*modal đổi vai trò*/}
      <ChangeRoleModal
        open={openRoleModal}
        user={selectedUser}
        onClose={() => {
          setOpenRoleModal(false);
          setSelectedUser(null);
        }}
        onSave={handleSaveRole}
        loading={roleSaving}
      />
    </div>
  );
}
