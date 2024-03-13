import { useEffect } from 'react'

export const useSidetittel = (tittel: string) => {
  useEffect(() => {
    document.title = `Gjenny - ${tittel}`
  }, [])
}
