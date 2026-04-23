import { defineConfig } from 'vitepress';

// https://vitepress.dev/reference/site-config
export default defineConfig({
  title: 'cn-holiday-kit',
  description: '中国节假日数据平台 —— 跨平台节假日工具集（TS / Java 8 / Java 25）',
  lang: 'zh-CN',
  lastUpdated: true,
  cleanUrls: true,
  // Build output sits next to the source so it can be picked up by gh-pages workflow.
  outDir: './dist',
  // Local example URLs reference 127.0.0.1/localhost; treat as expected.
  ignoreDeadLinks: [/^https?:\/\/(localhost|127\.0\.0\.1)/],

  themeConfig: {
    nav: [
      { text: '快速开始', link: '/guide/getting-started' },
      { text: 'API 参考', link: '/api/overview' },
      { text: '农历 & 节气', link: '/lunar/overview' },
      { text: 'JDK25 SDK', link: '/sdk-j25/overview' },
      {
        text: '相关链接',
        items: [
          { text: 'GitHub', link: 'https://github.com/gmkits/cn-holiday-kit' },
          { text: 'OpenAPI', link: '/api/openapi' },
        ],
      },
    ],

    sidebar: {
      '/guide/': [
        {
          text: '快速开始',
          items: [
            { text: '介绍', link: '/guide/getting-started' },
            { text: 'TypeScript', link: '/guide/typescript' },
            { text: 'Java 8 兼容层', link: '/guide/java8' },
            { text: 'Java 25 SDK', link: '/guide/java25' },
          ],
        },
      ],
      '/api/': [
        {
          text: 'API 参考',
          items: [
            { text: '总览', link: '/api/overview' },
            { text: '统一响应模型', link: '/api/response-model' },
            { text: '错误码', link: '/api/errors' },
            { text: 'OpenAPI', link: '/api/openapi' },
          ],
        },
      ],
      '/lunar/': [
        {
          text: '农历 & 节气',
          items: [
            { text: '总览', link: '/lunar/overview' },
            { text: '数据格式', link: '/lunar/data-format' },
          ],
        },
      ],
      '/sdk-j25/': [
        {
          text: 'JDK25 SDK',
          items: [
            { text: '总览', link: '/sdk-j25/overview' },
            { text: '虚拟线程与结构化并发', link: '/sdk-j25/virtual-threads' },
          ],
        },
      ],
    },

    socialLinks: [
      { icon: 'github', link: 'https://github.com/gmkits/cn-holiday-kit' },
    ],

    footer: {
      message: 'Released under the Apache-2.0 License.',
      copyright: 'Copyright © 2025 cn-holiday-kit',
    },

    search: {
      provider: 'local',
    },

    outline: {
      label: '本页内容',
      level: [2, 3],
    },

    docFooter: {
      prev: '上一页',
      next: '下一页',
    },
  },
});
