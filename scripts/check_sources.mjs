import fs from 'node:fs'
import path from 'node:path'
import { execFileSync } from 'node:child_process'
const root = path.resolve(import.meta.dirname, '..')
const pages = JSON.parse(fs.readFileSync(path.join(root, 'mini_program/src/pages.json'), 'utf8'))
const tracked = new Set(execFileSync('git', ['ls-files', '-z'], { cwd: root, encoding: 'utf8' }).split('\0'))
const missing = pages.pages.map(p => `mini_program/src/${p.path}.vue`).filter(p => !fs.existsSync(path.join(root, p)))
if (missing.length) throw new Error(`Missing page source: ${missing.join(', ')}`)
if (process.env.CI) {
  const untracked = pages.pages.map(p => `mini_program/src/${p.path}.vue`).filter(p => !tracked.has(p))
  if (untracked.length) throw new Error(`Page source not committed: ${untracked.join(', ')}`)
}
const route = fs.readFileSync(path.join(root, 'front-end/src/router/index.js'), 'utf8')
for (const file of route.matchAll(/@\/views\/([^']+)/g)) if (!fs.existsSync(path.join(root, 'front-end/src/views', file[1]))) throw new Error(`Missing route: ${file[1]}`)
console.log(`Source manifest OK: ${pages.pages.length} mini-program pages`)
