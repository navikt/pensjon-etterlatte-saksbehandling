import { useEffect, useState } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { StatusBar } from '~shared/statusbar/Statusbar'
import { Container } from '~shared/styled'
import { SakOversikt } from './SakOversikt'
import Spinner from '~shared/Spinner'
import { mapApiResult, useApiCall } from '~shared/hooks/useApiCall'
import { Tabs } from '@navikt/ds-react'
import { getPerson } from '~shared/api/grunnlag'
import { GYLDIG_FNR } from '~utils/fnr'
import NavigerTilbakeMeny from '~components/person/NavigerTilbakeMeny'
import { BulletListIcon, EnvelopeClosedIcon, FileTextIcon } from '@navikt/aksel-icons'
import { Dokumentoversikt } from './dokumenter/dokumentoversikt'
import { ApiErrorAlert } from '~ErrorBoundary'
import { ApiError } from '~shared/api/apiClient'
import BrevOversikt from '~components/person/brev/BrevOversikt'
import { hentSakMedBehandlnger } from '~shared/api/sak'

enum Fane {
  SAKER = 'SAKER',
  DOKUMENTER = 'DOKUMENTER',
  BREV = 'BREV',
}

export const Person = () => {
  const [search, setSearch] = useSearchParams()

  const [personStatus, hentPerson] = useApiCall(getPerson)
  const [sakStatus, hentSak] = useApiCall(hentSakMedBehandlnger)
  const [fane, setFane] = useState(search.get('fane') || Fane.SAKER)

  const velgFane = (value: string) => {
    const valgtFane = value as Fane

    setSearch({ fane: valgtFane })
    setFane(valgtFane)
  }

  const { fnr } = useParams()

  useEffect(() => {
    if (GYLDIG_FNR(fnr)) {
      hentPerson(fnr!!)
      hentSak(fnr!!)
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
          <Tabs value={fane} onChange={velgFane}>
            <Tabs.List>
              <Tabs.Tab value={Fane.SAKER} label="Sak og behandling" icon={<BulletListIcon />} />
              <Tabs.Tab value={Fane.DOKUMENTER} label="Dokumentoversikt" icon={<FileTextIcon />} />
              <Tabs.Tab value={Fane.BREV} label="Brev" icon={<EnvelopeClosedIcon />} />
            </Tabs.List>

            <Tabs.Panel value={Fane.SAKER}>
              <SakOversikt sakStatus={sakStatus} fnr={person.foedselsnummer} />
            </Tabs.Panel>
            <Tabs.Panel value={Fane.DOKUMENTER}>
              <Dokumentoversikt fnr={person.foedselsnummer} />
            </Tabs.Panel>
            <Tabs.Panel value={Fane.BREV}>
              <BrevOversikt sakStatus={sakStatus} />
            </Tabs.Panel>
          </Tabs>
        )
      )}
    </>
  )
}
