import nextJest from "next/jest.js";

const createJestConfig = nextJest({
  // Path to the Next.js app, used to load next.config.mjs and .env files.
  dir: "./",
});

/** @type {import('jest').Config} */
const customJestConfig = {
  setupFilesAfterEnv: ["<rootDir>/jest.setup.ts"],
  testEnvironment: "jsdom",
  moduleNameMapper: {
    "^@/(.*)$": "<rootDir>/src/$1",
  },
  testPathIgnorePatterns: ["<rootDir>/.next/", "<rootDir>/node_modules/", "<rootDir>/e2e/"],
  collectCoverageFrom: [
    "src/**/*.{ts,tsx}",
    "!src/**/*.d.ts",
    "!src/app/**/layout.tsx",
    "!src/app/**/loading.tsx",
  ],
  coverageReporters: ["text", "lcov"],
  reporters: process.env.CI
    ? ["default", ["jest-junit", { outputDirectory: "reports", outputName: "junit.xml" }]]
    : ["default"],
};

// next/jest returns an async function that merges the Next.js-specific config.
export default createJestConfig(customJestConfig);
