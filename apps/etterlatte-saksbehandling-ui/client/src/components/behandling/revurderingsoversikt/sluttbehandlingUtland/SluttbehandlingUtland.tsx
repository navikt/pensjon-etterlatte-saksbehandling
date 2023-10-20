import { Button, Heading } from '@navikt/ds-react'
import { isFailure, isSuccess, mapAllApiResult, useApiCall } from '~shared/hooks/useApiCall'
import { hentKravpakkeforSak } from '~shared/api/generellbehandling'
import { useEffect, useState } from 'react'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { InfoWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { formaterNavn } from '~shared/types/Person'
import { KravpakkeUtland } from '~shared/types/Generellbehandling'
import { hentAlleLand, ILand, sorterLand } from '~shared/api/trygdetid'
import SEDLand, { LandMedDokumenter } from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/SEDLand'
import { TextButton } from '~components/behandling/soeknadsoversikt/familieforhold/personer/personinfo/TextButton'
import SluttbehandlingUtlandFelter from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/SluttbehandlingUtlandFelter'
import { lagreRevurderingInfo } from '~shared/api/revurdering'

export default function SluttbehandlingUtland({ sakId, revurderingId }: { sakId: number; revurderingId: string }) {
  const [kravpakkeStatus, hentKravpakke] = useApiCall(hentKravpakkeforSak)
  const [hentAlleLandRequest, fetchAlleLand] = useApiCall(hentAlleLand)
  const [alleLandKodeverk, setAlleLandKodeverk] = useState<ILand[] | null>(null)
  const [visHistorikk, setVisHistorikk] = useState(false)
  const [lagrestatus, lagre] = useApiCall(lagreRevurderingInfo)
  const [landMedDokumenter, setLandMedDokumenter] = useState<LandMedDokumenter[]>([])

  useEffect(() => {
    hentKravpakke(sakId)
    fetchAlleLand(null, (landliste) => {
      setAlleLandKodeverk(sorterLand(landliste))
    })
  }, [])

  const lagreRevurderingsinfo = () => {
    if (true) {
      lagre({
        behandlingId: revurderingId,
        begrunnelse: 'regeprgjrepi',
        revurderingInfo: { type: 'SLUTTBEHANDLING_UTLAND', landMedDokumenter: landMedDokumenter },
      })
    }
  }

  return (
    <>
      <Heading level="2" size="medium">
        Sluttbehandling ved mottatt info utland
      </Heading>
      <p>
        Sluttbehandling gjøres i saker der gjenlevende er bosatt i Norge (på søknadstidspunktet), og avdøde har
        bodd/arbeidet i Norge og ett eller flere avtaleland, og det er satt fram krav om pensjon fra avtaleland. Det
        gjøres en vurdering av rettigheter og trygdeavtale etter man har mottatt nødvendig dokumentasjon fra utenlandske
        trygdemyndigheter
      </p>
      <Heading level="2" size="medium">
        Informasjon fra utsendelse av kravpakke
      </Heading>
      {mapAllApiResult(
        kravpakkeStatus,
        <Spinner visible={true} label="Henter kravpakke" />,
        null,
        () => (
          <ApiErrorAlert>Klarte ikke å hente kravpakke for sluttbehandling</ApiErrorAlert>
        ),
        (kravpakkeMedAvdoed) => (
          <>
            <InfoWrapper>
              <Info tekst={formaterNavn(kravpakkeMedAvdoed.avdoed)} label="Kravpakke gjelder" />
              <Info
                tekst={formaterKravpakkeLand(kravpakkeMedAvdoed.kravpakke.innhold, alleLandKodeverk)}
                label="Kravpakke sendt til"
              />
              <Info label="Saks-ID RINA" tekst={kravpakkeMedAvdoed.kravpakke.innhold.rinanummer} />
              <Info label="Notater" tekst={kravpakkeMedAvdoed.kravpakke.innhold.begrunnelse} />
            </InfoWrapper>
          </>
        )
      )}
      <Heading level="2" size="medium">
        Mottatte SED
      </Heading>
      <p>Fyll inn hvilke SED som er mottatt i RINA pr land.</p>
      {isSuccess(hentAlleLandRequest) && (
        <SEDLand
          landListe={hentAlleLandRequest.data}
          landMedDokumenter={landMedDokumenter}
          setLandMedDokumenter={setLandMedDokumenter}
        />
      )}
      <Button variant="secondary" onClick={() => lagreRevurderingsinfo()}>
        Lagre opplysninger
      </Button>
      {isFailure(lagrestatus) && <ApiErrorAlert>Kunne ikke lagre revurderingsinfo</ApiErrorAlert>}
      <Heading level="2" size="medium" style={{ marginTop: '2rem' }}>
        Tidligere mottatte SED
      </Heading>
      <TextButton isOpen={visHistorikk} setIsOpen={setVisHistorikk} />
      {visHistorikk && <>Vis historikk her </>}
      {isSuccess(kravpakkeStatus) && (
        <SluttbehandlingUtlandFelter tilknyttetBehandling={kravpakkeStatus.data.kravpakke.tilknyttetBehandling} />
      )}
    </>
  )
}

function formaterKravpakkeLand(innhold: KravpakkeUtland, landliste: ILand[] | null) {
  if (landliste) {
    return innhold.landIsoKode
      ?.map((kode) => landliste.find((kodeverkLand) => kodeverkLand.isoLandkode === kode))
      .join(', ')
  } else {
    return innhold.landIsoKode ? innhold.landIsoKode.join(', ') : ''
  }
}
