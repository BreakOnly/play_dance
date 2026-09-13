// 后端地址。微信开发者工具需勾选「不校验合法域名」才能访问 localhost。
// 开发者工具默认走 localhost（换网络/IP 不受影响）；真机调试改成电脑当前局域网 IP
// （查询：ipconfig，如 http://192.168.1.93:8080）或线上域名。
// #ifdef H5
// H5 走同源相对路径，由 vite 代理转发到后端（手机经任意 IP/隧道访问均可用）
export const BASE_URL = ''
// #endif
// #ifndef H5
export const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
// #endif
