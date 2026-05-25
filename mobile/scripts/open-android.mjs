import { rm } from 'node:fs/promises'
import { spawn } from 'node:child_process'

const isWindows = process.platform === 'win32'
const npmCmd = 'npm'
const npxCmd = 'npx'
const androidWebAssets = 'android/app/src/main/assets/public'

function run(command, args) {
  return new Promise((resolve, reject) => {
    const child = isWindows
      ? spawn('cmd.exe', ['/d', '/s', '/c', command, ...args], { stdio: 'inherit' })
      : spawn(command, args, { stdio: 'inherit' })
    child.on('error', reject)
    child.on('exit', code => {
      if (code === 0) {
        resolve()
      } else {
        reject(new Error(`${command} ${args.join(' ')} failed with exit code ${code}`))
      }
    })
  })
}

await run(npmCmd, ['run', 'build'])
await rm(androidWebAssets, { recursive: true, force: true })
await run(npxCmd, ['cap', 'sync', 'android'])
await run(npxCmd, ['cap', 'open', 'android'])
