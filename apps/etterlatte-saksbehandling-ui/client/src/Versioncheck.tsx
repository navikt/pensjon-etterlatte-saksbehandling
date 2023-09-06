import { useEffect, useState } from 'react'
import { hentClientConfig } from '~clientConfig'
import { useAppSelector } from '~store/Store'
import { Alert, BodyLong, Button, Modal } from '@navikt/ds-react'

const Versioncheck = () => {
  const appVersion = useAppSelector((state) => state.appReducer.appversion)
  const [isOutdated, setIsOutdated] = useState<boolean>(false)
  const [timeoutReloadPage, setTimeoutReloadPage] = useState<number | null>(null)

  useEffect(() => {
    const timeout5minutesinms = 300000
    const tisekunderiMs = 10000
    let intervalVersionCheck: null | number = null
    let timeoutReload: null | number = null
    intervalVersionCheck = window.setInterval(() => {
      hentClientConfig().then((res) => {
        if (res.status === 'ok') {
          const fetchedAppversion = res.data.cachebuster
          if (appVersion && fetchedAppversion !== appVersion) {
            setIsOutdated(true)
            const timeoutreftmp = window.setTimeout(() => {
              window.location.reload()
            }, tisekunderiMs)
            setTimeoutReloadPage(timeoutreftmp)
            timeoutReload = timeoutreftmp
          }
        }
      })
    }, timeout5minutesinms)

    return () => {
      timeoutReload && window.clearTimeout(timeoutReload)
      intervalVersionCheck && window.clearTimeout(intervalVersionCheck)
    }
  }, [])

  return (
    <>
      <Modal
        header={{
          label: 'Ny oppdatering',
          heading: 'Utdatert web applikasjon',
          closeButton: false,
        }}
        open={isOutdated}
      >
        <Modal.Body>
          <Alert variant="warning">Web applikasjonen din er utdatert. Laster siden på nytt om 10 sekunder.</Alert>
          <p>
            <BodyLong>
              Har du endringer du ønsker å lagre først? Trykk på avbryt knappen. (Obs! dette kan medføre at du mangler
              enkelte funksjoner og havner i feilsituasjoner!)
            </BodyLong>
          </p>
        </Modal.Body>
        <Modal.Footer>
          <Button
            variant="secondary"
            onClick={() => {
              setIsOutdated(false)
              timeoutReloadPage && window.clearTimeout(timeoutReloadPage)
            }}
          >
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
