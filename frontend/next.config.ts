import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  transpilePackages: ["@open-elements/ui", "@open-elements/nextjs-app-layer"],
};

export default nextConfig;
