export default [
    {
        ignores: ["node_modules/**", "target/**"]
    },
    {
        files: ["**/*.js", "**/*.mjs"],
        languageOptions: {
            ecmaVersion: "latest",
            sourceType: "module"
        },
        rules: {
            "no-var": "error",
            "prefer-const": "error"
        }
    },
    {
        files: ["src/main/html/scripts/*.js"],
        languageOptions: {
            sourceType: "script"
        }
    }
];
