// 后端地址。微信开发者工具需勾选「不校验合法域名」才能访问 localhost。
// 真机调试请改成局域网 IP（如 http://192.168.1.x:8080）或线上域名。
// #ifdef H5
// H5 走同源相对路径，由 vite 代理转发到后端（手机经任意 IP/隧道访问均可用）
export const BASE_URL = ''
// #endif
// #ifndef H5
export const BASE_URL = 'http://192.168.1.93:8080'
// #endif
