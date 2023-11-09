import { MottatteSeder } from '~components/behandling/soeknadsoversikt/bosattUtland/MottatteSeder'
import { SendteSeder } from '~components/behandling/soeknadsoversikt/bosattUtland/SendteSeder'
import { is5xxError, isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { hentAlleLand, ILand, sorterLand } from '~shared/api/trygdetid'
import React, { useEffect, useState } from 'react'
import Spinner from '~shared/Spinner'
import { Bosattutland, hentBosattutland, lagreBosattutland } from '~shared/api/bosattutland'
import { Alert, Button } from '@navikt/ds-react'
import { LandMedDokumenter } from '~shared/types/RevurderingInfo'
import { oppdaterBehandlingsstatus } from '~store/reducers/BehandlingReducer'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { useAppDispatch } from '~store/Store'
import { ApiErrorAlert } from '~ErrorBoundary'
import { CheckmarkCircleIcon } from '@navikt/aksel-icons'
import { AWhite } from '@navikt/ds-tokens/dist/tokens'

export const BosattUtland = ({ behandlingId }: { behandlingId: string }) => {
  const dispatch = useAppDispatch()
  const [hentAlleLandRequest, fetchAlleLand] = useApiCall(hentAlleLand)
  const [alleLandKodeverk, setAlleLandKodeverk] = useState<ILand[] | null>(null)
  const [lagreBosattutlandStatus, lagreBosattutlandApi] = useApiCall(lagreBosattutland)
  const [hentBosattUtlandStatus, hentBosattUtlandApi] = useApiCall(hentBosattutland)
  const [hentetBosattUtland, setHentetBosattUtland] = useState<Bosattutland | null>(null)
  const [visLagretOk, setVisLagretOk] = useState<boolean>(false)

  const initalStateLandMedDokumenterMottatte = [
    {
      landIsoKode: undefined,
      dokumenter: [
        { dokumenttype: 'P5000', dato: undefined, kommentar: '' },
        { dokumenttype: 'P6000', dato: undefined, kommentar: '' },
        { dokumenttype: 'P2100', dato: undefined, kommentar: '' },
      ],
    },
  ]
  const [landMedDokumenterMottatte, setLandMedDokumenterMottatte] = useState<LandMedDokumenter[]>(
    hentetBosattUtland?.mottatteSeder ?? initalStateLandMedDokumenterMottatte
  )
  const [feilkoderMottatte, setFeilkoderMottatte] = useState<Set<string>>(new Set([]))
  const [rinanummer, setRinanummer] = useState<string>(hentetBosattUtland?.rinanummer ?? '')

  const initalStateLandMedDokumenter = [
    {
      landIsoKode: undefined,
      dokumenter: [{ dokumenttype: 'P8000', dato: undefined, kommentar: '' }],
    },
  ]
  const [landMedDokumenter, setLandMedDokumenter] = useState<LandMedDokumenter[]>(
    hentetBosattUtland?.sendteSeder ?? initalStateLandMedDokumenter
  )
  const [feilkoder, setFeilkoder] = useState<Set<string>>(new Set([]))

  useEffect(() => {
    hentBosattUtlandApi(behandlingId, (bosattUtland) => setHentetBosattUtland(bosattUtland))
    fetchAlleLand(null, (landliste) => {
      setAlleLandKodeverk(sorterLand(landliste))
    })
  }, [])

  const validerSkjema = (landMedDokumenter: LandMedDokumenter[]): Set<string> => {
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
    return feilkoder
  }

  const lagreBosattutlandApiWrapper = () => {
    const feilmeldingerMottatte = validerSkjema(landMedDokumenterMottatte)
    if (!rinanummer) feilmeldingerMottatte.add('Du må legge til et rinanummer')

    const feilmeldingerSendte = validerSkjema(landMedDokumenter)
    setFeilkoder(feilmeldingerSendte)
    setFeilkoderMottatte(feilmeldingerMottatte)
    if (feilmeldingerMottatte.size === 0 && feilmeldingerSendte.size === 0) {
      const bosattUtland: Bosattutland = {
        behandlingId: behandlingId,
        mottatteSeder: landMedDokumenterMottatte,
        sendteSeder: landMedDokumenter,
        rinanummer: rinanummer,
      }
      const toSekunderIMs = 2000
      lagreBosattutlandApi({ bosattutland: bosattUtland, behandlingId: behandlingId }, () => {
        setVisLagretOk(true)
        setTimeout(() => setVisLagretOk(false), toSekunderIMs)
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.OPPRETTET))
      })
    }
  }

  return (
    <>
      {isPending(hentAlleLandRequest) && <Spinner visible={true} label="Henter land" />}
      {is5xxError(hentBosattUtlandStatus) && (
        <Alert variant="warning">Vi klarte ikke å hente lagret data for bosatt utland</Alert>
      )}
      {alleLandKodeverk && (
        <>
          <MottatteSeder
            landliste={alleLandKodeverk as ILand[]}
            feilkoder={feilkoderMottatte}
            setFeilkoderMottatte={setFeilkoderMottatte}
            landMedDokumenterMottatte={landMedDokumenterMottatte}
            setLandMedDokumenterMottatte={setLandMedDokumenterMottatte}
            rinanummer={rinanummer}
            setRinanummer={setRinanummer}
          />
          <SendteSeder
            landliste={alleLandKodeverk as ILand[]}
            feilkoder={feilkoder}
            setFeilkoder={setFeilkoder}
            landMedDokumenter={landMedDokumenter}
            setLandMedDokumenter={setLandMedDokumenter}
          />
          {isPending(lagreBosattutlandStatus) && <Spinner visible={true} label="Lagrer bosatt utland" />}
          {isFailure(lagreBosattutlandStatus) && <ApiErrorAlert>Klarte ikke å lagre bosatt utland</ApiErrorAlert>}
          <Button onClick={() => lagreBosattutlandApiWrapper()}>
            {visLagretOk ? (
              <div style={{ minWidth: '148px', minHeight: '24px' }}>
                <CheckmarkCircleIcon
                  color={AWhite}
                  stroke={AWhite}
                  aria-hidden="true"
                  style={{
                    width: '1.8rem',
                    height: '1.8rem',
                    transform: 'translate(-40%, -10%)',
                    position: 'absolute',
                  }}
                />
              </div>
            ) : (
              <>Lagre bosatt utland</>
            )}
          </Button>
        </>
      )}
    </>
  )
}
