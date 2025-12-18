/* eslint-disable @typescript-eslint/no-unused-expressions */

import { describe, expect, it } from 'vitest'
import { fnrErGyldig, fnrHarGyldigFormat } from './fnr'

describe('Test validering av fnr - med kontrollsiffer', () => {
  it('Sjekk diverse gyldige test fnr', () => {
    expect(fnrErGyldig('11057523044')).to.be.true
    expect(fnrErGyldig('26117512737')).to.be.true
    expect(fnrErGyldig('26104500284')).to.be.true
    expect(fnrErGyldig('24116324268')).to.be.true
    expect(fnrErGyldig('19440397142')).to.be.true
    expect(fnrErGyldig('05126307952')).to.be.true
  })

  it('Sjekk diverse syntetiske fnr', () => {
    expect(fnrErGyldig('07823847585')).to.be.true
    expect(fnrErGyldig('24910198617')).to.be.true
    expect(fnrErGyldig('06859597627')).to.be.true
    expect(fnrErGyldig('21919898760')).to.be.true
    expect(fnrErGyldig('12900799991')).to.be.true
    expect(fnrErGyldig('20840699480')).to.be.true
    expect(fnrErGyldig('27813648046')).to.be.true
    expect(fnrErGyldig('07823847585')).to.be.true
    expect(fnrErGyldig('24910198617')).to.be.true
    expect(fnrErGyldig('06859597627')).to.be.true
    expect(fnrErGyldig('21919898760')).to.be.true
    expect(fnrErGyldig('06488309690')).to.be.true
    expect(fnrErGyldig('30017215759')).to.be.true
  })

  it('Sjekk diverse ugyldige numeriske verdier', () => {
    expect(fnrErGyldig('1234')).to.be.false

    expect(fnrErGyldig('15048900000')).to.be.false
    expect(fnrErGyldig('00000000000')).to.be.false
    expect(fnrErGyldig('11111111111')).to.be.false
    expect(fnrErGyldig('22222222222')).to.be.false
    expect(fnrErGyldig('33333333333')).to.be.false
    expect(fnrErGyldig('44444444444')).to.be.false
    expect(fnrErGyldig('55555555555')).to.be.false
    expect(fnrErGyldig('66666666666')).to.be.false
    expect(fnrErGyldig('77777777777')).to.be.false
    expect(fnrErGyldig('88888888888')).to.be.false
    expect(fnrErGyldig('99999999999')).to.be.false

    expect(fnrErGyldig('36117512737')).to.be.false
    expect(fnrErGyldig('12345678901')).to.be.false
    expect(fnrErGyldig('00000000001')).to.be.false
    expect(fnrErGyldig('10000000000')).to.be.false
  })

  it('Sjekk diverse ugyldige tekst verdier', () => {
    expect(fnrErGyldig(undefined)).to.be.false
    expect(fnrErGyldig('')).to.be.false
    expect(fnrErGyldig('     ')).to.be.false
    expect(fnrErGyldig('hei')).to.be.false
    expect(fnrErGyldig('gyldigfnrmedtekst11057523044')).to.be.false
  })
})

describe('Test format på fnr', () => {
  it('Sjekk diverse gyldige verdier', () => {
    expect(fnrHarGyldigFormat('07823847585')).to.be.true
    expect(fnrHarGyldigFormat('24910198617')).to.be.true
    expect(fnrHarGyldigFormat('06859597627')).to.be.true
    expect(fnrHarGyldigFormat('21919898760')).to.be.true
    expect(fnrHarGyldigFormat('12900799991')).to.be.true
    expect(fnrHarGyldigFormat('20840699480')).to.be.true
    expect(fnrHarGyldigFormat('27813648046')).to.be.true
    expect(fnrHarGyldigFormat('07823847585')).to.be.true
    expect(fnrHarGyldigFormat('24910198617')).to.be.true
    expect(fnrHarGyldigFormat('06859597627')).to.be.true
    expect(fnrHarGyldigFormat('21919898760')).to.be.true
    expect(fnrHarGyldigFormat('06488309690')).to.be.true
    expect(fnrHarGyldigFormat('30017215759')).to.be.true
  })

  it('Sjekk diverse ugyldige verdier', () => {
    expect(fnrHarGyldigFormat('1')).to.be.false
    expect(fnrHarGyldigFormat('12')).to.be.false
    expect(fnrHarGyldigFormat('123')).to.be.false
    expect(fnrHarGyldigFormat('1234')).to.be.false
    expect(fnrHarGyldigFormat('12345')).to.be.false
    expect(fnrHarGyldigFormat('123456')).to.be.false
    expect(fnrHarGyldigFormat('1234567')).to.be.false
    expect(fnrHarGyldigFormat('12345678')).to.be.false
    expect(fnrHarGyldigFormat('123456789')).to.be.false
    expect(fnrHarGyldigFormat('1234567890')).to.be.false
    expect(fnrHarGyldigFormat('123456789012')).to.be.false
    expect(fnrHarGyldigFormat(undefined)).to.be.false
    expect(fnrHarGyldigFormat('')).to.be.false
    expect(fnrHarGyldigFormat('     ')).to.be.false
    expect(fnrHarGyldigFormat('hei')).to.be.false
    expect(fnrHarGyldigFormat('gyldigfnrmedtekst11057523044')).to.be.false
  })
})
