import { describe, expect, it } from 'vitest'
import { fnrErGyldig } from './fnr'

describe('Test date functions', () => {
  it(`Sjekk diverse gyldige test fnr`, () => {
    expect(fnrErGyldig('11057523044')).to.be.true
    expect(fnrErGyldig('26117512737')).to.be.true
    expect(fnrErGyldig('26104500284')).to.be.true
    expect(fnrErGyldig('24116324268')).to.be.true
    expect(fnrErGyldig('04096222195')).to.be.true
    expect(fnrErGyldig('05126307952')).to.be.true
  })

  it(`Sjekk diverse syntetiske fnr`, () => {
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

  it(`Sjekk diverse ugyldige numeriske verdier`, () => {
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

  it(`Sjekk diverse ugyldige tekst verdier`, () => {
    expect(fnrErGyldig(undefined)).to.be.false
    expect(fnrErGyldig('')).to.be.false
    expect(fnrErGyldig('     ')).to.be.false
    expect(fnrErGyldig('hei')).to.be.false
    expect(fnrErGyldig('gyldigfnrmedtekst11057523044')).to.be.false
  })
})
