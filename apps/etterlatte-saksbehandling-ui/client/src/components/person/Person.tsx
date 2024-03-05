import { useEffect, useState } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { PdlPersonStatusBar } from '~shared/statusbar/Statusbar'
import { Container } from '~shared/styled'
import { SakOversikt } from './SakOversikt'
import Spinner from '~shared/Spinner'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Tabs } from '@navikt/ds-react'
import { fnrHarGyldigFormat } from '~utils/fnr'
import NavigerTilbakeMeny from '~components/person/NavigerTilbakeMeny'
import { BulletListIcon, EnvelopeClosedIcon, FileTextIcon } from '@navikt/aksel-icons'
import { ApiErrorAlert } from '~ErrorBoundary'
import { ApiError } from '~shared/api/apiClient'
import BrevOversikt from '~components/person/brev/BrevOversikt'
import { hentSakMedBehandlnger } from '~shared/api/sak'

import { mapAllApiResult, mapSuccess } from '~shared/api/apiUtils'
import { Dokumentliste } from '~components/person/dokumenter/Dokumentliste'
import { hentPersonNavn } from '~shared/api/pdltjenester'

export enum PersonOversiktFane {
  SAKER = 'SAKER',
  DOKUMENTER = 'DOKUMENTER',
  BREV = 'BREV',
}

export const Person = () => {
  const [search, setSearch] = useSearchParams()

  const [personStatus, hentPerson] = useApiCall(hentPersonNavn)
  const [sakStatus, hentSak] = useApiCall(hentSakMedBehandlnger)
  const [fane, setFane] = useState(search.get('fane') || PersonOversiktFane.SAKER)

  const velgFane = (value: string) => {
    const valgtFane = value as PersonOversiktFane

    setSearch({ fane: valgtFane })
    setFane(valgtFane)
  }

  const { fnr } = useParams()

  useEffect(() => {
    if (fnrHarGyldigFormat(fnr)) {
      hentSak(fnr!!)
    }
  }, [fnr])

  useEffect(() => {
    hentPerson(fnr!!)
  }, [])

  const handleError = (error: ApiError) => {
    if (error.status === 400) {
      return <ApiErrorAlert>Ugyldig forespørsel: {error.detail}</ApiErrorAlert>
    } else {
      return <ApiErrorAlert>Feil oppsto ved henting av person med fødselsnummer {fnr}</ApiErrorAlert>
    }
  }

  if (!fnrHarGyldigFormat(fnr)) {
    return <ApiErrorAlert>Fødselsnummeret {fnr} har et ugyldig format (ikke 11 siffer)</ApiErrorAlert>
  }

  return (
    <>
      {mapSuccess(personStatus, (person) => (
        <PdlPersonStatusBar person={person} />
      ))}

      <NavigerTilbakeMeny label="Tilbake til oppgavebenken" path="/" />
      {mapAllApiResult(
        personStatus,
        <Spinner visible label="Laster personinfo ..." />,
        null,
        (error) => (
          <Container>{handleError(error)}</Container>
        ),
        (person) => (
          <Tabs value={fane} onChange={velgFane}>
            <Tabs.List>
              <Tabs.Tab value={PersonOversiktFane.SAKER} label="Sak og behandling" icon={<BulletListIcon />} />
              <Tabs.Tab value={PersonOversiktFane.DOKUMENTER} label="Dokumentoversikt" icon={<FileTextIcon />} />
              <Tabs.Tab value={PersonOversiktFane.BREV} label="Brev" icon={<EnvelopeClosedIcon />} />
            </Tabs.List>

            <Tabs.Panel value={PersonOversiktFane.SAKER}>
              <SakOversikt sakStatus={sakStatus} fnr={person.foedselsnummer} />
            </Tabs.Panel>
            <Tabs.Panel value={PersonOversiktFane.DOKUMENTER}>
              <Dokumentliste fnr={person.foedselsnummer} />
            </Tabs.Panel>
            <Tabs.Panel value={PersonOversiktFane.BREV}>
              <BrevOversikt sakStatus={sakStatus} />
            </Tabs.Panel>
          </Tabs>
        )
      )}
    </>
  )
}
