import React, { useEffect, useState } from 'react'
import {
  hentOgLeggInnTrygdetidsGrunnlagForUfoeretrygdOgAlderspensjon,
  ITrygdetid,
  ITrygdetidGrunnlagType,
  sjekkOmAvdoedHarTrygdetidsgrunnlagIPesys,
  slettTrygdetidsgrunnlag,
} from '~shared/api/trygdetid'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Alert, BodyShort, Box, Button, Heading, HStack, VStack } from '@navikt/ds-react'
import { CalendarIcon, PlusIcon } from '@navikt/aksel-icons'
import { formaterEnumTilLesbarString } from '~utils/formatering/formatering'
import {
  FaktiskTrygdetidHjelpeTekst,
  FremtidigTrygdetidHjelpeTekst,
} from '~components/behandling/trygdetid/trygdetidPerioder/components/HjelpeTekster'
import { TrygdetidPerioderTable } from '~components/behandling/trygdetid/trygdetidPerioder/TrygdetidPerioderTable'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { TrygdetidGrunnlag } from '~components/behandling/trygdetid/TrygdetidGrunnlag'
import { ILand } from '~utils/kodeverk'
import { isFailure, isPending, mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { FeatureToggle, useFeaturetoggle } from '~useUnleash'
import { IBehandlingReducer, oppdaterBehandlingsstatus } from '~store/reducers/BehandlingReducer'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { useAppDispatch } from '~store/Store'

interface Props {
  trygdetid: ITrygdetid
  oppdaterTrygdetid: (trygdetid: ITrygdetid) => void
  trygdetidGrunnlagType: ITrygdetidGrunnlagType
  landListe: ILand[]
  redigerbar: boolean
  behandling: IBehandlingReducer
  setTrygdetider: (trygdetider: ITrygdetid[]) => void
}

export const TrygdetidPerioder = ({
  redigerbar,
  trygdetid,
  oppdaterTrygdetid,
  trygdetidGrunnlagType,
  landListe,
  behandling,
  setTrygdetider,
}: Props) => {
  const [slettTrygdetidResult, slettTrygdetidsgrunnlagRequest] = useApiCall(slettTrygdetidsgrunnlag)
  const dispatch = useAppDispatch()
  const [visLeggTilTrygdetidPeriode, setVisLeggTilTrygdetidPeriode] = useState<boolean>(false)
  const kanHenteTrygdetidFraPesys = useFeaturetoggle(FeatureToggle.trygdetid_fra_pesys)
  const [sjekkOmAvodedHarTTIPesysStatus, sjekkOmAvdoedHarTTIPesysHent] = useApiCall(
    sjekkOmAvdoedHarTrygdetidsgrunnlagIPesys
  )
  useEffect(() => {
    if (kanHenteTrygdetidFraPesys) {
      sjekkOmAvdoedHarTTIPesysHent(behandling.id)
    }
  }, [kanHenteTrygdetidFraPesys])
  const [hentTTPesysStatus, hentOgOppdaterDataFraPesys] = useApiCall(
    hentOgLeggInnTrygdetidsGrunnlagForUfoeretrygdOgAlderspensjon
  )
  const oppdaterTrygdetider = (trygdetid: ITrygdetid[]) => {
    setTrygdetider(trygdetid)
    dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.TRYGDETID_OPPDATERT))
  }

  const oppdaterTrygdetidMedPesysData = () => {
    hentOgOppdaterDataFraPesys(behandling.id, (trygdetider: ITrygdetid[]) => {
      oppdaterTrygdetider(trygdetider)
    })
  }

  const trygdetidPerioder = trygdetid.trygdetidGrunnlag
    .filter((trygdetid) => trygdetid.type === trygdetidGrunnlagType)
    .sort((a, b) => (a.periodeFra > b.periodeFra ? 1 : -1))

  const kanLeggeTilNyPeriode =
    redigerbar &&
    !visLeggTilTrygdetidPeriode &&
    !(trygdetidGrunnlagType === ITrygdetidGrunnlagType.FREMTIDIG && trygdetidPerioder.length > 0)

  const slettTrygdetid = (trygdetidGrunnlagId: string) => {
    slettTrygdetidsgrunnlagRequest(
      {
        trygdetidId: trygdetid.id,
        behandlingId: trygdetid.behandlingId,
        trygdetidGrunnlagId,
      },
      oppdaterTrygdetid
    )
  }

  const faktiskTrygdetid = trygdetidGrunnlagType === ITrygdetidGrunnlagType.FAKTISK
  return (
    <VStack gap="4">
      <HStack gap="2" align="center">
        <CalendarIcon aria-hidden height="1.5rem" width="1.5rem" />
        <Heading size="small">{formaterEnumTilLesbarString(trygdetidGrunnlagType)} trygdetid</Heading>
      </HStack>

      {faktiskTrygdetid ? <FaktiskTrygdetidHjelpeTekst /> : <FremtidigTrygdetidHjelpeTekst />}

      <TrygdetidPerioderTable
        trygdetidId={trygdetid.id}
        trygdetidPerioder={trygdetidPerioder}
        trygdetidGrunnlagType={trygdetidGrunnlagType}
        oppdaterTrygdetid={oppdaterTrygdetid}
        slettTrygdetid={slettTrygdetid}
        slettTrygdetidResult={slettTrygdetidResult}
        landListe={landListe}
        redigerbar={redigerbar}
      />

      {isFailureHandler({ apiResult: slettTrygdetidResult, errorMessage: 'Feil oppstått i slettingen av trygdetid' })}

      {visLeggTilTrygdetidPeriode && (
        <TrygdetidGrunnlag
          eksisterendeGrunnlag={undefined}
          trygdetidId={trygdetid.id}
          setTrygdetid={(trygdetid) => {
            oppdaterTrygdetid(trygdetid)
            setVisLeggTilTrygdetidPeriode(false)
          }}
          avbryt={() => setVisLeggTilTrygdetidPeriode(false)}
          trygdetidGrunnlagType={trygdetidGrunnlagType}
          landListe={landListe}
        />
      )}

      {kanLeggeTilNyPeriode && (
        <div>
          <Button
            size="small"
            variant="secondary"
            icon={<PlusIcon aria-hidden />}
            onClick={() => setVisLeggTilTrygdetidPeriode(true)}
          >
            Ny periode
          </Button>
        </div>
      )}
      {kanLeggeTilNyPeriode && faktiskTrygdetid && (
        <>
          {kanHenteTrygdetidFraPesys && (
            <>
              {mapResult(sjekkOmAvodedHarTTIPesysStatus, {
                initial: null,
                pending: <Spinner label="Sjekker om avdøede har trygdetidsgrunnlag i Pesys" />,
                error: () => <Alert variant="warning">Kunne ikke sjekke trygdetidsgrunnag i Pesys</Alert>,
                success: (harTrygdetidsgrunnlagIPesys) => {
                  return (
                    <>
                      {harTrygdetidsgrunnlagIPesys && (
                        <>
                          <Box maxWidth="fit-content">
                            <BodyShort>
                              Avdøed har trygdetidsgrunnlag registrert på uføretrygd eller alderspensjon.
                            </BodyShort>
                            <Button
                              size="small"
                              onClick={oppdaterTrygdetidMedPesysData}
                              loading={isPending(hentTTPesysStatus)}
                            >
                              Legg inn trygdetidsgrunnlag fra pesys
                            </Button>
                          </Box>
                          {isFailure(hentTTPesysStatus) && (
                            <Alert variant="warning">Kunne ikke hente trygdetid fra Pesys</Alert>
                          )}
                          {isPending(hentTTPesysStatus) && <Spinner label="Henter trygdetid i Pesys" />}
                        </>
                      )}
                    </>
                  )
                },
              })}
            </>
          )}
        </>
      )}
    </VStack>
  )
}
