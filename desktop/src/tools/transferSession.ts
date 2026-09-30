// Transfer session state for the desktop side of the relay file-transfer
// channel. This complements the `desktop_transfer_*` handlers in
// `handlers/transfer.ts`: the handlers initiate a session, the router routes
// incoming `desktop.transfer_*` envelopes here, and both directions stream
// 256 KB base64 chunks over the existing `desktop` channel.

import { createHash } from 'node:crypto'
import { createWriteStream } from 'node:fs'
import { promises as fs } from 'node:fs'
import * as path from 'node:path'

export const TRANSFER_CHUNK_SIZE = 256 * 1024

export type TransferEmit = (type: string, payload: Record<string, unknown>) => void

interface ReceiveState {
  finalPath: string
  partPath: string
  expectedSha256: string
  totalChunks: number
  received: number
  hasher: ReturnType<typeof createHash>
  stream: ReturnType<typeof createWriteStream>
  emit: TransferEmit
  finalized: boolean
}

const receives = new Map<string, ReceiveState>()

function base64ToBuffer(data: unknown): Buffer {
  if (typeof data !== 'string' || data.length === 0) {
    throw new Error('invalid chunk data')
  }
  return Buffer.from(data, 'base64')
}

/**
 * Begin receiving a pushed file. Creates the `.part` staging path and a
 * write stream; chunks land via {@link appendChunk} and the file is moved
 * into place by {@link finalizeReceive}.
 */
export async function beginReceive(args: {
  transfer_id: string
  path: string
  size: number
  sha256: string
  overwrite: boolean
  emit: TransferEmit
}): Promise<void> {
  const finalPath = path.resolve(args.path)
  const dir = path.dirname(finalPath)
  await fs.mkdir(dir, { recursive: true })

  if (!args.overwrite) {
    await fs.access(finalPath).then(
      () => {
        throw new Error(`destination exists: ${finalPath} (set overwrite to replace)`)
      },
      () => {}
    )
  }

  const hasher = createHash('sha256')
  const stream = createWriteStream(`${finalPath}.${args.transfer_id}.part`, { flags: 'w' })
  receives.set(args.transfer_id, {
    finalPath,
    partPath: `${finalPath}.${args.transfer_id}.part`,
    expectedSha256: args.sha256,
    totalChunks: Math.max(1, Math.ceil(args.size / TRANSFER_CHUNK_SIZE)),
    received: 0,
    hasher,
    stream,
    emit: args.emit,
    finalized: false
  })
}

/**
 * Append one decoded chunk to the in-progress receive. Synchronous — writes
 * are buffered by the write stream and hashing is incremental. Returns the
 * updated counters, or null if the session is unknown.
 */
export function appendChunk(transfer_id: string, data: unknown): { received: number; total: number } | null {
  const state = receives.get(transfer_id)
  if (!state || state.finalized) return null
  const buf = base64ToBuffer(data)
  state.hasher.update(buf)
  state.stream.write(buf)
  state.received += 1
  return { received: state.received, total: state.totalChunks }
}

/**
 * Finalize a receive whose last chunk arrived: flush the stream, verify the
 * whole-file sha256, atomically move into place, and emit transfer_done.
 */
export async function finalizeReceive(transfer_id: string): Promise<void> {
  const state = receives.get(transfer_id)
  if (!state || state.finalized) return
  state.finalized = true

  try {
    await new Promise<void>((resolve, reject) => {
      state.stream.once('error', reject)
      state.stream.end(() => resolve())
    })
    const sha256 = state.hasher.digest('hex')
    if (sha256 !== state.expectedSha256) {
      await fs.rm(state.partPath, { force: true })
      state.emit('desktop.transfer_error', {
        transfer_id,
        message: `sha256 mismatch: expected ${state.expectedSha256}, got ${sha256}`
      })
      return
    }
    await fs.rm(state.finalPath, { force: true })
    await fs.rename(state.partPath, state.finalPath)
    const stat = await fs.stat(state.finalPath)
    state.emit('desktop.transfer_done', { transfer_id, size: stat.size, sha256 })
  } catch (e) {
    await fs.rm(state.partPath, { force: true }).catch(() => {})
    state.emit('desktop.transfer_error', {
      transfer_id,
      message: e instanceof Error ? e.message : String(e)
    })
  } finally {
    receives.delete(transfer_id)
  }
}

/**
 * Abort an in-progress receive and drop its staging file.
 */
export async function abortReceive(transfer_id: string): Promise<void> {
  const state = receives.get(transfer_id)
  if (!state) return
  state.finalized = true
  state.stream.destroy()
  await fs.rm(state.partPath, { force: true }).catch(() => {})
  receives.delete(transfer_id)
}

/**
 * Stream a local file to the relay as `desktop.transfer_chunk` envelopes,
 * computing the whole-file sha256 incrementally. Emits `desktop.transfer_done`
 * on success or `desktop.transfer_error` on failure.
 */
export async function streamSend(args: {
  transfer_id: string
  path: string
  size: number
  emit: TransferEmit
}): Promise<void> {
  const totalChunks = Math.max(1, Math.ceil(args.size / TRANSFER_CHUNK_SIZE))
  const hasher = createHash('sha256')
  const fh = await fs.open(args.path, 'r')
  const buf = Buffer.alloc(TRANSFER_CHUNK_SIZE)
  let sent = 0
  try {
    for (let index = 0; index < totalChunks; index++) {
      const { bytesRead } = await fh.read(buf, 0, TRANSFER_CHUNK_SIZE, index * TRANSFER_CHUNK_SIZE)
      const slice = buf.subarray(0, bytesRead)
      hasher.update(slice)
      args.emit('desktop.transfer_chunk', {
        transfer_id: args.transfer_id,
        index,
        total: totalChunks,
        data: slice.toString('base64')
      })
      sent += bytesRead
    }
    args.emit('desktop.transfer_done', {
      transfer_id: args.transfer_id,
      size: sent,
      sha256: hasher.digest('hex')
    })
  } catch (e) {
    args.emit('desktop.transfer_error', {
      transfer_id: args.transfer_id,
      message: e instanceof Error ? e.message : String(e)
    })
  } finally {
    await fh.close()
  }
}
