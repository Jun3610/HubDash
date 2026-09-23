import { useSyncExternalStore } from 'react'

// localStorage는 사생활 보호 모드 등에서 예외를 던질 수 있어 모든 접근을 감싼다.
function read<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key)
    return raw === null ? fallback : (JSON.parse(raw) as T)
  } catch {
    return fallback
  }
}

function write<T>(key: string, value: T) {
  try {
    localStorage.setItem(key, JSON.stringify(value))
  } catch {
    // 저장 실패는 조용히 무시한다 (메모리 값은 그대로 유지)
  }
}

export interface Store<T> {
  get(): T
  set(value: T | ((prev: T) => T)): void
  subscribe(listener: () => void): () => void
}

/** localStorage에 저장되는 작은 전역 상태. React에서는 useStore로 구독한다. */
export function createStore<T>(key: string, fallback: T): Store<T> {
  let value = read(key, fallback)
  const listeners = new Set<() => void>()
  return {
    get: () => value,
    set(next) {
      value = typeof next === 'function' ? (next as (prev: T) => T)(value) : next
      write(key, value)
      listeners.forEach((l) => l())
    },
    subscribe(listener) {
      listeners.add(listener)
      return () => listeners.delete(listener)
    },
  }
}

export function useStore<T>(store: Store<T>): T {
  return useSyncExternalStore(store.subscribe, store.get, store.get)
}

/** 저장하지 않는 메모리 전용 전역 상태 */
export function createMemoryStore<T>(initial: T): Store<T> {
  let value = initial
  const listeners = new Set<() => void>()
  return {
    get: () => value,
    set(next) {
      value = typeof next === 'function' ? (next as (prev: T) => T)(value) : next
      listeners.forEach((l) => l())
    },
    subscribe(listener) {
      listeners.add(listener)
      return () => listeners.delete(listener)
    },
  }
}
