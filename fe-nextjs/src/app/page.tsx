"use client";

import { useState, useEffect } from "react";
import Link from "next/link";

export default function LandingPage() {
  // Không cần state isScrolled cho màu nền nữa vì ta sẽ để nền trắng cố định
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  // Xử lý hiệu ứng Fade-in khi cuộn (Intersection Observer)
  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add("opacity-100", "translate-y-0");
            entry.target.classList.remove("opacity-0", "translate-y-10");
          }
        });
      },
      { threshold: 0.1 },
    );

    const elements = document.querySelectorAll(".animate-on-scroll");
    elements.forEach((el) => observer.observe(el));

    return () => observer.disconnect();
  }, []);

  const scrollToSection = (id: string) => {
    const element = document.getElementById(id);
    if (element) {
      element.scrollIntoView({ behavior: "smooth" });
      setIsMobileMenuOpen(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-800 font-sans selection:bg-indigo-100">
      {/* --- NAVBAR --- */}
      {/* CẬP NHẬT: Luôn hiển thị nền trắng để tránh bị trùng màu với Hero Section */}
      <nav className="fixed top-0 w-full z-50 bg-white/95 backdrop-blur-md shadow-sm transition-all duration-300 py-3">
        <div className="max-w-7xl mx-auto px-6 flex justify-between items-center">
          {/* Logo */}
          <div
            className="flex-shrink-0 flex items-center gap-2 cursor-pointer"
            onClick={() => window.scrollTo({ top: 0, behavior: "smooth" })}
          >
            <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center text-white font-bold">
              C
            </div>
            <span className="font-bold text-xl text-gray-800">CareerMate</span>
          </div>

          {/* Desktop Menu */}
          <div className="hidden md:flex items-center gap-8 font-medium text-slate-600">
            <button
              onClick={() => scrollToSection("features")}
              className="hover:text-indigo-600 transition hover:cursor-pointer"
            >
              Tính năng
            </button>
            <button
              onClick={() => scrollToSection("how-it-works")}
              className="hover:text-indigo-600 transition hover:cursor-pointer"
            >
              Cách hoạt động
            </button>
            <button
              onClick={() => scrollToSection("pricing")}
              className="hover:text-indigo-600 transition hover:cursor-pointer"
            >
              Bảng giá
            </button>
            <Link
              href="/login"
              className="text-indigo-600 border border-indigo-600 px-5 py-2 rounded-full hover:bg-indigo-600 hover:text-white transition"
            >
              Đăng nhập
            </Link>
            <Link
              href="/register"
              className="bg-gradient-to-r from-indigo-500 to-purple-600 text-white px-5 py-2 rounded-full shadow-lg hover:shadow-indigo-500/30 hover:-translate-y-0.5 transition"
            >
              Đăng ký miễn phí
            </Link>
          </div>

          {/* Mobile Menu Button */}
          <button
            className="md:hidden text-2xl text-slate-700 p-2"
            onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
          >
            {isMobileMenuOpen ? "✕" : "☰"}
          </button>
        </div>

        {/* Mobile Menu Dropdown */}
        <div
          className={`md:hidden absolute top-full left-0 w-full bg-white shadow-xl flex flex-col items-center gap-6 py-8 transition-all duration-300 border-t border-gray-100 ${
            isMobileMenuOpen ? "opacity-100 visible" : "opacity-0 invisible"
          }`}
        >
          <button
            onClick={() => scrollToSection("features")}
            className="text-lg font-medium text-slate-700 hover:cursor-pointer"
          >
            Tính năng
          </button>
          <button
            onClick={() => scrollToSection("how-it-works")}
            className="text-lg font-medium text-slate-700 hover:cursor-pointer"
          >
            Cách hoạt động
          </button>
          <button
            onClick={() => scrollToSection("pricing")}
            className="text-lg font-medium text-slate-700 hover:cursor-pointer"
          >
            Bảng giá
          </button>
          <Link href="/login" className="text-indigo-600 font-medium text-lg">
            Đăng nhập
          </Link>
          <Link
            href="/register"
            className="bg-indigo-600 text-white px-8 py-3 rounded-full text-lg"
          >
            Đăng ký ngay
          </Link>
        </div>
      </nav>

      {/* --- HERO SECTION --- */}
      <section className="relative pt-32 pb-20 lg:pt-48 lg:pb-32 overflow-hidden bg-gradient-to-br from-indigo-500 to-purple-600">
        {/* Background Animation Circle */}
        <div className="absolute top-0 right-0 w-full h-full overflow-hidden pointer-events-none">
          <div className="absolute -top-[50%] -right-[50%] w-[100%] h-[100%] bg-white/10 rounded-full animate-pulse scale-150 blur-3xl"></div>
        </div>

        <div className="max-w-7xl mx-auto px-6 flex flex-col lg:flex-row items-center gap-12 relative z-10">
          <div className="flex-1 text-center lg:text-left text-white animate-on-scroll opacity-0 translate-y-10 transition-all duration-1000">
            <h1 className="text-4xl lg:text-6xl font-bold leading-tight mb-6">
              Bạn đồng hành ứng tuyển <br /> thông minh với AI
            </h1>
            <p className="text-lg lg:text-xl text-indigo-100 mb-8 max-w-2xl mx-auto lg:mx-0">
              Nâng tầm sự nghiệp với công nghệ AI - Phân tích CV, Cố vấn nghề
              nghiệp, Phỏng vấn thử và Kết nối việc làm.
            </p>
            <div className="flex flex-wrap justify-center lg:justify-start gap-4">
              <Link
                href="/register"
                className="bg-white text-indigo-600 px-8 py-3 rounded-full font-bold shadow-lg hover:shadow-xl hover:bg-gray-50 transition transform hover:-translate-y-1"
              >
                Bắt đầu miễn phí
              </Link>
              <button
                onClick={() => scrollToSection("how-it-works")}
                className="border-2 border-white text-white px-8 py-3 rounded-full font-bold hover:bg-white/10 transition hover: cursor-pointer"
              >
                Tìm hiểu thêm
              </button>
            </div>
          </div>

          <div className="flex-1 flex justify-center animate-on-scroll opacity-0 translate-y-10 transition-all duration-1000 delay-200">
            {/* SVG Image converted to JSX */}
            <svg
              width="500"
              height="400"
              viewBox="0 0 500 400"
              fill="none"
              className="max-w-full drop-shadow-2xl animate-[float_3s_ease-in-out_infinite]"
            >
              <circle cx="250" cy="200" r="150" fill="rgba(255,255,255,0.1)" />
              <circle cx="250" cy="200" r="120" fill="rgba(255,255,255,0.15)" />
              <rect
                x="150"
                y="120"
                width="200"
                height="160"
                rx="10"
                fill="white"
                opacity="0.95"
              />
              <rect
                x="170"
                y="140"
                width="160"
                height="10"
                rx="5"
                fill="#6366f1"
              />
              <rect
                x="170"
                y="160"
                width="120"
                height="8"
                rx="4"
                fill="#e2e8f0"
              />
              <rect
                x="170"
                y="175"
                width="140"
                height="8"
                rx="4"
                fill="#e2e8f0"
              />
              <rect
                x="170"
                y="200"
                width="160"
                height="30"
                rx="15"
                fill="#6366f1"
              />
            </svg>
          </div>
        </div>
      </section>

      {/* --- STATS SECTION --- */}
      <section className="py-16 bg-white relative -mt-10 mx-6 rounded-3xl shadow-xl z-20 max-w-7xl lg:mx-auto">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-8 px-8">
          {[
            { num: "10K+", label: "Sinh viên tin dùng" },
            { num: "500+", label: "Công ty tuyển dụng" },
            { num: "95%", label: "Tỷ lệ hài lòng" },
            { num: "24/7", label: "Hỗ trợ AI" },
          ].map((stat, idx) => (
            <div
              key={idx}
              className="text-center group hover:-translate-y-2 transition duration-300"
            >
              <div className="text-4xl lg:text-5xl font-bold bg-gradient-to-r from-indigo-500 to-purple-600 bg-clip-text text-transparent mb-2">
                {stat.num}
              </div>
              <div className="text-slate-500 font-medium">{stat.label}</div>
            </div>
          ))}
        </div>
      </section>

      {/* --- FEATURES SECTION --- */}
      <section id="features" className="py-24 bg-slate-50">
        <div className="max-w-7xl mx-auto px-6">
          <div className="text-center mb-16 animate-on-scroll opacity-0 translate-y-10 transition-all duration-700">
            <h2 className="text-4xl font-bold bg-gradient-to-r from-indigo-600 to-purple-600 bg-clip-text text-transparent mb-4">
              Tính năng nổi bật
            </h2>
            <p className="text-xl text-slate-500">
              Công cụ AI toàn diện hỗ trợ hành trình nghề nghiệp của bạn
            </p>
          </div>

          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
            {[
              {
                icon: "📄",
                title: "Phân tích CV bằng AI",
                desc: "Đánh giá chi tiết CV, nhận phản hồi tức thì về cấu trúc và nội dung để tăng cơ hội trúng tuyển.",
              },
              {
                icon: "🤖",
                title: "Chatbot Cố vấn",
                desc: "Tư vấn nghề nghiệp 24/7, gợi ý kỹ năng cần học và lộ trình phát triển bản thân.",
              },
              {
                icon: "💼",
                title: "Sàn việc làm AI",
                desc: "Kết nối trực tiếp với nhà tuyển dụng, gợi ý công việc phù hợp dựa trên hồ sơ của bạn.",
              },
              {
                icon: "🎯",
                title: "Phỏng vấn thử",
                desc: "Luyện tập phỏng vấn với AI, nhận đánh giá chi tiết về câu trả lời và kỹ năng giao tiếp.",
              },
              {
                icon: "📚",
                title: "Gợi ý mẫu CV",
                desc: "Dựa trên CV hiện có của bạn, thiết kế lại CV với các mẫu chuyên nghiệp và phù hợp ngành nghề.",
              },
              {
                icon: "🏆",
                title: "Gamification",
                desc: "Hoàn thành thử thách, nhận huy hiệu và cạnh tranh trên bảng xếp hạng.",
              },
            ].map((feature, idx) => (
              <div
                key={idx}
                className="animate-on-scroll opacity-0 translate-y-10 transition-all duration-700 bg-white p-8 rounded-2xl shadow-sm hover:shadow-xl hover:-translate-y-2 border border-transparent hover:border-indigo-100 transition group"
              >
                <div className="w-16 h-16 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-2xl flex items-center justify-center text-3xl text-white mb-6 group-hover:scale-110 transition">
                  {feature.icon}
                </div>
                <h3 className="text-xl font-bold text-slate-800 mb-3">
                  {feature.title}
                </h3>
                <p className="text-slate-500 leading-relaxed">{feature.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* --- HOW IT WORKS --- */}
      <section id="how-it-works" className="py-24 bg-white">
        <div className="max-w-7xl mx-auto px-6">
          <div className="text-center mb-16">
            <h2 className="text-4xl font-bold text-slate-800 mb-4 ">
              Cách hoạt động
            </h2>
            <p className="text-xl text-slate-500">4 bước đơn giản để bắt đầu</p>
          </div>

          <div className="grid md:grid-cols-4 gap-8">
            {[
              {
                step: 1,
                title: "Đăng ký",
                desc: "Tạo tài khoản miễn phí chỉ trong vài phút",
              },
              {
                step: 2,
                title: "Upload CV",
                desc: "Nhận phân tích chi tiết từ AI ngay lập tức",
              },
              {
                step: 3,
                title: "Tư vấn AI",
                desc: "Nhận lộ trình sự nghiệp và gợi ý việc làm",
              },
              {
                step: 4,
                title: "Ứng tuyển",
                desc: "Kết nối với nhà tuyển dụng và nhận việc",
              },
            ].map((item, idx) => (
              <div
                key={idx}
                className="animate-on-scroll opacity-0 translate-y-10 transition-all duration-700 text-center relative"
              >
                <div className="w-20 h-20 mx-auto bg-gradient-to-br from-indigo-500 to-purple-600 rounded-full flex items-center justify-center text-3xl font-bold text-white mb-6 shadow-lg shadow-indigo-200">
                  {item.step}
                </div>
                <h3 className="text-xl font-bold text-slate-800 mb-2">
                  {item.title}
                </h3>
                <p className="text-slate-500">{item.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* --- PRICING (CẬP NHẬT THEO GÓI VIP) --- */}
      <section id="pricing" className="py-24 bg-slate-50">
        <div className="max-w-7xl mx-auto px-6">
          <div className="text-center mb-16">
            <h2 className="text-4xl font-bold text-slate-800 mb-4">
              Bảng giá linh hoạt
            </h2>
            <p className="text-xl text-slate-500">
              Các gói dịch vụ dành cho Ứng viên và Nhà tuyển dụng
            </p>
          </div>

          <div className="grid md:grid-cols-3 gap-8 items-start">
            {/* 1. Gói Cơ bản (Free) */}
            <div className="bg-white p-8 rounded-2xl shadow-sm border border-slate-100 animate-on-scroll opacity-0 translate-y-10 transition-all duration-700">
              <h3 className="text-2xl font-bold text-slate-800 mb-2">Cơ bản</h3>
              <div className="text-4xl font-bold text-slate-600 mb-1">0₫</div>
              <p className="text-slate-400 mb-6">Mãi mãi miễn phí</p>
              <div className="text-xs font-bold uppercase tracking-wide text-slate-400 mb-4">
                Dành cho Ứng viên
              </div>
              <ul className="space-y-4 mb-8 text-slate-500 text-sm">
                {[
                  "Tạo hồ sơ năng lực",
                  "Phân tích CV cơ bản",
                  "Tìm kiếm việc làm",
                  "Chat với AI (Giới hạn)",
                ].map((feat, i) => (
                  <li key={i} className="flex items-center gap-2">
                    <span className="text-slate-400 font-bold">✓</span> {feat}
                  </li>
                ))}
              </ul>
              <Link
                href="/register"
                className="block w-full py-3 border-2 border-slate-200 text-slate-600 font-bold text-center rounded-xl hover:bg-slate-50 transition"
              >
                Đăng ký ngay
              </Link>
            </div>

            {/* 2. Gói Candidate Pro (200k) */}
            <div className="bg-white p-10 rounded-2xl shadow-2xl border-2 border-indigo-500 relative transform md:scale-105 z-10 animate-on-scroll opacity-0 translate-y-10 transition-all duration-700 delay-100">
              <div className="absolute top-0 right-0 bg-gradient-to-r from-indigo-500 to-purple-600 text-white text-xs font-bold px-3 py-1 rounded-bl-lg rounded-tr-lg uppercase tracking-wider">
                Ứng viên VIP
              </div>
              <h3 className="text-2xl font-bold text-slate-800 mb-2">
                Candidate Pro
              </h3>
              <div className="text-5xl font-bold bg-gradient-to-r from-indigo-600 to-purple-600 bg-clip-text text-transparent mb-1">
                200K
              </div>
              <p className="text-slate-400 mb-6">/ 30 ngày</p>

              <ul className="space-y-4 mb-8 text-slate-600 text-sm">
                {[
                  "AI Phân tích & Chấm điểm CV chi tiết",
                  "Phỏng vấn thử 1-1 với AI (Mock Interview)",
                  "Huy hiệu Ứng viên Tài năng (VIP)",
                ].map((feat, i) => (
                  <li key={i} className="flex items-start gap-2">
                    <span className="text-indigo-500 font-bold text-lg leading-none">
                      ✓
                    </span>{" "}
                    {feat}
                  </li>
                ))}
              </ul>
              <Link
                href="/register?role=CANDIDATE"
                className="block w-full py-4 bg-gradient-to-r from-indigo-500 to-purple-600 text-white font-bold text-center rounded-xl shadow-lg hover:shadow-indigo-500/40 hover:-translate-y-1 transition"
              >
                Nâng cấp ngay
              </Link>
            </div>

            {/* 3. Gói Recruiter Premium (100k) */}
            <div className="bg-white p-8 rounded-2xl shadow-sm border border-orange-200 animate-on-scroll opacity-0 translate-y-10 transition-all duration-700 delay-200">
              <h3 className="text-2xl font-bold text-slate-800 mb-2">
                Recruiter Premium
              </h3>
              <div className="text-4xl font-bold text-orange-600 mb-1">
                100K
              </div>
              <p className="text-slate-400 mb-6">/ 30 ngày</p>
              <div className="text-xs font-bold uppercase tracking-wide text-orange-500 mb-4">
                Dành cho Nhà tuyển dụng
              </div>
              <ul className="space-y-4 mb-8 text-slate-500 text-sm">
                {["Phân tích hồ sơ ứng viên bằng AI"].map((feat, i) => (
                  <li key={i} className="flex items-start gap-2">
                    <span className="text-orange-500 font-bold">✓</span> {feat}
                  </li>
                ))}
              </ul>
              <Link
                href="/register?role=RECRUITER"
                className="block w-full py-3 border-2 border-orange-500 text-orange-600 font-bold text-center rounded-xl hover:bg-orange-50 transition"
              >
                Dùng thử ngay
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* --- CTA SECTION --- */}
      <section className="py-20 bg-gradient-to-r from-indigo-600 to-purple-600 text-white text-center px-6">
        <h2 className="text-3xl md:text-5xl font-bold mb-6">
          Sẵn sàng bắt đầu hành trình nghề nghiệp?
        </h2>
        <p className="text-xl text-indigo-100 mb-10">
          Tham gia cùng hàng nghìn sinh viên đã thành công với CareerMate
        </p>
        <Link
          href="/register"
          className="inline-block bg-white text-indigo-600 px-10 py-4 rounded-full font-bold text-lg shadow-xl hover:shadow-2xl hover:bg-gray-50 transition transform hover:-translate-y-1"
        >
          Đăng ký miễn phí ngay
        </Link>
      </section>

      {/* --- FOOTER --- */}
      <footer className="bg-slate-900 text-slate-300 py-16">
        <div className="max-w-7xl mx-auto px-6 pt-8 border-t border-slate-800 text-center text-slate-500">
          &copy; 2026 CareerMate. All rights reserved.
        </div>
      </footer>
    </div>
  );
}
