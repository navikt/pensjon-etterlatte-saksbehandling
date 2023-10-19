import { Heading } from '@navikt/ds-react'
import { isSuccess, mapAllApiResult, useApiCall } from '~shared/hooks/useApiCall'
import { hentKravpakkeforSak } from '~shared/api/generellbehandling'
import { useEffect, useState } from 'react'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { InfoWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { formaterNavn } from '~shared/types/Person'
import { KravpakkeUtland } from '~shared/types/Generellbehandling'
import { hentAlleLand, ILand, sorterLand } from '~shared/api/trygdetid'
import SEDLand from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/SEDLand'
import { TextButton } from '~components/behandling/soeknadsoversikt/familieforhold/personer/personinfo/TextButton'
import SluttbehandlingUtlandFelter from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/SluttbehandlingUtlandFelter'

export default function SluttbehandlingUtland({ sakId }: { sakId: number }) {
  const [kravpakkeStatus, hentKravpakke] = useApiCall(hentKravpakkeforSak)
  const [hentAlleLandRequest, fetchAlleLand] = useApiCall(hentAlleLand)
  const [alleLandKodeverk, setAlleLandKodeverk] = useState<ILand[] | null>(null)
  const [visHistorikk, setVisHistorikk] = useState(false)
  useEffect(() => {
    hentKravpakke(sakId)
    fetchAlleLand(null, (landliste) => {
      setAlleLandKodeverk(sorterLand(landliste))
    })
  }, [])

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
      {isSuccess(hentAlleLandRequest) && <SEDLand landListe={hentAlleLandRequest.data} />}
      <Heading level="2" size="medium" style={{ marginTop: '2rem' }}>
        Tidligere mottatte SED
      </Heading>
      <TextButton isOpen={visHistorikk} setIsOpen={setVisHistorikk} />
      {visHistorikk && <>Vis historikk her </>}
      <SluttbehandlingUtlandFelter />
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
