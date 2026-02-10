import React, { useEffect, useState } from 'react'
import { LandMedDokumenter } from '~shared/types/RevurderingInfo'
import { useApiCall } from '~shared/hooks/useApiCall'
import {
  hentAlleLand,
  hentSluttbehandlingOmgjoering,
  lagreSluttbehandlingOmgjoering,
  SluttbehandlingUtlandOmgjoering,
} from '~shared/api/behandling'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import Spinner from '~shared/Spinner'
import { ILand, sorterLand } from '~utils/kodeverk'
import SEDLandMedDokumenter from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/SEDLandMedDokumenter'
import { Alert, BodyShort, Box, Button, ErrorSummary, Heading } from '@navikt/ds-react'
import { CheckmarkCircleIcon } from '@navikt/aksel-icons'

import { isFailureHandler } from '~shared/api/IsFailureHandler'

export default function SluttBehandlingOmgjoering({
  behandlingId,
  redigerbar,
}: {
  behandlingId: string
  redigerbar: boolean
}) {
  const [sluttbehandlingStatus, hentSluttbehandling] = useApiCall(hentSluttbehandlingOmgjoering)
  useEffect(() => {
    hentSluttbehandling(behandlingId)
  }, [])

  return (
    <Box marginBlock="space-8 space-0" maxWidth="1200px">
      {mapResult(sluttbehandlingStatus, {
        success: (sluttbehandlingUtland) => (
          <Sluttbehandling
            sluttbehandlingUtland={sluttbehandlingUtland}
            behandlingId={behandlingId}
            redigerbar={redigerbar}
          />
        ),
        error: (error) => <ApiErrorAlert>{error.detail}</ApiErrorAlert>,
        pending: <Spinner label="Henter status for sluttbehandling..." />,
      })}
    </Box>
  )
}

function Sluttbehandling({
  behandlingId,
  sluttbehandlingUtland,
  redigerbar,
}: {
  behandlingId: string
  sluttbehandlingUtland: SluttbehandlingUtlandOmgjoering | null
  redigerbar: boolean
}) {
  const [hentAlleLandRequest, fetchAlleLand] = useApiCall(hentAlleLand)
  const [, fetchAlleLandKodeverk] = useState<ILand[] | null>(null)
  const [lagreStatus, lagreSluttbehandlingDokumenter] = useApiCall(lagreSluttbehandlingOmgjoering)

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
    fetchAlleLand(null, (landliste) => {
      fetchAlleLandKodeverk(sorterLand(landliste))
    })
  }, [])

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

  const lagreSluttbehandling = () => {
    const feilkoder = validerSkjema()
    if (feilkoder.size === 0) {
      lagreSluttbehandlingDokumenter(
        {
          behandlingId: behandlingId,
          sluttbehandling: { landMedDokumenter: landMedDokumenter },
        },
        () => {
          setVisLagretOk(true)
          const toSekunderIMs = 2000
          setTimeout(() => setVisLagretOk(false), toSekunderIMs)
        }
      )
    }
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
          loading={isPending(lagreStatus)}
          variant="primary"
          onClick={() => lagreSluttbehandling()}
        >
          {visLagretOk ? (
            <div style={{ minWidth: '148px', minHeight: '24px' }}>
              <CheckmarkCircleIcon
                color="var(--a-white)"
                stroke="var(--a-white)"
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
        apiResult: lagreStatus,
        errorMessage: 'Kunne ikke lagre revurderingsinfo',
      })}
    </>
  )
}
