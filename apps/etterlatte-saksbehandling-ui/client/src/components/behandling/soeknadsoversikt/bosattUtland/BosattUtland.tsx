import { MottatteSeder } from '~components/behandling/soeknadsoversikt/bosattUtland/MottatteSeder'
import { SendteSeder } from '~components/behandling/soeknadsoversikt/bosattUtland/SendteSeder'
import { useApiCall } from '~shared/hooks/useApiCall'
import React, { useEffect, useState } from 'react'
import Spinner from '~shared/Spinner'
import { Bosattutland, lagreBosattutland } from '~shared/api/bosattutland'
import { Button } from '@navikt/ds-react'
import { LandMedDokumenter } from '~shared/types/RevurderingInfo'
import { oppdaterBehandlingsstatus } from '~store/reducers/BehandlingReducer'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { useAppDispatch } from '~store/Store'
import { CheckmarkCircleIcon } from '@navikt/aksel-icons'

import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { hentAlleLand } from '~shared/api/behandling'
import { ILand, sorterLand } from '~utils/kodeverk'

export const BosattUtland = ({
  behandlingId,
  bosattutland,
  redigerbar,
}: {
  behandlingId: string
  bosattutland: Bosattutland | null
  redigerbar: boolean
}) => {
  const dispatch = useAppDispatch()
  const [hentAlleLandRequest, fetchAlleLand] = useApiCall(hentAlleLand)
  const [alleLandKodeverk, setAlleLandKodeverk] = useState<ILand[] | null>(null)
  const [lagreBosattutlandStatus, lagreBosattutlandApi] = useApiCall(lagreBosattutland)

  const [visLagretOk, setVisLagretOk] = useState<boolean>(false)

  useEffect(() => {
    fetchAlleLand(null, (landliste) => {
      setAlleLandKodeverk(sorterLand(landliste))
    })
  }, [])

  const initalStateLandMedDokumenterMottatte = [
    {
      landIsoKode: undefined,
      dokumenter: [],
    },
  ]
  const [landMedDokumenterMottatte, setLandMedDokumenterMottatte] = useState<LandMedDokumenter[]>(
    bosattutland?.mottatteSeder ?? initalStateLandMedDokumenterMottatte
  )
  const [feilkoderMottatte, setFeilkoderMottatte] = useState<Set<string>>(new Set([]))

  const [rinanummer, setRinanummer] = useState<string>(bosattutland?.rinanummer ?? '')
  const [landMedDokumenter, setLandMedDokumenter] = useState<LandMedDokumenter[]>(bosattutland?.sendteSeder ?? [])
  const [feilkoder, setFeilkoder] = useState<Set<string>>(new Set([]))

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
      {isFailureHandler({
        apiResult: hentAlleLandRequest,
        errorMessage: 'Vi klarte ikke å hente landlisten, den er påkrevd for å kunne fylle inn SED data',
      })}

      <Spinner visible={isPending(hentAlleLandRequest)} label="Henter land" />

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
            redigerbar={redigerbar}
          />
          <SendteSeder
            landliste={alleLandKodeverk as ILand[]}
            feilkoder={feilkoder}
            setFeilkoder={setFeilkoder}
            landMedDokumenter={landMedDokumenter}
            setLandMedDokumenter={setLandMedDokumenter}
            redigerbar={redigerbar}
          />
          {isFailureHandler({
            apiResult: lagreBosattutlandStatus,
            errorMessage: 'Klarte ikke å lagre bosatt utland',
          })}
          <div style={{ marginTop: '2rem' }}>
            {redigerbar && (
              <Button onClick={() => lagreBosattutlandApiWrapper()} loading={isPending(lagreBosattutlandStatus)}>
                {visLagretOk ? (
                  <div style={{ minWidth: '148px', minHeight: '24px' }}>
                    <CheckmarkCircleIcon
                      color="var(--ax-neutral-100)"
                      stroke="var(--ax-neutral-100)"
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
            )}
          </div>
        </>
      )}
    </>
  )
}
