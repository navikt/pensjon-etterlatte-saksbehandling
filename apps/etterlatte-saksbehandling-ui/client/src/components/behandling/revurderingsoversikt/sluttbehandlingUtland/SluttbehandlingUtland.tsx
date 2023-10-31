import { Alert, BodyShort, Button, ErrorSummary, Heading } from '@navikt/ds-react'
import { isFailure, isPending, isSuccess, mapAllApiResult, useApiCall } from '~shared/hooks/useApiCall'
import { hentKravpakkeforSak } from '~shared/api/generellbehandling'
import { useEffect, useState } from 'react'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { InfoWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { formaterNavn } from '~shared/types/Person'
import { KravpakkeUtland } from '~shared/types/Generellbehandling'
import { hentAlleLand, ILand, sorterLand } from '~shared/api/trygdetid'
import SEDLandMedDokumenter from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/SEDLandMedDokumenter'
import { TextButton } from '~components/behandling/soeknadsoversikt/familieforhold/personer/personinfo/TextButton'
import SluttbehandlingUtlandFelter from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/SluttbehandlingUtlandFelter'
import { hentRevurderingerForSakMedAarsak, lagreRevurderingInfo } from '~shared/api/revurdering'
import { AWhite } from '@navikt/ds-tokens/dist/tokens'
import { CheckmarkCircleIcon } from '@navikt/aksel-icons'
import { LandMedDokumenter, SluttbehandlingUtlandInfo } from '~shared/types/RevurderingInfo'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'
import HistoriskeSEDer from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/historikk/HistoriskeSEDer'
import { formaterStringDato } from '~utils/formattering'

export default function SluttbehandlingUtland({
  sakId,
  revurderingId,
  sluttbehandlingUtland,
}: {
  sakId: number
  revurderingId: string
  sluttbehandlingUtland: SluttbehandlingUtlandInfo | undefined
}) {
  const [kravpakkeStatus, hentKravpakke] = useApiCall(hentKravpakkeforSak)
  const [hentAlleLandRequest, fetchAlleLand] = useApiCall(hentAlleLand)
  const [alleLandKodeverk, setAlleLandKodeverk] = useState<ILand[] | null>(null)
  const [visHistorikk, setVisHistorikk] = useState(false)
  const [lagreRevurderingsinfoStatus, lagreRevurderingsinfoApi] = useApiCall(lagreRevurderingInfo)
  const [hentRevurderingerForSakMedAarsakStatus, hentRevurderingerForSakMedAarsakFetch] = useApiCall(
    hentRevurderingerForSakMedAarsak
  )
  const initalStateLandMedDokumenter = [
    { landIsoKode: undefined, dokumenter: [{ dokumenttype: '', dato: undefined, kommentar: '' }] },
  ]
  const [landMedDokumenter, setLandMedDokumenter] = useState<LandMedDokumenter[]>(
    sluttbehandlingUtland ? sluttbehandlingUtland.landMedDokumenter : initalStateLandMedDokumenter
  )
  const [feilkoder, setFeilkoder] = useState<Set<string>>(new Set([]))
  const [visLagretOk, setVisLagretOk] = useState<boolean>(false)

  useEffect(() => {
    hentKravpakke(sakId)
    fetchAlleLand(null, (landliste) => {
      setAlleLandKodeverk(sorterLand(landliste))
    })
    hentRevurderingerForSakMedAarsakFetch({ sakId, revurderingsaarsak: Revurderingaarsak.SLUTTBEHANDLING_UTLAND })
  }, [])

  const lagreRevurderingsinfo = () => {
    const feilkoder = validerSkjema()
    if (feilkoder.size === 0) {
      lagreRevurderingsinfoApi(
        {
          behandlingId: revurderingId,
          revurderingInfo: { type: 'SLUTTBEHANDLING_UTLAND', landMedDokumenter: landMedDokumenter },
        },
        () => {
          setVisLagretOk(true)
          const toSekunderIMs = 2000
          setTimeout(() => setVisLagretOk(false), toSekunderIMs)
        }
      )
    }
  }

  const validerSkjema = () => {
    const feilkoder: Set<string> = new Set([])
    if (landMedDokumenter.find((landmedDokument) => !landmedDokument.landIsoKode)) {
      feilkoder.add('Du må velge et land for hver SED`er(land rad i tabellen under)')
    }
    if (landMedDokumenter.find((landMedDokument) => landMedDokument.dokumenter.length === 0)) {
      feilkoder.add('Du må legge til minst et dokument per land rad, eller slette landraden.')
    }
    landMedDokumenter.forEach((landMedDokument) => {
      if (landMedDokument.dokumenter.find((e) => !e.dokumenttype)) {
        feilkoder.add('Du må skrive inn en dokumenttype(P2000 feks) eller fjerne dokumentraden.')
      }
      if (landMedDokument.dokumenter.find((e) => !e.dato)) {
        feilkoder.add('Du må legge til dato for hvert dokument')
      }
    })
    setFeilkoder(feilkoder)
    return feilkoder
  }

  return (
    <>
      <Heading level="2" size="medium">
        Sluttbehandling ved mottatt info utland
      </Heading>
      <BodyShort>
        Sluttbehandling gjøres i saker der gjenlevende er bosatt i Norge (på søknadstidspunktet), og avdøde har
        bodd/arbeidet i Norge og ett eller flere avtaleland, og det er satt fram krav om pensjon fra avtaleland. Det
        gjøres en vurdering av rettigheter og trygdeavtale etter man har mottatt nødvendig dokumentasjon fra utenlandske
        trygdemyndigheter
      </BodyShort>
      <>
        <Heading level="2" size="medium" style={{ marginTop: '4rem' }}>
          Informasjon fra utsendelse av kravpakke
        </Heading>
        {mapAllApiResult(
          kravpakkeStatus,
          <Spinner visible={true} label="Henter kravpakke" />,
          null,
          () => (
            <ApiErrorAlert>Klarte ikke å hente kravpakke for sluttbehandling</ApiErrorAlert>
          ),
          (kravpakkeMedAvdoed) => {
            return (
              <>
                {kravpakkeMedAvdoed.kravpakke.innhold ? (
                  <InfoWrapper>
                    <Info label="Kravpakke gjelder" tekst={formaterNavn(kravpakkeMedAvdoed.avdoed)} />
                    <Info
                      tekst={formaterKravpakkeLand(kravpakkeMedAvdoed.kravpakke.innhold, alleLandKodeverk)}
                      label="Kravpakke sendt til"
                    />
                    <Info
                      label="Dokumenttyper og dato sendt"
                      tekst={visDatoerForSendteDokumenter(kravpakkeMedAvdoed.kravpakke.innhold)}
                    />
                    <Info label="Saks-ID RINA" tekst={kravpakkeMedAvdoed.kravpakke.innhold.rinanummer} />
                    <Info label="Kommentar" tekst={kravpakkeMedAvdoed.kravpakke.innhold.begrunnelse} />
                  </InfoWrapper>
                ) : (
                  <Alert variant="warning">
                    Fant ingen kravpakke for saken, kontroller at kravpakke ble opprettet. Finn sakens
                    førstegangsbehandling og kontroller at den har huket av {`"skal sende kravpakke"`}
                  </Alert>
                )}
              </>
            )
          }
        )}
      </>
      {!!feilkoder?.size ? (
        <ErrorSummary
          style={{ marginTop: '10rem' }}
          heading="SED`ene er ufullstendig utfylt, vennligst rett opp så du kan gå videre i revurderingen."
        >
          {Array.from(feilkoder).map((feilmelding, i) => (
            <ErrorSummary.Item key={i}>{feilmelding}</ErrorSummary.Item>
          ))}
        </ErrorSummary>
      ) : null}
      <Heading level="2" size="medium" style={{ marginTop: '2rem' }}>
        Mottatte SED
      </Heading>
      <BodyShort>Fyll inn hvilke SED som er mottatt i RINA pr land.</BodyShort>
      {isSuccess(hentAlleLandRequest) && (
        <SEDLandMedDokumenter
          landListe={hentAlleLandRequest.data}
          landMedDokumenter={landMedDokumenter}
          setLandMedDokumenter={setLandMedDokumenter}
          resetFeilkoder={() => setFeilkoder(new Set([]))}
        />
      )}
      {landMedDokumenter.length > 0 ? (
        <Button
          style={{ marginTop: '1.5rem', marginLeft: '0.5rem' }}
          loading={isPending(lagreRevurderingsinfoStatus)}
          variant="primary"
          onClick={() => lagreRevurderingsinfo()}
        >
          {visLagretOk ? (
            <div style={{ minWidth: '148px', minHeight: '24px' }}>
              <CheckmarkCircleIcon
                color={AWhite}
                stroke={AWhite}
                aria-hidden="true"
                style={{ width: '1.8rem', height: '1.8rem', transform: 'translate(-40%, -10%)', position: 'absolute' }}
              />
            </div>
          ) : (
            <>Lagre opplysninger</>
          )}
        </Button>
      ) : null}
      {isFailure(lagreRevurderingsinfoStatus) && <ApiErrorAlert>Kunne ikke lagre revurderingsinfo</ApiErrorAlert>}
      <Heading level="2" size="medium" style={{ marginTop: '4rem' }}>
        Tidligere sluttbehandlinger
      </Heading>
      <TextButton isOpen={visHistorikk} setIsOpen={setVisHistorikk} />
      {visHistorikk &&
        mapAllApiResult(
          hentRevurderingerForSakMedAarsakStatus,
          <Spinner visible={true} label="Henter historikk" />,
          null,
          () => <ApiErrorAlert>Klarte ikke å hente historikken</ApiErrorAlert>,
          (revurderingsinfoliste) => (
            <HistoriskeSEDer
              revurderingsinfoliste={revurderingsinfoliste}
              landListe={alleLandKodeverk ? alleLandKodeverk : []}
            />
          )
        )}
      {isSuccess(kravpakkeStatus) && (
        <SluttbehandlingUtlandFelter tilknyttetBehandling={kravpakkeStatus.data.kravpakke.tilknyttetBehandling} />
      )}
    </>
  )
}

function formaterKravpakkeLand(innhold: KravpakkeUtland | undefined, landliste: ILand[] | null) {
  if (landliste && innhold?.landIsoKode) {
    return innhold?.landIsoKode?.map((kode) => oversettIsokodeTilLandnavn(landliste, kode)).join(', ')
  } else {
    return innhold?.landIsoKode ? innhold.landIsoKode.join(', ') : ''
  }
}

const visDatoerForSendteDokumenter = (innhold: KravpakkeUtland | undefined): string => {
  if (innhold?.dokumenter) {
    return innhold.dokumenter
      .filter((doc) => doc.sendt && doc.dato)
      .map((e) => `${e.dokumenttype} ${formaterStringDato(e.dato as string)}`)
      .join(', ')
  }
  return ''
}

export function oversettIsokodeTilLandnavn(landliste: ILand[], landIsoKode?: string) {
  const funnetLand = landliste.find((kodeverkLand) => kodeverkLand.isoLandkode === landIsoKode)
  if (funnetLand) {
    return funnetLand.beskrivelse.tekst
  } else {
    return landIsoKode
  }
}
