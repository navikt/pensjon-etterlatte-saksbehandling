import React, { useEffect, useState } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { PdlPersonStatusBar } from '~shared/statusbar/Statusbar'
import { SakOversikt } from './sakOgBehandling/SakOversikt'
import Spinner from '~shared/Spinner'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Box, Tabs } from '@navikt/ds-react'
import { fnrHarGyldigFormat } from '~utils/fnr'
import NavigerTilbakeMeny from '~components/person/NavigerTilbakeMeny'
import {
  BellIcon,
  BulletListIcon,
  CogRotationIcon,
  EnvelopeClosedIcon,
  FileTextIcon,
  PersonIcon,
} from '@navikt/aksel-icons'
import { ApiErrorAlert } from '~ErrorBoundary'
import { ApiError } from '~shared/api/apiClient'
import BrevOversikt from '~components/person/brev/BrevOversikt'
import { hentSak, hentSakMedBehandlnger } from '~shared/api/sak'
import { isSuccess, mapAllApiResult, mapFailure, mapSuccess, Result } from '~shared/api/apiUtils'
import { Dokumentliste } from '~components/person/dokumenter/Dokumentliste'
import { hentPersonNavnogFoedsel } from '~shared/api/pdltjenester'
import { SamordningSak } from '~components/person/SamordningSak'
import { SakMedBehandlinger } from '~components/person/typer'
import { SakType } from '~shared/types/sak'
import { Personopplysninger } from '~components/person/personopplysninger/Personopplysninger'
import { useSidetittel } from '~shared/hooks/useSidetittel'
import { Hendelser } from '~components/person/hendelser/Hendelser'
import NotatOversikt from '~components/person/notat/NotatOversikt'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import { settSak } from '~store/reducers/SakReducer'
import { useAppDispatch } from '~store/Store'
import { GetFnrFromSessionStorage } from '~components/oppgavebenk/components/PersonoversiktLenke'

export enum PersonOversiktFane {
  PERSONOPPLYSNINGER = 'PERSONOPPLYSNINGER',
  SAKER = 'SAKER',
  DOKUMENTER = 'DOKUMENTER',
  BREV = 'BREV',
  NOTATER = 'NOTATER',
  SAMORDNING = 'SAMORDNING',
  HENDELSER = 'HENDELSER',
}

export const Person = () => {
  useSidetittel('Personoversikt')
  const dispatch = useAppDispatch()

  const { sakId } = useParams()
  const [search, setSearch] = useSearchParams()
  const [personNavnResult, personNavnFetch] = useApiCall(hentPersonNavnogFoedsel)
  const [sakMedBehandlingResult, sakMedBehandlingFetch] = useApiCall(hentSakMedBehandlnger)
  const [sakResult, sakFetch] = useApiCall(hentSak)

  const [fane, setFane] = useState(search.get('fane') || PersonOversiktFane.SAKER)
  const skalViseNotater = useFeatureEnabledMedDefault('notater', false)

  const fnr = GetFnrFromSessionStorage()

  const velgFane = (value: string) => {
    const valgtFane = value as PersonOversiktFane
    setSearch({ fane: valgtFane })
    setFane(valgtFane)
  }

  useEffect(() => {
    sakFetch(Number(sakId))
  }, [sakId])

  useEffect(() => {
    if (isSuccess(sakResult)) {
      dispatch(settSak(sakResult.data))

      const ident = sakResult.data.ident
      if (fnrHarGyldigFormat(ident)) {
        personNavnFetch(ident)
        sakMedBehandlingFetch(ident)
      }
    }
  }, [sakResult])

  useEffect(() => {
    if (sakId == null && fnr && fnrHarGyldigFormat(fnr)) {
      personNavnFetch(fnr)
      sakMedBehandlingFetch(fnr)
    }
  }, [fnr, sakId])

  const handleError = (error: ApiError) => {
    if (error.status === 400) {
      return <ApiErrorAlert>Ugyldig forespørsel: {error.detail}</ApiErrorAlert>
    } else {
      return <ApiErrorAlert>Feil oppsto ved henting av sak</ApiErrorAlert>
    }
  }

  const isOmstillingsstoenad = (sakStatus: Result<SakMedBehandlinger>) => {
    return isSuccess(sakStatus) && sakStatus.data.sak.sakType === SakType.OMSTILLINGSSTOENAD
  }

  return (
    <>
      {mapSuccess(personNavnResult, (person) => (
        <PdlPersonStatusBar person={person} saksId={Number(sakId)} />
      ))}

      <NavigerTilbakeMeny label="Tilbake til oppgavebenken" path="/" />

      {mapAllApiResult(
        personNavnResult,
        <Spinner visible label="Laster personinfo ..." />,
        null,
        (error) => (
          <Box padding="8">{handleError(error)}</Box>
        ),
        (person) => (
          <Tabs value={fane} onChange={velgFane}>
            <Tabs.List>
              <Tabs.Tab value={PersonOversiktFane.SAKER} label="Sak og behandling" icon={<BulletListIcon />} />
              <Tabs.Tab
                value={PersonOversiktFane.PERSONOPPLYSNINGER}
                label="Personopplysninger"
                icon={<PersonIcon />}
              />
              <Tabs.Tab value={PersonOversiktFane.HENDELSER} label="Hendelser" icon={<BellIcon />} />
              <Tabs.Tab value={PersonOversiktFane.DOKUMENTER} label="Dokumentoversikt" icon={<FileTextIcon />} />
              <Tabs.Tab value={PersonOversiktFane.BREV} label="Brev" icon={<EnvelopeClosedIcon />} />
              {skalViseNotater && (
                <Tabs.Tab value={PersonOversiktFane.NOTATER} label="Notater" icon={<FileTextIcon />} />
              )}
              {isOmstillingsstoenad(sakMedBehandlingResult) && (
                <Tabs.Tab value={PersonOversiktFane.SAMORDNING} label="Samordning" icon={<CogRotationIcon />} />
              )}
            </Tabs.List>

            <Tabs.Panel value={PersonOversiktFane.SAKER}>
              <SakOversikt sakResult={sakMedBehandlingResult} fnr={person.foedselsnummer} />
            </Tabs.Panel>
            <Tabs.Panel value={PersonOversiktFane.PERSONOPPLYSNINGER}>
              <Personopplysninger sakResult={sakMedBehandlingResult} fnr={person.foedselsnummer} />
            </Tabs.Panel>
            <Tabs.Panel value={PersonOversiktFane.HENDELSER}>
              <Hendelser sakResult={sakMedBehandlingResult} fnr={person.foedselsnummer} />
            </Tabs.Panel>
            <Tabs.Panel value={PersonOversiktFane.DOKUMENTER}>
              <Dokumentliste sakResult={sakMedBehandlingResult} fnr={person.foedselsnummer} />
            </Tabs.Panel>
            <Tabs.Panel value={PersonOversiktFane.BREV}>
              <BrevOversikt sakResult={sakMedBehandlingResult} />
            </Tabs.Panel>
            {skalViseNotater && (
              <Tabs.Panel value={PersonOversiktFane.NOTATER}>
                <NotatOversikt sakResult={sakMedBehandlingResult} />
              </Tabs.Panel>
            )}
            <Tabs.Panel value={PersonOversiktFane.SAMORDNING}>
              <SamordningSak fnr={person.foedselsnummer} sakResult={sakMedBehandlingResult} />
            </Tabs.Panel>
          </Tabs>
        )
      )}

      {sakId &&
        mapFailure(sakResult, (error) => {
          return <Box padding="8">{handleError(error)}</Box>
        })}

      {!sakId && !fnr && <ApiErrorAlert>Feil oppsto ved henting av person</ApiErrorAlert>}
    </>
  )
}
