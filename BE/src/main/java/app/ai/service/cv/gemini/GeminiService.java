package app.ai.service.cv.gemini;

import app.ai.service.cv.gemini.dto.GeminiResponse;
import app.ai.service.cv.gemini.dto.MatchResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final ObjectMapper objectMapper;
    private final GeminiApiClient geminiApiClient;

    // 👇 ĐỊNH NGHĨA CÁC MỨC NHIỆT ĐỘ CHUẨN
    private static final float TEMP_STRICT = 0.0f;     // Nghiêm túc tuyệt đối (JSON, Chấm điểm)
    private static final float TEMP_ANALYTICAL = 0.2f; // Phân tích logic (So khớp CV)
    private static final float TEMP_BALANCED = 0.5f;   // Cân bằng (Phỏng vấn chuyên nghiệp)
    /**
     * CHỨC NĂNG 1: Phân tích CV (Raw Text -> JSON Profile)
     */
    public GeminiResponse parseCV(String rawText) {
        // [DEBUG LOG 1] Kiểm tra text đầu vào
        log.info("=== START PARSING CV ===");
        log.info("Raw Text Length: {}", rawText != null ? rawText.length() : 0);

        String prompt = """
              Bạn là một trợ lý nhân sự chuyên nghiệp (HR Assistant).
              Nhiệm vụ: Trích xuất thông tin từ văn bản CV dưới đây thành JSON hợp lệ.

              NỘI DUNG CV:
              %s

              YÊU CẦU:
              - Chỉ trả về JSON hợp lệ, không thêm lời chào, không thêm Markdown.
              - JSON phải theo đúng cấu trúc sau (Chú ý dấu phẩy và ngoặc):

              {
                "contact": {
                  "name": "Họ tên đầy đủ",
                  "email": "Email",
                  "phoneNumber": "Số điện thoại",
                  "address": "Địa chỉ",
                  "linkedIn": "Link LinkedIn"
                },
                "skills": ["Kỹ năng A", "Kỹ năng B"],
                "experiences": [
                  {
                    "company": "Tên công ty",
                    "role": "Vị trí",
                    "startDate": "Time bắt đầu",
                    "endDate": "Time kết thúc",
                    "description": "Mô tả"
                  }
                ],
                "aboutMe": "Trích xuất đoạn giới thiệu/Summary/About Me/Profile/Objective. Nếu không có mục riêng, hãy tự tóm tắt ngắn gọn năng lực ứng viên."
              }
              """.formatted(rawText);

        return parseResponse(prompt, GeminiResponse.class, TEMP_STRICT);
    };



    /**
     * CHỨC NĂNG 2: Tách Skill từ Job Description
     */
    public List<String> extractSkillsFromJob(String jobDescription, String jobRequirements) {
       String prompt = """
                You are an expert Job Analyst. Extract technical and soft skills from the Job Description below.
                Return ONLY a JSON Array of strings (e.g., ["Java", "Teamwork", "SQL"]).
                Do not include generic words like "Experience", "Degree". Keep skills concise.
                JOB TITLE & DESCRIPTION:
                %s
                REQUIREMENTS:
                %s
                """.formatted(jobDescription, jobRequirements);

        try {
            String jsonString = geminiApiClient.generateContent(prompt, TEMP_STRICT);
           return objectMapper.readValue(jsonString, new TypeReference<List<String>>(){});
        } catch (Exception e) {
            log.error("Lỗi tách skill từ Job: ", e);
            return Collections.emptyList();
        }
    }

    /**
     * CHỨC NĂNG 3: Chấm điểm & Gợi ý lộ trình (All-in-One)
     */
    public MatchResult matchCVWithJob(String cvText, String jobDescription, String jobRequirements) {
       String prompt = """
                Bạn là Chuyên gia Tuyển dụng (HR Tech). Hãy phân tích CV so với JD và phân loại kỹ năng vào 5 NHÓM riêng biệt.
                --- CẢNH BÁO QUAN TRỌNG ---
                  BẠN LÀ MỘT API TRẢ VỀ DỮ LIỆU. KHÔNG ĐƯỢC CHÀO HỎI. KHÔNG ĐƯỢC GIẢI THÍCH.
                  CHỈ TRẢ VỀ DUY NHẤT MỘT KHỐI JSON HỢP LỆ.
                  VIỆC THÊM BẤT KỲ VĂN BẢN NÀO NGOÀI JSON SẼ LÀM HỎNG HỆ THỐNG.
                --- LOGIC PHÂN LOẠI 5 CỘT (BẮT BUỘC) ---
                1. **matchedSkillsList** (ĐÁP ỨNG): 
                  - Kỹ năng (Cả Cứng & Mềm) mà Job YÊU CẦU và CV ĐÃ CÓ.

                2. **missingSkillsList** (THIẾU):
                  - Kỹ năng (Cả Cứng & Mềm) mà Job YÊU CẦU nhưng CV KHÔNG CÓ.

                3. **otherHardSkillsList** (CHUYÊN MÔN KHÁC):
                  - Kỹ năng CHUYÊN MÔN (Hard Skills/Tech Stack/Công cụ) mà CV CÓ nhưng Job KHÔNG yêu cầu.
                  - Ví dụ: Job cần Java, CV có thêm Python -> Python vào đây.

                4. **otherSoftSkillsList** (KỸ NĂNG MỀM KHÁC):
                  - Kỹ năng MỀM (Soft Skills/Ngôn ngữ/Thái độ) mà CV CÓ nhưng Job KHÔNG yêu cầu.
                  - Ví dụ: Leadership, English, Teamwork (nếu Job không ghi).

                5. **recommendedSkillsList** (GỢI Ý THÊM):
                  - Các kỹ năng (Cứng hoặc Mềm) mà CẢ Job và CV ĐỀU KHÔNG CÓ.
                  - NHƯNG bạn (AI) thấy cần thiết cho vị trí này trong thực tế công việc hiện đại.
                  - BẮT BUỘC phải gợi ý ít nhất 3 kỹ năng, gồm cả Hard Skills (ví dụ: CI/CD, Cloud, Monitoring, Security) và Soft Skills (ví dụ: Communication, Critical Thinking, Time Management).
                  - Nếu không chắc, hãy đưa ra gợi ý phổ biến trong ngành liên quan hoặc các kỹ năng mềm phổ biến.


                --- DỮ LIỆU ĐẦU VÀO ---
                [JOB]
                %s
                %s
                [CV]
                %s

                --- OUTPUT JSON ---
                {
                  "matchPercentage": (0-100),
                  "totalRequiredSkills": (int),
                  
                  "matchedSkillsCount": (int),
                  "matchedSkillsList": ["A", "B"],

                  "missingSkillsCount": (int),
                  "missingSkillsList": ["C"],

                  "otherHardSkillsCount": (int),
                  "otherHardSkillsList": ["D"],

                  "otherSoftSkillsCount": (int),
                  "otherSoftSkillsList": ["E"],

                  "recommendedSkillsCount": (int),
                  "recommendedSkillsList": ["F", "G"],

                  "evaluation": "Nhận xét tiếng Việt...",
                  "learningPath": "Lộ trình học tập (Markdown Tiếng Việt)...",
                  "careerAdvice": "Lời khuyên (Tiếng Việt)..."
                }
                """.formatted(jobDescription, jobRequirements, cvText);

       return parseResponse(prompt, MatchResult.class,TEMP_ANALYTICAL);
    }

    /**
     * CHỨC NĂNG: Chat thông thường (Không lưu DB)
     */
    public String chatWithAI(String userMessage) {
        // Prompt định hình tính cách cho AI
        String prompt = """
                Bạn là một trợ lý ảo AI thông minh và hữu ích.
                Nhiệm vụ của bạn là trả lời câu hỏi của người dùng một cách ngắn gọn, chính xác.
                
                YÊU CẦU:
                - Trả lời bằng Tiếng Việt.
                - Nếu là câu hỏi lập trình, hãy định dạng code bằng Markdown.
                - Giữ giọng văn thân thiện, chuyên nghiệp.
                
                CÂU HỎI CỦA NGƯỜI DÙNG:
                %s
                """.formatted(userMessage);

        
        return callAiChat(prompt);
    }

    public String callAiChat(String prompt) {
        return geminiApiClient.generateContent(prompt, TEMP_BALANCED);
    }

    // --- HÀM HELPER ---
   private <T> T parseResponse(String prompt, Class<T> responseType, float temperature) {
        try {
            String jsonResponse = geminiApiClient.generateContent(prompt, temperature);
            return objectMapper.readValue(jsonResponse, responseType);
        } catch (Exception e) {
            log.error("Lỗi parse dữ liệu AI: ", e);
            throw new RuntimeException("AI Error: " + e.getMessage());
        }
    }

    /**
     * [MỚI] CHỨC NĂNG: OCR thông minh (Ảnh -> Text có cấu trúc)
     * Dùng để xử lý CV dạng ảnh (PNG, JPG)
     */
    public String convertImageToText(byte[] imageBytes, String mimeType) {
        String prompt = """
                Bạn là một công cụ OCR chuyên dụng cho CV (Hồ sơ xin việc).
                Nhiệm vụ: Trích xuất TOÀN BỘ chữ trong hình ảnh này.
                
                YÊU CẦU QUAN TRỌNG:
                1. Giữ nguyên cấu trúc phân đoạn (Header, Kinh nghiệm, Kỹ năng, Học vấn).
                2. Không tóm tắt, phải lấy chi tiết từng gạch đầu dòng.
                3. Chỉ trả về văn bản thô (Plain Text), không thêm Markdown (```), không thêm lời dẫn.
                4. Nếu ảnh mờ hoặc không phải CV, hãy cố gắng đọc hết mức có thể.
                """;
        
        // Dùng nhiệt độ thấp để OCR chính xác nhất
        return geminiApiClient.generateContentWithImage(prompt, imageBytes, mimeType, 0.1f);
    }
}