import { useEffect } from 'react'
import { miljoeErDev } from '~utils/miljoe'

const PROD_CONFIG = {
  websiteId: '98e556e1-0606-4f3d-a7bd-0c72e8476bb3',
  umamiUrl: 'https://reops-event-proxy.nav.no',
  umamiScript: 'https://cdn.nav.no/team-researchops/sporing/sporing.js',
} as const

const DEV_CONFIG = {
  websiteId: 'ffc09260-0a09-48a2-a225-0ad9ae1e1e0a',
  umamiUrl: 'https://reops-event-proxy.ekstern.dev.nav.no',
  umamiScript: 'https://cdn.nav.no/team-researchops/sporing/sporing-dev.js',
} as const

export const useLastUmami = () => {
  useEffect(() => {
    const { umamiScript, umamiUrl, websiteId } = miljoeErDev ? DEV_CONFIG : PROD_CONFIG
    const script = document.createElement('script')
    script.src = umamiScript
    script.defer = true
    script.setAttribute('data-host-url', umamiUrl)
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
