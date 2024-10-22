import { Alert, BodyShort, Box, Button, ErrorSummary, Heading, VStack } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentKravpakkeforSak } from '~shared/api/generellbehandling'
import React, { useEffect, useState } from 'react'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { formaterNavn } from '~shared/types/Person'
import { KravpakkeUtland } from '~shared/types/Generellbehandling'
import SEDLandMedDokumenter from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/SEDLandMedDokumenter'
import { TextButton } from '~components/behandling/soeknadsoversikt/familieforhold/personer/personinfo/TextButton'
import { hentRevurderingerForSakMedAarsak, lagreRevurderingInfo } from '~shared/api/revurdering'
import { AWhite } from '@navikt/ds-tokens/dist/tokens'
import { CheckmarkCircleIcon } from '@navikt/aksel-icons'
import { LandMedDokumenter, SluttbehandlingUtlandInfo } from '~shared/types/RevurderingInfo'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'
import HistoriskeSEDer from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/historikk/HistoriskeSEDer'
import { formaterDato } from '~utils/formatering/dato'

import { isPending, mapAllApiResult, mapResult } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { hentAlleLand } from '~shared/api/behandling'
import { ILand, sorterLand } from '~utils/kodeverk'

export default function SluttbehandlingUtland({
  sakId,
  revurderingId,
  sluttbehandlingUtland,
  redigerbar,
}: {
  sakId: number
  revurderingId: string
  sluttbehandlingUtland: SluttbehandlingUtlandInfo | undefined
  redigerbar: boolean
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
    sluttbehandlingUtland && sluttbehandlingUtland.landMedDokumenter.length
      ? sluttbehandlingUtland.landMedDokumenter
      : initalStateLandMedDokumenter
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
    <Box marginBlock="10 0" maxWidth="1200px">
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
          <Spinner label="Henter kravpakke" />,
          null,
          () => (
            <ApiErrorAlert>Klarte ikke å hente kravpakke for sluttbehandling</ApiErrorAlert>
          ),
          (kravpakkeMedAvdoed) => {
            return (
              <>
                {kravpakkeMedAvdoed.kravpakke.innhold ? (
                  <VStack gap="4">
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
                  </VStack>
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
        Mottatt krav fra utland
      </Heading>
      <BodyShort>Fyll inn hvilke SED som er mottatt i RINA pr land.</BodyShort>
      {redigerbar || !!sluttbehandlingUtland?.landMedDokumenter.length ? (
        mapResult(hentAlleLandRequest, {
          success: (landliste) => (
            <SEDLandMedDokumenter
              redigerbar={redigerbar}
              landListe={landliste}
              landMedDokumenter={landMedDokumenter}
              setLandMedDokumenter={setLandMedDokumenter}
              resetFeilkoder={() => setFeilkoder(new Set([]))}
            />
          ),
          error: (error) => (
            <ApiErrorAlert>En feil oppstod under henting av perioder fra kravgrunnlag: {error.detail}</ApiErrorAlert>
          ),
          pending: <Spinner label="Henter land" />,
        })
      ) : (
        <Alert variant="info">Ingen dokumenter er registrert</Alert>
      )}
      {redigerbar ? (
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
      {isFailureHandler({
        apiResult: lagreRevurderingsinfoStatus,
        errorMessage: 'Kunne ikke lagre revurderingsinfo',
      })}
      <Heading level="2" size="medium" style={{ marginTop: '4rem' }}>
        Tidligere sluttbehandlinger
      </Heading>
      <TextButton isOpen={visHistorikk} setIsOpen={setVisHistorikk} />
      {visHistorikk &&
        mapAllApiResult(
          hentRevurderingerForSakMedAarsakStatus,
          <Spinner label="Henter historikk" />,
          null,
          () => <ApiErrorAlert>Klarte ikke å hente historikken</ApiErrorAlert>,
          (revurderingsinfoliste) => (
            <HistoriskeSEDer
              revurderingsinfoliste={revurderingsinfoliste}
              landListe={alleLandKodeverk ? alleLandKodeverk : []}
            />
          )
        )}
    </Box>
  )
}

function formaterKravpakkeLand(innhold: KravpakkeUtland | null, landliste: ILand[] | null) {
  if (landliste && innhold?.landIsoKode) {
    return innhold?.landIsoKode?.map((kode) => oversettIsokodeTilLandnavn(landliste, kode)).join(', ')
  } else {
    return innhold?.landIsoKode ? innhold.landIsoKode.join(', ') : ''
  }
}

const visDatoerForSendteDokumenter = (innhold: KravpakkeUtland | null): string => {
  if (innhold?.dokumenter) {
    return innhold.dokumenter
      .filter((doc) => doc.sendt && doc.dato)
      .map((e) => `${e.dokumenttype} ${formaterDato(e.dato as string)}`)
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
