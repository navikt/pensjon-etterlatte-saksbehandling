import React, { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { StatusBar } from '~shared/statusbar/Statusbar'
import { SakOversikt } from './sakOgBehandling/SakOversikt'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Link, Tabs } from '@navikt/ds-react'
import { fnrHarGyldigFormat } from '~utils/fnr'
import NavigerTilbakeMeny from '~components/person/NavigerTilbakeMeny'
import {
  BellIcon,
  BriefcaseClockIcon,
  BulletListIcon,
  CogRotationIcon,
  EnvelopeClosedIcon,
  FileTextIcon,
  PersonIcon,
  CurrencyExchangeIcon,
} from '@navikt/aksel-icons'
import { ApiErrorAlert } from '~ErrorBoundary'
import BrevOversikt from '~components/person/brev/BrevOversikt'
import { hentSakMedBehandlnger } from '~shared/api/sak'
import { isSuccess, Result, transformResult } from '~shared/api/apiUtils'
import { Dokumentliste } from '~components/person/dokumenter/Dokumentliste'
import { SamordningSak } from '~components/person/SamordningSak'
import { SakMedBehandlinger } from '~components/person/typer'
import { SakType } from '~shared/types/sak'
import { useSidetittel } from '~shared/hooks/useSidetittel'
import { Hendelser } from '~components/person/hendelser/Hendelser'
import NotatOversikt from '~components/person/notat/NotatOversikt'
import { usePersonLocationState } from '~components/person/lenker/usePersonLocationState'
import { AktivitetspliktSakoversikt } from '~components/person/aktivitet/AktivitetspliktSakoversikt'
import { Personopplysninger } from '~components/person/personopplysninger/Personopplysninger'
import EtteroppgjoerSaksoversikt from '~components/person/etteroppgjoer/EtteroppgjoerSaksoversikt'

export enum PersonOversiktFane {
  PERSONOPPLYSNINGER = 'PERSONOPPLYSNINGER',
  SAKER = 'SAKER',
  DOKUMENTER = 'DOKUMENTER',
  BREV = 'BREV',
  NOTATER = 'NOTATER',
  SAMORDNING = 'SAMORDNING',
  HENDELSER = 'HENDELSER',
  AKTIVITET = 'AKTIVITET',
  ETTEROPPGJOER = 'ETTEROPPGJOER',
}

export const Person = () => {
  useSidetittel('Personoversikt')

  const [search, setSearch] = useSearchParams()
  const fnr = usePersonLocationState(search.get('key'))?.fnr

  const [foretrukketSak, setForetrukketSak] = useState<number | undefined>()

  const [sakResult, sakFetch] = useApiCall(hentSakMedBehandlnger)
  const [fane, setFane] = useState(search.get('fane') || PersonOversiktFane.SAKER)

  const velgFane = (value: string) => {
    const valgtFane = value as PersonOversiktFane

    setSearch({ fane: valgtFane }, { state: { fnr } })
    setFane(valgtFane)
  }

  // Setter returnert sak til å eventuelt være den foretrukkede andre saken på bruker, hvis den er valgt
  const foretrukketSakResult = transformResult(sakResult, (sakData) =>
    foretrukketSak && foretrukketSak === sakData.ekstraSak?.sak.id
      ? { ...sakData.ekstraSak, ekstraSak: { ...sakData } }
      : sakData
  )

  useEffect(() => {
    if (fnrHarGyldigFormat(fnr)) {
      sakFetch(fnr!!)
    }
  }, [fnr])

  if (!fnr) {
    return (
      <ApiErrorAlert>
        Fant ikke fødselsnummer. Dette kan komme av at URL-en ble kopiert fra en annen fane eller nettleser, eller har
        blitt endret manuelt.
        <br />
        <Link href="/">Gå til forsiden</Link>
      </ApiErrorAlert>
    )
  } else if (!fnrHarGyldigFormat(fnr)) {
    return <ApiErrorAlert>Fødselsnummeret {fnr} har et ugyldig format (ikke 11 siffer)</ApiErrorAlert>
  }

  const isOmstillingsstoenad = (sakStatus: Result<SakMedBehandlinger>) => {
    return isSuccess(sakStatus) && sakStatus.data.sak.sakType === SakType.OMSTILLINGSSTOENAD
  }

  return (
    <>
      <StatusBar ident={fnr} />

      <NavigerTilbakeMeny to="/">Tilbake til oppgavebenken</NavigerTilbakeMeny>

      <Tabs value={fane} onChange={velgFane}>
        <Tabs.List>
          <Tabs.Tab value={PersonOversiktFane.SAKER} label="Sak og behandling" icon={<BulletListIcon />} />
          <Tabs.Tab value={PersonOversiktFane.PERSONOPPLYSNINGER} label="Personopplysninger" icon={<PersonIcon />} />
          <Tabs.Tab value={PersonOversiktFane.HENDELSER} label="Hendelser" icon={<BellIcon />} />
          {isOmstillingsstoenad(foretrukketSakResult) && (
            <Tabs.Tab value={PersonOversiktFane.AKTIVITET} label="Aktivitet" icon={<BriefcaseClockIcon />} />
          )}
          <Tabs.Tab value={PersonOversiktFane.DOKUMENTER} label="Dokumentoversikt" icon={<FileTextIcon />} />
          <Tabs.Tab value={PersonOversiktFane.BREV} label="Brev" icon={<EnvelopeClosedIcon />} />
          <Tabs.Tab value={PersonOversiktFane.NOTATER} label="Notater" icon={<FileTextIcon />} />
          {isOmstillingsstoenad(foretrukketSakResult) && (
            <Tabs.Tab value={PersonOversiktFane.SAMORDNING} label="Samordning" icon={<CogRotationIcon />} />
          )}

          {isOmstillingsstoenad(foretrukketSakResult) && (
            <Tabs.Tab value={PersonOversiktFane.ETTEROPPGJOER} label="Etteroppgjør" icon={<CurrencyExchangeIcon />} />
          )}
        </Tabs.List>

        <Tabs.Panel value={PersonOversiktFane.SAKER}>
          <SakOversikt sakResult={foretrukketSakResult} setForetrukketSak={setForetrukketSak} fnr={fnr} />
        </Tabs.Panel>
        <Tabs.Panel value={PersonOversiktFane.PERSONOPPLYSNINGER}>
          <Personopplysninger sakResult={foretrukketSakResult} fnr={fnr} />
        </Tabs.Panel>
        <Tabs.Panel value={PersonOversiktFane.HENDELSER}>
          <Hendelser sakResult={foretrukketSakResult} fnr={fnr} />
        </Tabs.Panel>
        <Tabs.Panel value={PersonOversiktFane.DOKUMENTER}>
          <Dokumentliste sakResult={foretrukketSakResult} fnr={fnr} />
        </Tabs.Panel>
        <Tabs.Panel value={PersonOversiktFane.BREV}>
          <BrevOversikt sakResult={foretrukketSakResult} />
        </Tabs.Panel>
        <Tabs.Panel value={PersonOversiktFane.NOTATER}>
          <NotatOversikt sakResult={foretrukketSakResult} />
        </Tabs.Panel>
        <Tabs.Panel value={PersonOversiktFane.SAMORDNING}>
          <SamordningSak fnr={fnr} sakResult={foretrukketSakResult} />
        </Tabs.Panel>
        <Tabs.Panel value={PersonOversiktFane.AKTIVITET}>
          <AktivitetspliktSakoversikt fnr={fnr} sakResult={foretrukketSakResult} />
        </Tabs.Panel>

        {isOmstillingsstoenad(foretrukketSakResult) && (
          <Tabs.Panel value={PersonOversiktFane.ETTEROPPGJOER}>
            <EtteroppgjoerSaksoversikt sakResult={foretrukketSakResult} />
          </Tabs.Panel>
        )}
      </Tabs>
    </>
  )
}
