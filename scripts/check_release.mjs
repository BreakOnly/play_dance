const base = process.env.VITE_API_BASE_URL || ''
if (!/^https:\/\//.test(base)) throw new Error('Release requires VITE_API_BASE_URL=https://your-api-domain')
const host = new URL(base).hostname
if (host === 'localhost' || host === '127.0.0.1' || host === '0.0.0.0') throw new Error('Release cannot use a loopback API URL')
console.log('Release API configuration validated')
