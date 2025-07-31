import { useEffect } from 'react'
import { miljoeErDev } from '~utils/miljoe'

export const useLastUmami = () => {
  useEffect(() => {
    const websiteId = miljoeErDev ? 'ffc09260-0a09-48a2-a225-0ad9ae1e1e0a' : '98e556e1-0606-4f3d-a7bd-0c72e8476bb3'

    const script = document.createElement('script')
    script.src = 'https://cdn.nav.no/team-researchops/sporing/sporing.js'
    script.defer = true
    script.setAttribute('data-host-url', 'https://umami.nav.no')
    script.setAttribute('data-website-id', websiteId)

    document.body.appendChild(script)

    return () => {
      try {
        document.body.removeChild(script)
      } catch {
        /* empty */
      }
    }
  }, [])
}
