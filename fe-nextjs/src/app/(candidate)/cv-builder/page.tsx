"use client";

import React, { useState, useRef } from "react";
import html2canvas from "html2canvas";
import jsPDF from "jspdf";
import {
  Layout,
  Plus,
  Trash2,
  Upload,
  User as UserIcon,
  MapPin,
  Phone,
  Mail,
  Download,
  Loader2,
  Info,
  Lightbulb,
} from "lucide-react";
import { uploadAvatar } from "@/services/candidateService";

// --- TYPES ---
interface Experience {
  company: string;
  role: string;
  duration: string;
  description: string;
}

interface Education {
  school: string;
  degree: string;
  year: string;
}

interface CVData {
  avatarUrl: string;
  fullName: string;
  email: string;
  phone: string;
  address: string;
  summary: string;
  skills: string;
  experiences: Experience[];
  education: Education[];
}

const initialData: CVData = {
  avatarUrl: "",
  fullName: "Nguyễn Văn A",
  email: "nguyenvana@email.com",
  phone: "0909 123 456",
  address: "Quận 1, TP. Hồ Chí Minh",
  summary:
    "Lập trình viên Full-stack với 3 năm kinh nghiệm chuyên sâu về Java Spring Boot và ReactJS. Có khả năng làm việc độc lập và làm việc nhóm tốt.",
  skills: "Java Core, Spring Boot, Hibernate, ReactJS, Next.js, MySQL, Git",
  experiences: [
    {
      company: "Công ty Công nghệ ABC",
      role: "Java Developer",
      duration: "01/2023 - Nay",
      description:
        "- Phát triển hệ thống microservices.\n- Tối ưu hóa database.",
    },
  ],
  education: [
    {
      school: "Đại học Bách Khoa TP.HCM",
      degree: "Kỹ sư CNTT",
      year: "2017 - 2021",
    },
  ],
};

export default function CVBuilderPage() {
  const [cvData, setCvData] = useState<CVData>(initialData);
  const [template, setTemplate] = useState<"MODERN" | "CLASSIC">("MODERN");
  const [activeTab, setActiveTab] = useState<"GENERAL" | "EXP" | "EDU">(
    "GENERAL",
  );
  const [isExporting, setIsExporting] = useState(false);

  const componentRef = useRef<HTMLDivElement>(null);

  // --- HÀM TẢI PDF ---
  const handleDownloadPDF = async () => {
    if (!componentRef.current) return;
    setIsExporting(true);

    try {
      const element = componentRef.current;

      // Cấu hình html2canvas để tránh lỗi màu
      const canvas = await html2canvas(element, {
        scale: 2, // Tăng độ nét
        useCORS: true, // Hỗ trợ tải ảnh từ nguồn ngoài
        logging: false,
        backgroundColor: "#ffffff", // Ép nền trắng
        imageTimeout: 0,
      });

      const imgData = canvas.toDataURL("image/png");
      const pdf = new jsPDF({
        orientation: "portrait",
        unit: "mm",
        format: "a4",
      });

      const imgWidth = 210;
      const imgHeight = (canvas.height * imgWidth) / canvas.width;

      pdf.addImage(imgData, "PNG", 0, 0, imgWidth, imgHeight);

      const fileName = `CV_${cvData.fullName.replace(/\s+/g, "_")}_${new Date().getTime()}.pdf`;
      pdf.save(fileName);
    } catch (error) {
      console.error("Lỗi xuất PDF:", error);
      alert("Có lỗi khi tạo file PDF. Vui lòng thử lại.");
    } finally {
      setIsExporting(false);
    }
  };

  // --- HÀM XỬ LÝ KHÁC (Giữ nguyên) ---
  const handleAvatarUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files || e.target.files.length === 0) return;
    try {
      const file = e.target.files[0];
      const newAvatarUrl = await uploadAvatar(file);
      setCvData({ ...cvData, avatarUrl: newAvatarUrl });
    } catch (error) {
      alert("Lỗi upload ảnh (Backend cần đang chạy).");
    }
  };

  const updateItem = (
    section: "experiences" | "education",
    index: number,
    field: string,
    value: string,
  ) => {
    const newList = [...cvData[section]] as any[];
    newList[index][field] = value;
    setCvData({ ...cvData, [section]: newList });
  };

  const addItem = (section: "experiences" | "education") => {
    const newItem =
      section === "experiences"
        ? { company: "", role: "", duration: "", description: "" }
        : { school: "", degree: "", year: "" };
    setCvData({ ...cvData, [section]: [...cvData[section], newItem] });
  };

  const removeItem = (section: "experiences" | "education", index: number) => {
    const newList = [...cvData[section]];
    newList.splice(index, 1);
    setCvData({ ...cvData, [section]: newList });
  };

  const renderDescriptionList = (description: string) => {
    if (!description) return null;
    const items = description
      .split("\n")
      .filter((item) => item.trim().length > 0);
    return (
      <ul className="list-disc ml-5 space-y-1">
        {items.map((item, index) => (
          <li key={index}>{item.replace(/^-+\s*/, "")}</li>
        ))}
      </ul>
    );
  };

  // --- TEMPLATES ---
  // LƯU Ý: Đã thay thế toàn bộ class màu (vd: text-blue-900) thành mã HEX (vd: text-[#1e3a8a])
  // để tương thích với html2canvas + Tailwind v4.

  const ModernTemplate = () => (
    <div className="p-8 bg-white h-full font-sans min-h-[297mm] text-[#1f2937]">
      {" "}
      {/* text-gray-800 */}
      <div className="flex gap-6 mb-8 border-b-2 border-[#dbeafe] pb-6 items-center">
        {" "}
        {/* border-blue-100 */}
        {cvData.avatarUrl ? (
          <img
            src={cvData.avatarUrl}
            alt="Avatar"
            className="w-32 h-32 rounded-full object-cover border-4 border-[#eff6ff] flex-shrink-0"
            crossOrigin="anonymous"
          />
        ) : (
          <div className="w-32 h-32 rounded-full bg-[#eff6ff] border-4 border-[#dbeafe] flex items-center justify-center flex-shrink-0">
            <span className="text-4xl font-bold text-[#93c5fd]">
              {cvData.fullName.charAt(0).toUpperCase()}
            </span>
          </div>
        )}
        <div className="flex-1 flex flex-col">
          <h1 className="text-4xl font-bold text-[#1e3a8a] uppercase tracking-wide leading-tight">
            {cvData.fullName}
          </h1>{" "}
          {/* text-blue-900 */}
          <div className="text-[#4b5563] mt-3 grid grid-cols-1 md:grid-cols-2 gap-y-1 gap-x-4 text-sm">
            {" "}
            {/* text-gray-600 */}
            <span className="flex items-center gap-1">
              <Mail size={14} className="text-[#3b82f6]" /> {cvData.email}
            </span>{" "}
            {/* text-blue-500 */}
            <span className="flex items-center gap-1">
              <Phone size={14} className="text-[#3b82f6]" /> {cvData.phone}
            </span>
            <span className="flex items-center gap-1 md:col-span-2">
              <MapPin size={14} className="text-[#3b82f6]" /> {cvData.address}
            </span>
          </div>
        </div>
      </div>
      <div className="grid grid-cols-3 gap-8">
        <div className="col-span-1 space-y-8">
          <section>
            <h3 className="text-lg font-bold text-[#1e40af] border-b border-[#bfdbfe] mb-3 pb-1 uppercase">
              Giới thiệu
            </h3>{" "}
            {/* text-blue-800 border-blue-200 */}
            <p className="text-sm text-[#374151] leading-relaxed text-justify">
              {cvData.summary}
            </p>{" "}
            {/* text-gray-700 */}
          </section>
          <section>
            <h3 className="text-lg font-bold text-[#1e40af] border-b border-[#bfdbfe] mb-3 pb-1 uppercase">
              Kỹ năng
            </h3>
            <div className="flex flex-wrap gap-2">
              {cvData.skills.split(",").map((s, i) =>
                s.trim() ? (
                  <span
                    key={i}
                    className="bg-[#eff6ff] text-[#1d4ed8] px-3 py-1 rounded-md text-sm font-medium"
                  >
                    {s.trim()}
                  </span>
                ) : null,
              )}{" "}
              {/* bg-blue-50 text-blue-700 */}
            </div>
          </section>
          <section>
            <h3 className="text-lg font-bold text-[#1e40af] border-b border-[#bfdbfe] mb-3 pb-1 uppercase">
              Học vấn
            </h3>
            {cvData.education.map((edu, i) => (
              <div key={i} className="mb-4 last:mb-0">
                <p className="font-bold text-[#1f2937]">{edu.degree}</p>{" "}
                {/* text-gray-800 */}
                <p className="text-sm font-medium text-[#1d4ed8]">
                  {edu.school}
                </p>{" "}
                {/* text-blue-700 */}
                <p className="text-xs text-[#6b7280] italic mt-0.5">
                  {edu.year}
                </p>{" "}
                {/* text-gray-500 */}
              </div>
            ))}
          </section>
        </div>
        <div className="col-span-2">
          <h3 className="text-xl font-bold text-[#1e40af] border-b border-[#bfdbfe] mb-5 pb-1 uppercase">
            Kinh nghiệm làm việc
          </h3>
          <div className="space-y-6">
            {cvData.experiences.map((exp, i) => (
              <div
                key={i}
                className="relative pl-5 border-l-2 border-[#bfdbfe]"
              >
                {" "}
                {/* border-blue-200 */}
                <div className="absolute -left-[7px] top-1.5 w-3 h-3 rounded-full bg-[#3b82f6] border-2 border-white"></div>{" "}
                {/* bg-blue-500 */}
                <div className="flex justify-between items-start mb-1">
                  <h4 className="font-bold text-lg text-[#1f2937] leading-tight">
                    {exp.role}
                  </h4>{" "}
                  {/* text-gray-800 */}
                  <span className="text-xs font-semibold text-[#1e40af] bg-[#eff6ff] px-2 py-1 rounded whitespace-nowrap ml-2">
                    {exp.duration}
                  </span>{" "}
                  {/* text-blue-800 bg-blue-50 */}
                </div>
                <p className="text-[#1d4ed8] font-semibold text-[15px] mb-2">
                  {exp.company}
                </p>{" "}
                {/* text-blue-700 */}
                <div className="text-sm text-[#374151] leading-relaxed text-justify">
                  {" "}
                  {/* text-gray-700 */}
                  {renderDescriptionList(exp.description)}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );

  const ClassicTemplate = () => (
    <div className="p-12 bg-white h-full text-black font-serif leading-relaxed min-h-[297mm]">
      <div className="text-center mb-8">
        <h1 className="text-4xl font-bold uppercase mb-2 tracking-wider text-[#111827]">
          {cvData.fullName}
        </h1>{" "}
        {/* text-gray-900 */}
        <div className="flex justify-center items-center gap-3 text-sm text-[#374151] italic flex-wrap">
          {" "}
          {/* text-gray-700 */}
          <span>{cvData.address}</span>
          <span className="text-[#d1d5db]">•</span> {/* text-gray-300 */}
          <span>{cvData.phone}</span>
          <span className="text-[#d1d5db]">•</span>
          <span>{cvData.email}</span>
        </div>
      </div>
      <hr className="border-t-2 border-[#1f2937] mb-6" />{" "}
      {/* border-gray-800 */}
      {cvData.summary && (
        <section className="mb-6">
          <h2 className="text-lg font-bold uppercase border-b border-[#9ca3af] pb-1 mb-3 text-[#1f2937]">
            Mục tiêu nghề nghiệp
          </h2>{" "}
          {/* border-gray-400 text-gray-800 */}
          <p className="text-justify text-[15px]">{cvData.summary}</p>
        </section>
      )}
      <section className="mb-6">
        <h2 className="text-lg font-bold uppercase border-b border-[#9ca3af] pb-1 mb-4 text-[#1f2937]">
          Kinh nghiệm làm việc
        </h2>
        <div className="space-y-5">
          {cvData.experiences.map((exp, i) => (
            <div key={i}>
              <div className="flex justify-between items-baseline mb-1">
                <h3 className="font-bold text-[16px]">
                  {exp.role}{" "}
                  <span className="font-normal text-[#374151]"> tại </span>{" "}
                  <span className="italic font-semibold">{exp.company}</span>
                </h3>
                <span className="text-sm font-semibold text-[#4b5563] whitespace-nowrap">
                  {exp.duration}
                </span>
              </div>
              <div className="text-[15px] text-[#1f2937] pl-2">
                {renderDescriptionList(exp.description)}
              </div>
            </div>
          ))}
        </div>
      </section>
      {cvData.skills && (
        <section className="mb-6">
          <h2 className="text-lg font-bold uppercase border-b border-[#9ca3af] pb-1 mb-3 text-[#1f2937]">
            Kỹ năng chuyên môn
          </h2>
          <p className="text-[15px] leading-7">
            {cvData.skills.split(",").map((skill, i, arr) => {
              const s = skill.trim();
              if (!s) return null;
              return (
                <span key={i}>
                  {s}
                  {i < arr.length - 1 ? (
                    <span className="mx-2 text-[#9ca3af]">•</span>
                  ) : (
                    ""
                  )}
                </span>
              );
            })}
          </p>
        </section>
      )}
      <section className="mb-6">
        <h2 className="text-lg font-bold uppercase border-b border-[#9ca3af] pb-1 mb-3 text-[#1f2937]">
          Học vấn & Bằng cấp
        </h2>
        <div className="space-y-3">
          {cvData.education.map((edu, i) => (
            <div key={i} className="flex justify-between items-baseline">
              <div>
                <h3 className="font-bold text-[16px]">{edu.degree}</h3>
                <p className="text-[15px] italic text-[#374151]">
                  {edu.school}
                </p>
              </div>
              <span className="text-sm font-semibold text-[#4b5563] whitespace-nowrap">
                {edu.year}
              </span>
            </div>
          ))}
        </div>
      </section>
    </div>
  );

  return (
    <div className="flex h-[calc(100vh-64px)] bg-gray-100 font-sans">
      {/* LEFT: EDITOR */}
      <div className="w-[420px] flex-shrink-0 bg-white border-r flex flex-col shadow-xl z-20">
        <div className="p-4 border-b bg-gray-50">
          <div className="flex justify-between items-center mb-3">
            <h2 className="font-bold text-gray-800 flex items-center gap-2 text-lg">
              <Layout size={20} className="text-blue-600" /> Trình tạo CV
            </h2>
          </div>
          <div>
            <label className="block text-xs font-semibold text-gray-500 uppercase mb-2">
              Chọn mẫu thiết kế
            </label>
            <div className="grid grid-cols-2 gap-2 text-sm">
              <button
                onClick={() => setTemplate("MODERN")}
                className={`px-3 py-2 rounded-md border-2 font-medium transition-all ${template === "MODERN" ? "border-blue-500 bg-blue-50 text-blue-700" : "border-gray-200 hover:border-blue-300 text-gray-600"}`}
              >
                Hiện đại
              </button>
              <button
                onClick={() => setTemplate("CLASSIC")}
                className={`px-3 py-2 rounded-md border-2 font-medium transition-all ${template === "CLASSIC" ? "border-gray-800 bg-gray-100 text-gray-900 font-serif" : "border-gray-200 hover:border-gray-400 text-gray-600 font-serif"}`}
              >
                Cổ điển
              </button>
            </div>
          </div>
        </div>

        <div className="flex border-b text-sm font-semibold bg-white sticky top-0 z-10">
          <button
            onClick={() => setActiveTab("GENERAL")}
            className={`flex-1 py-3 transition-colors ${activeTab === "GENERAL" ? "text-blue-600 border-b-2 border-blue-600 bg-blue-50/50" : "text-gray-500 hover:text-gray-700"}`}
          >
            Thông tin
          </button>
          <button
            onClick={() => setActiveTab("EXP")}
            className={`flex-1 py-3 transition-colors ${activeTab === "EXP" ? "text-blue-600 border-b-2 border-blue-600 bg-blue-50/50" : "text-gray-500 hover:text-gray-700"}`}
          >
            Kinh nghiệm
          </button>
          <button
            onClick={() => setActiveTab("EDU")}
            className={`flex-1 py-3 transition-colors ${activeTab === "EDU" ? "text-blue-600 border-b-2 border-blue-600 bg-blue-50/50" : "text-gray-500 hover:text-gray-700"}`}
          >
            Học vấn
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-5 space-y-5 bg-gray-50/30 scrollbar-thin scrollbar-thumb-gray-300 scrollbar-track-transparent">
          {activeTab === "GENERAL" && (
            <div className="space-y-4 animate-in fade-in slide-in-from-left-2 duration-200">
              <div className="flex items-center gap-4 p-3 bg-white rounded-lg border shadow-sm">
                <div className="relative w-16 h-16 bg-gray-100 rounded-full overflow-hidden border-2 border-gray-200 flex-shrink-0">
                  {cvData.avatarUrl ? (
                    <img
                      src={cvData.avatarUrl}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <UserIcon className="w-8 h-8 text-gray-400 absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2" />
                  )}
                </div>
                <div>
                  <label className="cursor-pointer bg-white border border-gray-300 text-gray-700 px-3 py-1.5 rounded-md text-sm font-medium hover:bg-gray-50 transition inline-flex items-center shadow-sm">
                    <Upload size={14} className="mr-2" /> Chọn ảnh thẻ
                    <input
                      type="file"
                      className="hidden"
                      accept="image/*"
                      onChange={handleAvatarUpload}
                    />
                  </label>
                  <p className="text-[11px] text-gray-400 mt-1 italic">
                    Khuyên dùng ảnh tỉ lệ 1:1, nền sáng
                  </p>
                </div>
              </div>

              <div className="space-y-3">
                <div>
                  <label className="text-xs font-semibold text-gray-600 uppercase mb-1 block">
                    Họ và tên <span className="text-red-500">*</span>
                  </label>
                  <input
                    className="w-full border border-gray-300 p-2.5 rounded-md text-sm focus:ring-2 focus:ring-blue-200 focus:border-blue-500 transition"
                    placeholder="VD: Nguyễn Văn A"
                    value={cvData.fullName}
                    onChange={(e) =>
                      setCvData({ ...cvData, fullName: e.target.value })
                    }
                  />
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="text-xs font-semibold text-gray-600 uppercase mb-1 block">
                      Email
                    </label>
                    <input
                      className="w-full border border-gray-300 p-2.5 rounded-md text-sm"
                      placeholder="email@example.com"
                      value={cvData.email}
                      onChange={(e) =>
                        setCvData({ ...cvData, email: e.target.value })
                      }
                    />
                  </div>
                  <div>
                    <label className="text-xs font-semibold text-gray-600 uppercase mb-1 block">
                      Số điện thoại
                    </label>
                    <input
                      className="w-full border border-gray-300 p-2.5 rounded-md text-sm"
                      placeholder="0909 xxx xxx"
                      value={cvData.phone}
                      onChange={(e) =>
                        setCvData({ ...cvData, phone: e.target.value })
                      }
                    />
                  </div>
                </div>
                <div>
                  <label className="text-xs font-semibold text-gray-600 uppercase mb-1 block">
                    Địa chỉ
                  </label>
                  <input
                    className="w-full border border-gray-300 p-2.5 rounded-md text-sm"
                    placeholder="Quận, Thành phố (VD: TP.HCM)"
                    value={cvData.address}
                    onChange={(e) =>
                      setCvData({ ...cvData, address: e.target.value })
                    }
                  />
                </div>

                <div>
                  <div className="flex justify-between items-center mb-1">
                    <label className="text-xs font-semibold text-gray-600 uppercase">
                      Giới thiệu bản thân
                    </label>
                    <span className="text-[10px] text-blue-600 bg-blue-50 px-2 py-0.5 rounded-full flex items-center gap-1">
                      <Info size={10} /> Nên viết 2-3 câu
                    </span>
                  </div>
                  <textarea
                    className="w-full border border-gray-300 p-2.5 rounded-md text-sm h-28 focus:ring-2 focus:ring-blue-200 focus:border-blue-500 resize-none"
                    placeholder="Tóm tắt kinh nghiệm..."
                    value={cvData.summary}
                    onChange={(e) =>
                      setCvData({ ...cvData, summary: e.target.value })
                    }
                  />
                </div>

                <div>
                  <div className="flex justify-between items-center mb-1">
                    <label className="text-xs font-semibold text-gray-600 uppercase">
                      Kỹ năng
                    </label>
                    <span className="text-[10px] text-gray-500 italic">
                      Phân cách bằng dấu phẩy (,)
                    </span>
                  </div>
                  <textarea
                    className="w-full border border-gray-300 p-2.5 rounded-md text-sm h-20 focus:ring-2 focus:ring-blue-200 focus:border-blue-500 resize-none"
                    placeholder="VD: Java, Spring Boot, ReactJS..."
                    value={cvData.skills}
                    onChange={(e) =>
                      setCvData({ ...cvData, skills: e.target.value })
                    }
                  />
                </div>
              </div>
            </div>
          )}

          {activeTab === "EXP" && (
            <div className="space-y-5 animate-in fade-in slide-in-from-right-2 duration-200">
              <div className="bg-blue-50 p-3 rounded-lg border border-blue-100 flex items-start gap-2">
                <Lightbulb
                  size={16}
                  className="text-blue-600 mt-0.5 flex-shrink-0"
                />
                <p className="text-xs text-blue-800">
                  <span className="font-bold">Mẹo:</span> Sử dụng các động từ
                  mạnh (Phát triển, Thiết kế...) và con số cụ thể.
                </p>
              </div>

              {cvData.experiences.map((exp, idx) => (
                <div
                  key={idx}
                  className="bg-white p-4 rounded-lg border border-gray-200 shadow-sm relative group hover:border-blue-300 transition"
                >
                  <button
                    onClick={() => removeItem("experiences", idx)}
                    className="absolute top-3 right-3 text-gray-400 hover:text-red-500 p-1.5 rounded-full hover:bg-red-50 transition"
                  >
                    <Trash2 size={16} />
                  </button>
                  <div className="space-y-3">
                    <div className="grid grid-cols-3 gap-3">
                      <div className="col-span-2">
                        <label className="text-xs font-medium text-gray-500 uppercase mb-1 block">
                          Công ty / Dự án
                        </label>
                        <input
                          className="w-full border border-gray-300 p-2 rounded-md text-sm"
                          placeholder="Tên công ty"
                          value={exp.company}
                          onChange={(e) =>
                            updateItem(
                              "experiences",
                              idx,
                              "company",
                              e.target.value,
                            )
                          }
                        />
                      </div>
                      <div>
                        <label className="text-xs font-medium text-gray-500 uppercase mb-1 block">
                          Thời gian
                        </label>
                        <input
                          className="w-full border border-gray-300 p-2 rounded-md text-sm text-center"
                          placeholder="MM/YYYY - Nay"
                          value={exp.duration}
                          onChange={(e) =>
                            updateItem(
                              "experiences",
                              idx,
                              "duration",
                              e.target.value,
                            )
                          }
                        />
                      </div>
                    </div>
                    <div>
                      <label className="text-xs font-medium text-gray-500 uppercase mb-1 block">
                        Vị trí / Chức danh
                      </label>
                      <input
                        className="w-full border border-gray-300 p-2 rounded-md text-sm font-medium"
                        placeholder="VD: Backend Developer"
                        value={exp.role}
                        onChange={(e) =>
                          updateItem("experiences", idx, "role", e.target.value)
                        }
                      />
                    </div>
                    <div>
                      <label className="text-xs font-medium text-gray-500 uppercase mb-1 block">
                        Mô tả chi tiết
                      </label>
                      <textarea
                        className="w-full border border-gray-300 p-2 rounded-md text-sm h-32 focus:ring-2 focus:ring-blue-200 resize-none font-mono text-[13px] leading-snug"
                        placeholder="- Mô tả trách nhiệm chính&#10;- Thành tựu đạt được..."
                        value={exp.description}
                        onChange={(e) =>
                          updateItem(
                            "experiences",
                            idx,
                            "description",
                            e.target.value,
                          )
                        }
                      />
                      <p className="text-[10px] text-gray-400 mt-1 text-right">
                        Xuống dòng để tạo gạch đầu dòng mới
                      </p>
                    </div>
                  </div>
                </div>
              ))}
              <button
                onClick={() => addItem("experiences")}
                className="w-full py-3 border-2 border-dashed border-gray-300 text-gray-500 rounded-lg hover:bg-blue-50 hover:border-blue-300 hover:text-blue-600 flex items-center justify-center gap-2 text-sm font-bold transition uppercase tracking-wide"
              >
                <Plus size={18} /> Thêm kinh nghiệm
              </button>
            </div>
          )}

          {activeTab === "EDU" && (
            <div className="space-y-5 animate-in fade-in slide-in-from-right-2 duration-200">
              {cvData.education.map((edu, idx) => (
                <div
                  key={idx}
                  className="bg-white p-4 rounded-lg border border-gray-200 shadow-sm relative group hover:border-blue-300 transition"
                >
                  <button
                    onClick={() => removeItem("education", idx)}
                    className="absolute top-3 right-3 text-gray-400 hover:text-red-500 p-1.5 rounded-full hover:bg-red-50 transition"
                  >
                    <Trash2 size={16} />
                  </button>
                  <div className="space-y-3">
                    <div>
                      <label className="text-xs font-medium text-gray-500 uppercase mb-1 block">
                        Trường học / Tổ chức
                      </label>
                      <input
                        className="w-full border border-gray-300 p-2 rounded-md text-sm font-medium"
                        placeholder="Tên trường..."
                        value={edu.school}
                        onChange={(e) =>
                          updateItem("education", idx, "school", e.target.value)
                        }
                      />
                    </div>
                    <div className="grid grid-cols-3 gap-3">
                      <div className="col-span-2">
                        <label className="text-xs font-medium text-gray-500 uppercase mb-1 block">
                          Bằng cấp / Chuyên ngành
                        </label>
                        <input
                          className="w-full border border-gray-300 p-2 rounded-md text-sm"
                          placeholder="VD: Cử nhân CNTT"
                          value={edu.degree}
                          onChange={(e) =>
                            updateItem(
                              "education",
                              idx,
                              "degree",
                              e.target.value,
                            )
                          }
                        />
                      </div>
                      <div>
                        <label className="text-xs font-medium text-gray-500 uppercase mb-1 block">
                          Niên khóa
                        </label>
                        <input
                          className="w-full border border-gray-300 p-2 rounded-md text-sm text-center"
                          placeholder="YYYY - YYYY"
                          value={edu.year}
                          onChange={(e) =>
                            updateItem("education", idx, "year", e.target.value)
                          }
                        />
                      </div>
                    </div>
                  </div>
                </div>
              ))}
              <button
                onClick={() => addItem("education")}
                className="w-full py-3 border-2 border-dashed border-gray-300 text-gray-500 rounded-lg hover:bg-blue-50 hover:border-blue-300 hover:text-blue-600 flex items-center justify-center gap-2 text-sm font-bold transition uppercase tracking-wide"
              >
                <Plus size={18} /> Thêm học vấn
              </button>
            </div>
          )}
        </div>

        {/* Footer Actions */}
        <div className="p-4 border-t bg-white">
          <button
            onClick={handleDownloadPDF}
            disabled={isExporting}
            className="w-full bg-gradient-to-r from-blue-600 to-blue-700 text-white py-3 rounded-lg font-bold text-base hover:from-blue-700 hover:to-blue-800 flex items-center justify-center gap-3 shadow-md hover:shadow-lg transition transform hover:-translate-y-0.5 disabled:opacity-70 disabled:cursor-not-allowed"
          >
            {isExporting ? (
              <Loader2 className="animate-spin" size={20} />
            ) : (
              <Download size={20} />
            )}
            {isExporting ? "Đang tạo file PDF..." : "Tải xuống PDF"}
          </button>
          <p className="text-center text-xs text-gray-500 mt-2 italic">
            File PDF sẽ được lưu trực tiếp về máy tính của bạn.
          </p>
        </div>
      </div>

      {/* RIGHT: PREVIEW */}
      <div className="flex-1 bg-gray-100 p-8 overflow-y-auto flex justify-center relative">
        <div className="absolute top-4 right-4 bg-white/80 backdrop-blur-sm px-3 py-1.5 rounded-full text-xs font-medium text-gray-600 shadow-sm border z-10">
          Xem trước (Khổ A4)
        </div>
        {/* Container CV */}
        <div
          className="w-[210mm] min-h-[297mm] bg-white origin-top transform scale-[0.85] lg:scale-[0.9] xl:scale-[1] transition-transform duration-300 mt-8"
          ref={componentRef}
        >
          {template === "MODERN" ? <ModernTemplate /> : <ClassicTemplate />}
        </div>
      </div>
    </div>
  );
}
