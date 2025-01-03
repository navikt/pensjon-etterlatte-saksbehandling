import { useEffect, useState } from 'react'
import { hentClientConfig } from '~clientConfig'
import { useAppSelector } from '~store/Store'
import { Alert, Button, Modal } from '@navikt/ds-react'

const Versioncheck = () => {
  const appVersion = useAppSelector((state) => state.appReducer.appversion)
  const [isOutdated, setIsOutdated] = useState<boolean>(false)
  const [timeoutReloadPage, setTimeoutReloadPage] = useState<number | null>(null)

  useEffect(() => {
    const timeout5minutesinms = 300000
    const trettiSekunderiMs = 30000
    let intervalVersionCheck: null | number = null
    let timeoutReload: null | number = null
    intervalVersionCheck = window.setInterval(() => {
      hentClientConfig().then((res) => {
        if (res.ok) {
          const fetchedAppversion = res.data.cachebuster
          if (appVersion && fetchedAppversion !== appVersion) {
            setIsOutdated(true)
            const timeoutreftmp = window.setTimeout(() => {
              window.location.reload()
            }, trettiSekunderiMs)
            setTimeoutReloadPage(timeoutreftmp)
            timeoutReload = timeoutreftmp
          }
        }
      })
    }, timeout5minutesinms)

    return () => {
      if (timeoutReload) window.clearTimeout(timeoutReload)
      if (intervalVersionCheck) window.clearTimeout(intervalVersionCheck)
    }
  }, [])

  const onAvbryt = () => {
    setIsOutdated(false)
    if (timeoutReloadPage) window.clearTimeout(timeoutReloadPage)
  }

  return (
    <>
      <Modal
        header={{
          label: 'Ny oppdatering',
          heading: 'Utdatert web applikasjon',
          closeButton: false,
        }}
        open={isOutdated}
        onClose={onAvbryt}
      >
        <Modal.Body>
          <Alert variant="warning">
            Det er kommet en ny versjon av Gjenny. Nettsida vil automatisk laste på nytt om 30 sekunder for å hente inn
            den nyeste versjonen. Har du endringer du ønsker å lagre først? Trykk på avbryt knappen. Denne boksen vil da
            komme opp igjen om fem minutter. (Obs! dette kan medføre at du mangler enkelte funksjoner og havner i
            feilsituasjoner!)
          </Alert>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={onAvbryt}>
            Avbryt oppdatering
          </Button>
          <Button variant="primary" onClick={() => window.location.reload()}>
            Last siden på nytt nå
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  )
}

export default Versioncheck
