"use client";

import { useEffect, useState } from "react";
import { Star, User } from "lucide-react";
import { reviewService, Review } from "@/services/reviewService";
import { useAuth } from "@/context/Authcontext";
import toast from "react-hot-toast";

interface Props {
  companyId: number;
}

export default function CompanyReviews({ companyId }: Props) {
  const { user } = useAuth();
  const isAuthenticated = !!user;

  // Xác định vai trò người dùng (Cập nhật theo logic đề xuất)
  const isCandidate = user?.userRole === 'CANDIDATE' || user?.userRole === 'CANDIDATE_VIP';

  const [reviews, setReviews] = useState<Review[]>([]);
  const [average, setAverage] = useState(0);
  const [rating, setRating] = useState(0);
  const [comment, setComment] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    loadData();
  }, [companyId]);

  const loadData = async () => {
    try {
      const [list, avg] = await Promise.all([
        reviewService.getReviews(companyId),
        reviewService.getAverageRating(companyId)
      ]);
      setReviews(list);
      setAverage(avg);
    } catch (error) {
      console.error("Lỗi tải đánh giá", error);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (rating === 0) return toast.error("Vui lòng chọn số sao!");

    setIsSubmitting(true);
    try {
      await reviewService.createReview({ companyId, rating, comment });
      toast.success("Cảm ơn đánh giá của bạn!");
      setComment("");
      setRating(0);
      loadData();
    } catch (error: any) {
      toast.error(error?.response?.data?.message || "Lỗi khi gửi đánh giá");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="bg-white rounded-xl shadow-sm border p-6 mt-6">
      <h2 className="text-xl font-bold text-gray-900 mb-6 flex items-center gap-2">
        Đánh giá & Nhận xét
        <span className="text-sm font-normal text-gray-500 bg-gray-100 px-2 py-1 rounded-full">
          {reviews.length} lượt
        </span>
      </h2>

      {/* Điểm trung bình */}
      <div className="flex items-center gap-4 mb-8 bg-indigo-50 p-4 rounded-lg">
        <div className="text-4xl font-bold text-indigo-600">{average}</div>
        <div>
          <div className="flex text-yellow-400">
            {[1, 2, 3, 4, 5].map((star) => (
              <Star key={star} size={20} fill={star <= Math.round(average) ? "currentColor" : "none"} />
            ))}
          </div>
          <div className="text-sm text-gray-500 mt-1">Dựa trên {reviews.length} đánh giá</div>
        </div>
      </div>

      {/* Logic hiển thị Form hoặc Thông báo */}
      {isAuthenticated ? (
        isCandidate ? (
          /* Form dành cho Ứng viên */
          <form onSubmit={handleSubmit} className="mb-10 border-b pb-8">
            <h3 className="font-semibold mb-3">Viết đánh giá của bạn</h3>
            <div className="flex gap-2 mb-4">
              {[1, 2, 3, 4, 5].map((star) => (
                <button
                  key={star}
                  type="button"
                  onClick={() => setRating(star)}
                  className={`transition-colors ${rating >= star ? "text-yellow-400" : "text-gray-300 hover:text-yellow-200"}`}
                >
                  <Star size={28} fill="currentColor" />
                </button>
              ))}
            </div>
            <textarea
              className="w-full border rounded-lg p-3 focus:ring-2 focus:ring-indigo-500 outline-none"
              rows={4}
              placeholder="Chia sẻ trải nghiệm của bạn về công ty này..."
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              required
            />
            <button
              disabled={isSubmitting}
              className="mt-3 bg-indigo-600 text-white px-6 py-2 rounded-lg hover:bg-indigo-700 disabled:opacity-50"
            >
              {isSubmitting ? "Đang gửi..." : "Gửi đánh giá"}
            </button>
          </form>
        ) : (
          /* Thông báo dành cho Nhà tuyển dụng hoặc vai trò khác */
          <div className="bg-blue-50 p-4 rounded-lg text-center mb-8 border border-blue-100">
            <p className="text-blue-700 font-medium">Bạn đang xem đánh giá với tư cách Nhà tuyển dụng.</p>
            <p className="text-blue-500 text-sm">Chỉ ứng viên mới có quyền đăng bài đánh giá.</p>
          </div>
        )
      ) : (
        /* Thông báo cho người dùng chưa đăng nhập */
        <div className="bg-gray-50 p-4 rounded-lg text-center mb-8">
          <p className="text-gray-600">Vui lòng <span className="font-bold">Đăng nhập</span> để viết đánh giá.</p>
        </div>
      )}

      {/* Danh sách đánh giá */}
      <div className="space-y-6">
        {reviews.map((review) => (
          <div key={review.id} className="flex gap-4 pb-6 border-b last:border-0">
            <div className="w-10 h-10 bg-gray-200 rounded-full flex items-center justify-center overflow-hidden flex-shrink-0">
              {review.reviewerAvatar ? (
                <img src={review.reviewerAvatar} alt="User" className="w-full h-full object-cover" />
              ) : (
                <User size={20} className="text-gray-500" />
              )}
            </div>
            <div className="flex-1">
              <div className="flex justify-between items-start">
                <h4 className="font-bold text-gray-900">{review.reviewerName || "Người dùng ẩn danh"}</h4>
                <span className="text-sm text-gray-400">
                  {new Date(review.createdAt).toLocaleDateString("vi-VN")}
                </span>
              </div>
              <div className="flex text-yellow-400 my-1">
                {[...Array(5)].map((_, i) => (
                  <Star key={i} size={14} fill={i < review.rating ? "currentColor" : "none"} />
                ))}
              </div>
              <p className="text-gray-700 mt-2">{review.comment}</p>
            </div>
          </div>
        ))}
        {reviews.length === 0 && (
          <p className="text-center text-gray-500 py-4">Chưa có đánh giá nào. Hãy là người đầu tiên!</p>
        )}
      </div>
    </div>
  );
}