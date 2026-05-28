'use client';

import React, { useEffect, useMemo, useState } from 'react';
import api from '@/services/api';
import { Shield, Plus, Edit, Trash2 } from 'lucide-react';
import Link from 'next/link';

type RoleKey =
  | 'ADMIN'
  | 'CANDIDATE'
  | 'CANDIDATE_VIP'
  | 'RECRUITER'
  | 'RECRUITER_VIP';

type RoleCard = {
  id: number;
  key: RoleKey;
  name: string;
  description: string;
  usersCount: number;
  permissions: string[];
  isSystem: boolean;
};

const BASE_ROLES: RoleCard[] = [
  {
    id: 1,
    key: 'ADMIN',
    name: 'Administrator',
    description: 'Quyền cao nhất, quản lý toàn bộ hệ thống.',
    usersCount: 0,
    permissions: ['all_access', 'manage_users', 'manage_content', 'system_settings'],
    isSystem: true
  },
  {
    id: 2,
    key: 'RECRUITER',
    name: 'Recruiter',
    description: 'Nhà tuyển dụng, có thể đăng tin và xem hồ sơ ứng viên.',
    usersCount: 0,
    permissions: ['post_job', 'view_candidate', 'manage_applications'],
    isSystem: true
  },
  {
    id: 3,
    key: 'RECRUITER_VIP',
    name: 'Recruiter VIP',
    description: 'Recruiter nâng cao: nhiều quyền và hạn mức hơn.',
    usersCount: 0,
    permissions: ['post_job', 'view_candidate', 'manage_applications', 'vip_talent_search', 'bulk_actions'],
    isSystem: true
  },
  {
    id: 4,
    key: 'CANDIDATE',
    name: 'Candidate',
    description: 'Người tìm việc, có thể ứng tuyển và tạo hồ sơ.',
    usersCount: 0,
    permissions: ['create_profile', 'apply_job', 'view_job'],
    isSystem: true
  },
  {
    id: 5,
    key: 'CANDIDATE_VIP',
    name: 'Candidate VIP',
    description: 'Candidate nâng cao: dùng AI & template premium.',
    usersCount: 0,
    permissions: ['create_profile', 'apply_job', 'view_job', 'ai_coach', 'advanced_cv_review'],
    isSystem: true
  }
];

// Fix trường hợp BE trả key có khoảng trắng: "CANDIDATE VIP"
function normalizeRoleKey(k: string) {
  return k.trim().replace(/\s+/g, '_').toUpperCase();
}

export default function RolesPage() {
  const [roles, setRoles] = useState<RoleCard[]>(BASE_ROLES);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleDelete = (id: number) => {
    if (confirm('Bạn có chắc muốn xóa vai trò này?')) {
      setRoles((prev) => prev.filter((role) => role.id !== id));
    }
  };

  const fetchCounts = async () => {
    setLoading(true);
    setError(null);
    try {
      // ✅ dùng chung api client => tự gắn token => hết 401
      const res = await api.get('/admin/roles/count-users');

      // tuỳ BE trả structure: {success, message, data} hoặc trả raw map
      const raw = res.data?.data ?? res.data;

      const normalized: Record<string, number> = {};
      Object.keys(raw || {}).forEach((key) => {
        normalized[normalizeRoleKey(key)] = Number(raw[key] ?? 0);
      });

      setRoles((prev) =>
        prev.map((r) => ({
          ...r,
          usersCount: Number(normalized?.[r.key] ?? 0)
        }))
      );
    } catch (err: any) {
      console.error('Lỗi tải dữ liệu Roles:', err);
      setError(err?.response?.data?.message || err?.message || 'Không tải được dữ liệu phân quyền');
      // fallback giữ 0 để UI vẫn hiển thị
      setRoles(BASE_ROLES);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCounts();
  }, []);

  const subtitle = useMemo(() => {
    if (loading) return 'Đang tải dữ liệu phân quyền...';
    if (error) return `Lỗi: ${error}`;
    return 'Quản lý vai trò và quyền hạn truy cập của người dùng.';
  }, [loading, error]);

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Phân quyền hệ thống</h1>
          <p className="text-gray-500 text-sm">{subtitle}</p>
        </div>
        <button
          className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg text-sm font-medium flex items-center shadow-sm transition"
          onClick={() => alert('MVP: dùng 5 vai trò hệ thống.')}
        >
          <Plus className="w-4 h-4 mr-2" />
          Thêm vai trò mới
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
        {roles.map((role) => (
          <div key={role.id} className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 hover:shadow-md transition">
            <div className="flex justify-between items-start mb-4">
              <div className="p-3 bg-blue-50 text-blue-600 rounded-lg">
                <Shield className="w-6 h-6" />
              </div>
              <div className="flex gap-2">
                <button className="p-2 text-gray-400 hover:text-blue-600 transition" title="Chỉnh sửa">
                  <Edit className="w-4 h-4" />
                </button>

                {!role.isSystem && (
                  <button
                    onClick={() => handleDelete(role.id)}
                    className="p-2 text-gray-400 hover:text-red-600 transition"
                    title="Xóa"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                )}
              </div>
            </div>

            <h3 className="text-lg font-bold text-gray-800">{role.name}</h3>
            <p className="text-sm text-gray-500 mt-1 h-10 line-clamp-2">{role.description}</p>

            <div className="mt-4 flex items-center text-sm text-gray-600">
              <span className="font-semibold mr-1">{role.usersCount}</span> người dùng đang sở hữu
            </div>

            <div className="mt-2">
              <Link
                href={`/admin/users?role=${encodeURIComponent(role.key)}`}
                className="inline-flex items-center gap-2 text-sm text-blue-600 hover:text-blue-700 hover:underline"
              >
                Xem người dùng
                <span aria-hidden>→</span>
              </Link>
            </div>

            <div className="mt-6 pt-4 border-t border-gray-100">
              <p className="text-xs font-semibold text-gray-400 uppercase mb-3">Quyền hạn chính</p>
              <div className="flex flex-wrap gap-2">
                {role.permissions.map((perm, index) => (
                  <span key={index} className="px-2 py-1 bg-gray-100 text-gray-600 text-xs rounded border border-gray-200">
                    {perm.replaceAll('_', ' ')}
                  </span>
                ))}
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
