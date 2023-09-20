import { SakType } from '~shared/types/sak'
import { HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { BodyShort, Button, Heading } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { Content, ContentHeader } from '~shared/styled'
import { IBehandlingsType, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { formaterVedtaksResultat, useVedtaksResultat } from '~components/behandling/useVedtaksResultat'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { NesteOgTilbake } from '~components/behandling/handlinger/NesteOgTilbake'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'
import { useBehandlingRoutes } from '~components/behandling/BehandlingRoutes'
import { isFailure, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { hentFunksjonsbrytere } from '~shared/api/feature'
import { hentVilkaarsvurdering } from '~shared/api/vilkaarsvurdering'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useEffect, useState } from 'react'
import FastTrygdetid from '~components/behandling/trygdetid/FastTrygdetid'
import YrkesskadeTrygdetidBP from '~components/behandling/trygdetid/YrkesskadeTrygdetidBP'
import YrkesskadeTrygdetidOMS from '~components/behandling/trygdetid/YrkesskadeTrygdetidOMS'
import { Trygdetid } from '~components/behandling/trygdetid/Trygdetid'

const TrygdetidVisning = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const behandles = hentBehandlesFraStatus(behandling.status)
  const { next } = useBehandlingRoutes()
  const [funksjonsbrytere, postHentFunksjonsbrytere] = useApiCall(hentFunksjonsbrytere)
  const [beregnTrygdetid, setBeregnTrygdetid] = useState<boolean>(false)
  const [vilkaarsvurdering, getVilkaarsvurdering] = useApiCall(hentVilkaarsvurdering)
  const [yrkesskadeTrygdetid, setYrkesskadeTrygdetid] = useState<boolean>(false)

  const featureToggleNameTrygdetid = 'pensjon-etterlatte.bp-bruk-faktisk-trygdetid'

  const vedtaksresultat =
    behandling.behandlingType !== IBehandlingsType.MANUELT_OPPHOER ? useVedtaksResultat() : 'opphoer'
  const virkningstidspunkt = behandling.virkningstidspunkt?.dato
    ? formaterStringDato(behandling.virkningstidspunkt.dato)
    : undefined

  useEffect(() => {
    postHentFunksjonsbrytere([featureToggleNameTrygdetid], (brytere) => {
      const trygdetidBryter = brytere.find((bryter) => bryter.toggle === featureToggleNameTrygdetid)

      if (trygdetidBryter) {
        setBeregnTrygdetid(trygdetidBryter.enabled)

        getVilkaarsvurdering(behandling.id, (vurdering) => {
          setYrkesskadeTrygdetid(vurdering.isYrkesskade)
        })
      }
    })
  }, [])

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading spacing size={'large'} level={'1'}>
            Trygdetid
          </Heading>
          <BodyShort spacing>
            Vilkårsresultat: <strong>{formaterVedtaksResultat(vedtaksresultat, virkningstidspunkt)}</strong>
          </BodyShort>
        </HeadingWrapper>
      </ContentHeader>
      {isSuccess(funksjonsbrytere) &&
        isSuccess(vilkaarsvurdering) &&
        (yrkesskadeTrygdetid ? (
          {
            [SakType.BARNEPENSJON]: <YrkesskadeTrygdetidBP />,
            [SakType.OMSTILLINGSSTOENAD]: <YrkesskadeTrygdetidOMS />,
          }[behandling.sakType]
        ) : beregnTrygdetid ? (
          <Trygdetid redigerbar={behandles} utenlandstilsnitt={behandling.utenlandstilsnitt} />
        ) : (
          <FastTrygdetid />
        ))}

      {isFailure(vilkaarsvurdering) && <ApiErrorAlert>Kunne ikke hente vilkaarsvurdering</ApiErrorAlert>}
      {isFailure(funksjonsbrytere) && <ApiErrorAlert>Kunne ikke hente funksjonsbrytere</ApiErrorAlert>}

      {behandles ? (
        <BehandlingHandlingKnapper>
          <Button variant="primary" onClick={next}>
            Gå videre
          </Button>
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </Content>
  )
}

export default TrygdetidVisning
