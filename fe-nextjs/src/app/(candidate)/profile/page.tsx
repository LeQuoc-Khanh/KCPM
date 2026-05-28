"use client";
import React, { useEffect, useState } from "react";
import {
  getMyProfile,
  updateProfile,
  uploadAvatar,
} from "@/services/candidateService";
import {
  User,
  Globe,
  Linkedin,
  Book,
  Code,
  Save,
  Loader2,
  Camera,
  Briefcase,
  Plus,
  Trash2,
  Mail,
  AlertCircle,
  Edit2,
  X,
  Shield,
  FileText,
} from "lucide-react";
import toast from "react-hot-toast";
import { useAuth } from "@/context/Authcontext";

interface ExperienceForm {
  companyName: string;
  role: string;
  startDate: string;
  endDate: string;
  description: string;
}

export default function ProfilePage() {
  const { updateUser, user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [errors, setErrors] = useState<{ [key: string]: string }>({});

  const [formData, setFormData] = useState({
    userFullName: "",
    fullName: "",
    email: "",
    avatarUrl: "",
    aboutMe: "",
    phoneNumber: "",
    address: "",
    websiteUrl: "",
    linkedInUrl: "",
    skills: [] as string[],
  });

  const [experiences, setExperiences] = useState<ExperienceForm[]>([]);

  useEffect(() => {
    fetchProfile();
  }, []);

  const formatDateForInput = (dateStr: string) => {
    if (!dateStr) return "";
    if (/^\d{4}-\d{2}$/.test(dateStr)) return dateStr;
    try {
      const date = new Date(dateStr);
      if (!isNaN(date.getTime())) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        return `${year}-${month}`;
      }
    } catch (e) {
      console.warn("Cannot parse date:", dateStr);
    }
    return "";
  };

  const fetchProfile = async () => {
    try {
      const data = await getMyProfile();
      if (data) {
        setFormData({
          userFullName: data.userFullName || (user as any)?.fullName || "",
          fullName: data.fullName || "",
          email: data.email || "",
          avatarUrl: data.avatarUrl || (user as any)?.profileImageUrl || "",
          aboutMe: data.aboutMe || "",
          phoneNumber: data.phoneNumber || "",
          address: data.address || "",
          websiteUrl: data.websiteUrl || "",
          linkedInUrl: data.linkedInUrl || "",
          skills: data.skills || [],
        });

        if (data.experiences && data.experiences.length > 0) {
          const mappedExps = data.experiences.map((exp: any) => ({
            companyName: exp.company || "",
            role: exp.role || "",
            startDate: formatDateForInput(exp.startDate || ""),
            endDate: formatDateForInput(exp.endDate || ""),
            description: exp.description || "",
          }));
          setExperiences(mappedExps);
        }
      }
    } catch (error) {
      console.error("Lỗi load profile", error);
    } finally {
      setLoading(false);
    }
  };

  const handleAvatarChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size > 5 * 1024 * 1024) {
      toast.error("File quá lớn! Vui lòng chọn ảnh dưới 5MB");
      return;
    }

    const toastId = toast.loading("Đang tải ảnh lên...");
    try {
      const newUrl = await uploadAvatar(file);
      setFormData((prev) => ({ ...prev, avatarUrl: newUrl }));
      updateUser({ profileImageUrl: newUrl } as any);
      toast.success("Đã cập nhật ảnh đại diện!", { id: toastId });
    } catch (error) {
      console.error(error);
      toast.error("Lỗi upload ảnh", { id: toastId });
    }
  };

  const validateForm = () => {
    const newErrors: { [key: string]: string } = {};
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!formData.email) newErrors.email = "Email không được để trống";
    else if (!emailRegex.test(formData.email))
      newErrors.email = "Email không đúng định dạng";

    const phoneRegex = /(84|0[3|5|7|8|9])+([0-9]{8})\b/;
    if (formData.phoneNumber && !phoneRegex.test(formData.phoneNumber)) {
      newErrors.phoneNumber = "Số điện thoại không hợp lệ";
    }

    if (!formData.fullName.trim())
      newErrors.fullName = "Họ tên hồ sơ không được để trống";
    if (!formData.userFullName.trim())
      newErrors.userFullName = "Tên tài khoản không được để trống";

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>,
  ) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) setErrors((prev) => ({ ...prev, [name]: "" }));
  };

  const handleSkillChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const skillsArr = e.target.value.split(",").map((s) => s.trim());
    setFormData((prev) => ({ ...prev, skills: skillsArr }));
  };

  const handleExpChange = (
    index: number,
    field: keyof ExperienceForm,
    value: string,
  ) => {
    const newExps = [...experiences];
    newExps[index][field] = value;
    setExperiences(newExps);
  };

  const addExperience = () => {
    setExperiences([
      ...experiences,
      {
        companyName: "",
        role: "",
        startDate: "",
        endDate: "",
        description: "",
      },
    ]);
  };

  const removeExperience = (index: number) => {
    setExperiences(experiences.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validateForm()) {
      toast.error("Vui lòng kiểm tra lại thông tin nhập");
      return;
    }

    setSaving(true);
    try {
      const payload = { ...formData, experiences };
      await updateProfile(payload);

      updateUser({ fullName: formData.userFullName } as any);

      toast.success("Đã lưu hồ sơ thành công! 🎉");
      setIsEditing(false);
    } catch (error) {
      toast.error("Lỗi khi lưu hồ sơ.");
    } finally {
      setSaving(false);
    }
  };

  const getDisplayAvatar = () => {
    if (formData.avatarUrl) return formData.avatarUrl;
    const name = formData.userFullName || "User";
    return `https://ui-avatars.com/api/?name=${encodeURIComponent(name)}&background=random&color=fff&size=128`;
  };

  const inputClass = (isError: boolean = false) => `
    w-full mt-1 p-2 border rounded-md text-sm outline-none transition-all
    ${!isEditing ? "bg-transparent border-transparent cursor-default" : "bg-white border-gray-300 focus:border-blue-500"}
    ${isError ? "border-red-500 bg-red-50" : ""}
  `;

  if (loading)
    return (
      <div className="flex justify-center items-center h-screen">
        <Loader2 className="animate-spin text-blue-600" />
      </div>
    );

  return (
    <div className="max-w-5xl mx-auto py-8 px-4">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Hồ sơ cá nhân</h1>
          <p className="text-gray-500 text-sm">
            Quản lý thông tin hiển thị với nhà tuyển dụng
          </p>
        </div>

        <div className="flex gap-2">
          {!isEditing ? (
            <button
              onClick={() => setIsEditing(true)}
              className="flex items-center gap-2 bg-white text-blue-600 border border-blue-600 px-6 py-2.5 rounded-lg hover:bg-blue-50 transition shadow-sm font-medium"
            >
              <Edit2 size={18} /> Chỉnh sửa
            </button>
          ) : (
            <>
              <button
                onClick={() => {
                  setIsEditing(false);
                  fetchProfile();
                }}
                className="flex items-center gap-2 bg-gray-100 text-gray-600 px-4 py-2.5 rounded-lg hover:bg-gray-200 transition"
              >
                <X size={18} /> Hủy
              </button>
              <button
                onClick={handleSubmit}
                disabled={saving}
                className="flex items-center gap-2 bg-blue-600 text-white px-6 py-2.5 rounded-lg hover:bg-blue-700 transition shadow-md disabled:bg-blue-300"
              >
                {saving ? (
                  <Loader2 className="animate-spin" size={20} />
                ) : (
                  <Save size={20} />
                )}
                {saving ? "Đang lưu..." : "Lưu thay đổi"}
              </button>
            </>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Cột trái: Thông tin & Avatar */}
        <div className="lg:col-span-1 space-y-6">
          <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 flex flex-col items-center">
            <div className="relative group">
              <div className="w-32 h-32 rounded-full overflow-hidden border-4 border-white shadow-md bg-gray-100">
                <img
                  src={getDisplayAvatar()}
                  alt="Avatar"
                  className="w-full h-full object-cover"
                />
              </div>
              {isEditing && (
                <label className="absolute bottom-0 right-0 bg-blue-600 text-white p-2 rounded-full cursor-pointer hover:bg-blue-700 transition shadow-sm border-2 border-white">
                  <Camera size={18} />
                  <input
                    type="file"
                    className="hidden"
                    accept="image/*"
                    onChange={handleAvatarChange}
                  />
                </label>
              )}
            </div>

            {/* FIX LỖI 1: Bọc Shield trong span để dùng title */}
            <h2 className="mt-4 font-bold text-gray-800 text-lg flex items-center gap-1">
              {formData.userFullName}
              <span title="Tên tài khoản xác thực">
                <Shield size={14} className="text-blue-500" />
              </span>
            </h2>
            <p className="text-sm text-gray-500">{formData.email}</p>
          </div>

          <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
            <h3 className="font-semibold text-gray-800 mb-4 flex items-center gap-2">
              <User size={18} className="text-blue-600" /> Thông tin cơ bản
            </h3>
            <div className="space-y-4">
              {/* Ô nhập 1: Tên Tài Khoản (User) */}
              <div>
                <label className="text-xs font-medium text-gray-500 uppercase flex items-center gap-1">
                  Tên Tài Khoản (Đăng nhập){" "}
                  <Shield size={12} className="text-blue-500" />
                </label>
                <input
                  disabled={!isEditing}
                  type="text"
                  name="userFullName"
                  value={formData.userFullName}
                  onChange={handleChange}
                  className={inputClass(!!errors.userFullName)}
                  placeholder="Tên thật của bạn"
                />
                {errors.userFullName && (
                  <p className="text-red-500 text-xs mt-1">
                    {errors.userFullName}
                  </p>
                )}
              </div>

              {/* Ô nhập 2: Tên Hồ Sơ (Profile) */}
              <div>
                <label className="text-xs font-medium text-gray-500 uppercase flex items-center gap-1">
                  Tên Hiển Thị (CV / Hồ Sơ){" "}
                  <FileText size={12} className="text-green-500" />
                </label>
                <input
                  disabled={!isEditing}
                  type="text"
                  name="fullName"
                  value={formData.fullName}
                  onChange={handleChange}
                  className={inputClass(!!errors.fullName)}
                  placeholder="Tên trên CV của bạn"
                />
                {errors.fullName && (
                  <p className="text-red-500 text-xs mt-1">{errors.fullName}</p>
                )}
              </div>

              <div>
                <label className="text-xs font-medium text-gray-500 uppercase">
                  Email liên hệ (CV)
                </label>
                <div
                  className={`flex items-center gap-2 mt-1 border rounded-md px-3 py-2 ${!isEditing ? "border-transparent bg-transparent pl-0" : "bg-gray-50"}`}
                >
                  {isEditing && <Mail size={16} className="text-gray-400" />}
                  <input
                    disabled={!isEditing}
                    type="text"
                    name="email"
                    value={formData.email}
                    onChange={handleChange}
                    className={`w-full bg-transparent outline-none text-sm ${!isEditing ? "text-gray-700" : ""}`}
                  />
                </div>
                {errors.email && (
                  <p className="text-red-500 text-xs mt-1">{errors.email}</p>
                )}
              </div>

              <div>
                <label className="text-xs font-medium text-gray-500 uppercase">
                  Số điện thoại
                </label>
                <input
                  disabled={!isEditing}
                  type="text"
                  name="phoneNumber"
                  value={formData.phoneNumber}
                  onChange={handleChange}
                  className={inputClass(!!errors.phoneNumber)}
                  placeholder="0912..."
                />
                {errors.phoneNumber && (
                  <p className="text-red-500 text-xs mt-1 flex items-center gap-1">
                    <AlertCircle size={10} /> {errors.phoneNumber}
                  </p>
                )}
              </div>

              <div>
                <label className="text-xs font-medium text-gray-500 uppercase">
                  Địa chỉ
                </label>
                <input
                  disabled={!isEditing}
                  type="text"
                  name="address"
                  value={formData.address}
                  onChange={handleChange}
                  className={inputClass()}
                  placeholder="Tỉnh/Thành phố"
                />
              </div>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
            <h3 className="font-semibold text-gray-800 mb-4">Mạng xã hội</h3>
            <div className="space-y-3">
              <div
                className={`flex items-center gap-2 border rounded-md px-3 py-2 transition-all ${!isEditing ? "border-transparent pl-0" : "focus-within:border-blue-500"}`}
              >
                <Linkedin size={16} className="text-blue-700" />
                <input
                  disabled={!isEditing}
                  type="text"
                  name="linkedInUrl"
                  value={formData.linkedInUrl}
                  onChange={handleChange}
                  className={`w-full outline-none text-sm bg-transparent`}
                  placeholder="LinkedIn URL"
                />
              </div>
              <div
                className={`flex items-center gap-2 border rounded-md px-3 py-2 transition-all ${!isEditing ? "border-transparent pl-0" : "focus-within:border-blue-500"}`}
              >
                <Globe size={16} className="text-green-600" />
                <input
                  disabled={!isEditing}
                  type="text"
                  name="websiteUrl"
                  value={formData.websiteUrl}
                  onChange={handleChange}
                  className={`w-full outline-none text-sm bg-transparent`}
                  placeholder="Website / Portfolio"
                />
              </div>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
            <h3 className="font-semibold text-gray-800 mb-4 flex items-center gap-2">
              <Code size={18} className="text-purple-600" /> Kỹ năng
            </h3>
            {isEditing ? (
              <input
                type="text"
                value={formData.skills.join(", ")}
                onChange={handleSkillChange}
                className="w-full p-3 border rounded-lg text-sm focus:border-purple-500 outline-none"
                placeholder="Java, Spring Boot..."
              />
            ) : (
              <p className="text-sm text-gray-400 italic mb-2 display-none">
                {formData.skills.length === 0 ? "Chưa cập nhật kỹ năng" : ""}
              </p>
            )}
            <div className="mt-3 flex flex-wrap gap-2">
              {formData.skills.map(
                (skill, idx) =>
                  skill && (
                    <span
                      key={idx}
                      className="bg-blue-50 text-blue-700 px-3 py-1 rounded-full text-sm font-medium border border-blue-100"
                    >
                      {skill}
                    </span>
                  ),
              )}
            </div>
          </div>
        </div>

        {/* Cột phải: About & Experience */}
        <div className="lg:col-span-2 space-y-6">
          <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
            <h3 className="font-semibold text-gray-800 mb-4 flex items-center gap-2">
              <Book size={18} className="text-orange-500" /> Giới thiệu bản thân
            </h3>
            <textarea
              disabled={!isEditing}
              name="aboutMe"
              rows={4}
              value={formData.aboutMe}
              onChange={handleChange}
              className={`w-full p-3 border rounded-lg outline-none text-sm leading-relaxed transition-all ${!isEditing ? "border-transparent bg-transparent resize-none" : "focus:border-blue-500"}`}
              placeholder={
                isEditing
                  ? "Mục tiêu nghề nghiệp, điểm mạnh..."
                  : "Chưa có thông tin giới thiệu."
              }
            />
          </div>

          <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
            <div className="flex justify-between items-center mb-6">
              <h3 className="font-semibold text-gray-800 flex items-center gap-2">
                <Briefcase size={18} className="text-teal-600" /> Kinh nghiệm
                làm việc
              </h3>
              {isEditing && (
                <button
                  type="button"
                  onClick={addExperience}
                  className="text-sm flex items-center gap-1 text-blue-600 bg-blue-50 px-3 py-1.5 rounded hover:bg-blue-100 transition"
                >
                  <Plus size={16} /> Thêm mới
                </button>
              )}
            </div>

            <div className="space-y-6">
              {experiences.map((exp, index) => (
                <div
                  key={index}
                  className={`relative rounded-lg p-5 border group ${!isEditing ? "border-gray-100 bg-white" : "border-gray-200 bg-gray-50"}`}
                >
                  {isEditing && (
                    <button
                      onClick={() => removeExperience(index)}
                      className="absolute top-4 right-4 text-gray-400 hover:text-red-500 p-1"
                    >
                      <Trash2 size={18} />
                    </button>
                  )}

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                    <div>
                      <label className="text-xs font-semibold text-gray-600 mb-1 block">
                        Công ty
                      </label>
                      <input
                        disabled={!isEditing}
                        type="text"
                        value={exp.companyName}
                        onChange={(e) =>
                          handleExpChange(index, "companyName", e.target.value)
                        }
                        className={inputClass()}
                      />
                    </div>
                    <div>
                      <label className="text-xs font-semibold text-gray-600 mb-1 block">
                        Vị trí
                      </label>
                      <input
                        disabled={!isEditing}
                        type="text"
                        value={exp.role}
                        onChange={(e) =>
                          handleExpChange(index, "role", e.target.value)
                        }
                        className={inputClass()}
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                    <div>
                      <label className="text-xs font-semibold text-gray-600 mb-1 block">
                        Bắt đầu
                      </label>
                      <input
                        disabled={!isEditing}
                        type="month"
                        value={exp.startDate}
                        onChange={(e) =>
                          handleExpChange(index, "startDate", e.target.value)
                        }
                        className={inputClass()}
                      />
                    </div>
                    <div>
                      <label className="text-xs font-semibold text-gray-600 mb-1 block">
                        Kết thúc
                      </label>
                      <input
                        disabled={!isEditing}
                        type="month"
                        value={exp.endDate}
                        onChange={(e) =>
                          handleExpChange(index, "endDate", e.target.value)
                        }
                        className={inputClass()}
                      />
                    </div>
                  </div>

                  <div>
                    <label className="text-xs font-semibold text-gray-600 mb-1 block">
                      Mô tả công việc
                    </label>
                    <textarea
                      disabled={!isEditing}
                      rows={3}
                      value={exp.description}
                      onChange={(e) =>
                        handleExpChange(index, "description", e.target.value)
                      }
                      className={inputClass()}
                    />
                  </div>
                </div>
              ))}
              {experiences.length === 0 && !isEditing && (
                <p className="text-center text-gray-400 text-sm">
                  Chưa có kinh nghiệm làm việc.
                </p>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
