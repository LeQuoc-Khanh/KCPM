import type { Config } from "tailwindcss";

const config: Config = {
  // Trong Tailwind v4, 'content' được tự động phát hiện, 
  // nhưng bạn có thể giữ lại nếu muốn chỉ định rõ ràng hoặc dùng v3-compat.
  // Nếu dùng v4 thuần, bạn có thể bỏ dòng này.
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      backgroundImage: {
        "gradient-radial": "radial-gradient(var(--tw-gradient-stops))",
        "gradient-conic":
          "conic-gradient(from 180deg at 50% 50%, var(--tw-gradient-stops))",
      },
    },
  },
  plugins: [],
};
export default config;