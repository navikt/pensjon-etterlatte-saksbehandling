import { Detail, Heading } from '@navikt/ds-react'
import { InfoWrapper, DetailWrapper, HeadingWrapper } from '../styled'
import { BehandlingsStatusSmall, IBehandlingsStatus } from '../../behandlings-status'
import { BehandlingsTypeSmall, IBehandlingsType } from '../../behandlings-type'
import { format } from 'date-fns'
import { sjekkDodsfallMerEnn3AarSiden, hentVirkningstidspunkt, sjekkPersonFraSoeknadMotPdl } from './utils'
import { OmSoeknadVarsler } from './OmSoeknadVarsler'
import { WarningText } from '../../../../shared/styled'
import { PropsOmSoeknad } from '../props'

export const OmSoeknad: React.FC<PropsOmSoeknad> = ({
  soekerPdl,
  avdoedPersonPdl,
  soekerSoknad,
  avdodPersonSoknad,
  innsender,
  mottattDato,
  avdoedErForelderVilkaar,
}) => {
  const avdoedErLikISoeknad = avdoedPersonPdl?.foedselsnummer === avdodPersonSoknad?.foedselsnummer
  const dodsfallMerEnn3AarSiden = sjekkDodsfallMerEnn3AarSiden(avdoedPersonPdl?.doedsdato, mottattDato)

  if (!soekerPdl) {
    return (
      <>
        <Heading spacing size="small" level="5">
          Om søknaden
        </Heading>
        <div style={{ marginBottom: '3em' }}>Mangler info om søknad</div>
      </>
    )
  }

  return (
    <>
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
            sjekkPersonFraSoeknadMotPdl(avdoedPersonPdl, avdodPersonSoknad)
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
          {format(new Date(mottattDato), 'dd.MM.yyyy')}
        </DetailWrapper>
        <DetailWrapper>
          <Detail size="medium">Dato for dødsfall</Detail>
          <span className={dodsfallMerEnn3AarSiden ? 'warningText' : ''}>
            {format(new Date(avdoedPersonPdl?.doedsdato), 'dd.MM.yyyy')}
          </span>
        </DetailWrapper>
        <DetailWrapper>
          <Detail size="medium">Første mulig virkningstidspunkt</Detail>
          <span className={dodsfallMerEnn3AarSiden ? 'warningText' : ''}>
            {format(new Date(hentVirkningstidspunkt(avdoedPersonPdl?.doedsdato, mottattDato)), 'dd.MM.yyyy')}
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
