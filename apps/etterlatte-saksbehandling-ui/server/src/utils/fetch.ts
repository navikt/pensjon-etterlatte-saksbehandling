export const fetch = (...args) => import('node-fetch').then(({ default: fetch }) => fetch(...args))
