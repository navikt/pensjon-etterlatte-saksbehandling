import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { StatusBar } from '~shared/statusbar/Statusbar'
import { Container } from '~shared/styled'
import { SakOversikt } from './SakOversikt'
import Spinner from '~shared/Spinner'
import { mapApiResult, useApiCall } from '~shared/hooks/useApiCall'
import { Tabs } from '@navikt/ds-react'
import { getPerson } from '~shared/api/grunnlag'
import { GYLDIG_FNR } from '~utils/fnr'
import NavigerTilbakeMeny from '~components/person/NavigerTilbakeMeny'
import { BulletListIcon, FileTextIcon } from '@navikt/aksel-icons'
import { Dokumentoversikt } from './dokumenter/dokumentoversikt'
import { ApiErrorAlert } from '~ErrorBoundary'
import { ApiError } from '~shared/api/apiClient'

enum Fane {
  SAKER = 'SAKER',
  DOKUMENTER = 'DOKUMENTER',
}

export const Person = () => {
  const [personStatus, hentPerson] = useApiCall(getPerson)
  const [fane, setFane] = useState(Fane.SAKER)

  const { fnr } = useParams()

  useEffect(() => {
    if (GYLDIG_FNR(fnr)) {
      hentPerson(fnr!!)
    }
  }, [fnr])

  const handleError = (error: ApiError) => {
    if (error.status === 400) {
      return <ApiErrorAlert>Ugyldig forespørsel. Er fødselsnummeret korrekt?</ApiErrorAlert>
    } else if (error.status === 404) {
      return <ApiErrorAlert>Fant ikke person med fødselsnummer {fnr}</ApiErrorAlert>
    } else {
      return <ApiErrorAlert>Feil oppsto ved henting av person med fødselsnummer {fnr}</ApiErrorAlert>
    }
  }

  if (!GYLDIG_FNR(fnr)) {
    return <ApiErrorAlert>Fødselsnummeret {fnr} er ugyldig</ApiErrorAlert>
  }

  return (
    <>
      <StatusBar result={personStatus} />
      <NavigerTilbakeMeny label="Tilbake til oppgavebenken" path="/" />

      {mapApiResult(
        personStatus,
        <Spinner visible label="Laster personinfo ..." />,
        (error) => (
          <Container>{handleError(error)}</Container>
        ),
        (person) => (
          <Tabs value={fane} onChange={(val) => setFane(val as Fane)}>
            <Tabs.List>
              <Tabs.Tab value={Fane.SAKER} label="Sak og behandling" icon={<BulletListIcon title="saker" />} />
              <Tabs.Tab value={Fane.DOKUMENTER} label="Dokumenter" icon={<FileTextIcon title="dokumenter" />} />
            </Tabs.List>
            <Tabs.Panel value={Fane.SAKER}>
              <SakOversikt fnr={person.foedselsnummer} />
            </Tabs.Panel>
            <Tabs.Panel value={Fane.DOKUMENTER}>
              <Dokumentoversikt fnr={person.foedselsnummer} />
            </Tabs.Panel>
          </Tabs>
        )
      )}
    </>
  )
}
