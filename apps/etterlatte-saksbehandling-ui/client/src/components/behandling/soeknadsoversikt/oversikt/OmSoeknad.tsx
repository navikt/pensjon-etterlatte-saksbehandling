import { useContext } from 'react'
import { AppContext } from '../../../../store/AppContext'
import { Detail, Heading } from '@navikt/ds-react'
import { InfoWrapper, DetailWrapper, HeadingWrapper, WarningText } from '../styled'
import { BehandlingsStatusSmall, IBehandlingsStatus } from '../../behandlings-status'
import { BehandlingsTypeSmall, IBehandlingsType } from '../../behandlings-type'
import { format } from 'date-fns'
import { sjekkDodsfallMerEnn3AarSiden, hentVirkningstidspunkt, sjekkPersonFraSoeknadMotPdl } from './utils'
import {
  IKriterie,
  Kriterietype,
  VilkaarsType,
  VilkaarVurderingsResultat,
} from '../../../../store/reducers/BehandlingReducer'
import { usePersonInfoFromBehandling } from '../usePersonInfoFromBehandling'
import { OmSoeknadVarsler } from './OmSoeknadVarsler'

export const OmSoeknad = () => {
  const { soekerPdl, soekerSoknad, dodsfall, avdodPersonPdl, avdodPersonSoknad, innsender, mottattDato } =
    usePersonInfoFromBehandling()
  const ctx = useContext(AppContext)
  const vilkaar = ctx.state.behandlingReducer.vilkårsprøving

  const doedsfallVilkaar: any = vilkaar.find((vilkaar) => vilkaar.navn === VilkaarsType.DOEDSFALL_ER_REGISTRERT)
  const avdoedErForelderVilkaar =
    doedsfallVilkaar.kriterier.find((krit: IKriterie) => krit.navn === Kriterietype.AVDOED_ER_FORELDER).resultat ===
    VilkaarVurderingsResultat.OPPFYLT
  const avdoedErLikISoeknad = dodsfall?.foedselsnummer === avdodPersonSoknad.foedselsnummer
  const dodsfallMerEnn3AarSiden = sjekkDodsfallMerEnn3AarSiden(dodsfall?.doedsdato, mottattDato?.mottattDato)

  return (
    <>
      <h1>Søknadsoversikt</h1>
      <HeadingWrapper>
        <Heading spacing size="small" level="5">
          Om søknaden
        </Heading>
        <div className="details">
          <BehandlingsStatusSmall status={IBehandlingsStatus.FORSTEGANG} />
          <BehandlingsTypeSmall type={IBehandlingsType.BARNEPENSJON} />
        </div>
      </HeadingWrapper>

      <InfoWrapper>
        <DetailWrapper>
          <Detail size="medium">Mottaker</Detail>
          {sjekkPersonFraSoeknadMotPdl(soekerPdl, soekerSoknad)}
        </DetailWrapper>
        <DetailWrapper>
          <Detail size="medium">Avdød forelder</Detail>
          {avdoedErForelderVilkaar ? (
            sjekkPersonFraSoeknadMotPdl(avdodPersonPdl, avdodPersonSoknad)
          ) : (
            <WarningText>Ingen foreldre er død</WarningText>
          )}
        </DetailWrapper>
        <DetailWrapper>
          <Detail size="medium">Søknad fremsatt av</Detail>
          {innsender?.fornavn} {innsender?.etternavn}
        </DetailWrapper>
        <DetailWrapper>
          <Detail size="medium">Søknad mottatt</Detail>
          {format(new Date(mottattDato?.mottattDato), 'dd.MM.yyyy')}
        </DetailWrapper>
        <DetailWrapper>
          <Detail size="medium">Dato for dødsfall</Detail>
          <span className={dodsfallMerEnn3AarSiden ? 'warningText' : ''}>
            {format(new Date(dodsfall?.doedsdato), 'dd.MM.yyyy')}
          </span>
        </DetailWrapper>
        <DetailWrapper>
          <Detail size="medium">Første mulig virkningstidspunkt</Detail>
          <span className={dodsfallMerEnn3AarSiden ? 'warningText' : ''}>
            {format(new Date(hentVirkningstidspunkt(dodsfall?.doedsdato, mottattDato?.mottattDato)), 'dd.MM.yyyy')}
          </span>
        </DetailWrapper>
        <OmSoeknadVarsler
          feilForelderOppgittSomAvdoed={avdoedErForelderVilkaar && !avdoedErLikISoeknad}
          forelderIkkeDoed={!avdoedErForelderVilkaar}
          dodsfallMerEnn3AarSiden={dodsfallMerEnn3AarSiden}
        />
      </InfoWrapper>
    </>
  )
}
