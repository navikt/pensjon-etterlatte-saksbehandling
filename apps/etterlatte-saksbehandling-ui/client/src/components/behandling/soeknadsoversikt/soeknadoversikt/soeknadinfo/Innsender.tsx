import { Detail } from '@navikt/ds-react'
import { DetailWrapper, WarningIconWrapper } from '../../styled'
import { VurderingsResultat, IGyldighetproving } from '../../../../../store/reducers/BehandlingReducer'
import { WarningIcon } from '../../../../../shared/icons/warningIcon'
import { IPersonOpplysning } from '../../../types'

export const Innsender = ({
  innsenderHarForeldreAnsvar,
  innsender,
}: {
  innsenderHarForeldreAnsvar: IGyldighetproving | undefined
  innsender: IPersonOpplysning
}) => {
  return (
    <DetailWrapper>
      {innsenderHarForeldreAnsvar?.resultat === VurderingsResultat.OPPFYLT && (
        <div>
          <Detail size="medium">Innsender</Detail>
          {innsender?.fornavn} {innsender?.etternavn}
          <div>(gjenlevende forelder)</div>
        </div>
      )}

      {innsenderHarForeldreAnsvar?.resultat === VurderingsResultat.IKKE_OPPFYLT && (
        <div>
          <Detail size="medium" className="detailWrapperWithIcon">
            <WarningIconWrapper>
              <WarningIcon />
            </WarningIconWrapper>
            Innsender
          </Detail>
          <span className="warningText">
            {innsender?.fornavn} {innsender?.etternavn}
          </span>
        </div>
      )}

      {innsenderHarForeldreAnsvar?.resultat === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING && (
        <div>
          <Detail size="medium" className="detailWrapperWithIcon">
            <WarningIconWrapper>
              <WarningIcon />
            </WarningIconWrapper>
            Innsender
          </Detail>
          <span className="warningText">Mangler info</span>
        </div>
      )}
    </DetailWrapper>
  )
}
