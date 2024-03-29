ALTER TABLE brev
    ADD COLUMN brevtype varchar;

UPDATE brev
SET brevtype = 'INFORMASJON' -- Utsatt klagefrist-breva
where id in (
             11406,
             11407,
             11408,
             11409,
             11410,
             11411,
             11412,
             11413,
             11414,
             11415,
             11416,
             11417,
             11418,
             11419,
             11420,
             11421,
             11422,
             11423,
             11424,
             11425,
             11426,
             11427,
             11428,
             11429,
             11430,
             11431,
             11432,
             11433,
             11434,
             11435,
             11436,
             11437,
             11438,
             11439,
             11440,
             11441,
             11442,
             11443,
             11444,
             11445,
             11446,
             11447,
             11448,
             11449,
             11450,
             11451,
             11452,
             11453,
             11454,
             11455,
             11456,
             11457,
             11458,
             11459,
             11460,
             11461,
             11462,
             11463,
             11464,
             11465,
             11466,
             11467,
             11468,
             11469,
             11470,
             11471,
             11472,
             11473,
             11474,
             11475,
             11476,
             11477,
             11478,
             11479,
             11480,
             11481,
             11482,
             11483,
             11484,
             11485,
             11486,
             11487,
             11488,
             11489,
             11490,
             11491,
             11492,
             11493,
             11494,
             11495,
             11496,
             11497,
             11498,
             11499,
             11500,
             11501,
             11502,
             11503,
             11504,
             11505,
             11506,
             11507,
             11508,
             11509,
             11510,
             11511,
             11512,
             11513,
             11514,
             11515,
             11516,
             11517,
             11518,
             11519,
             11520,
             11521,
             11522,
             11523,
             11524,
             11525,
             11526,
             11527,
             11528,
             11529,
             11530,
             11531,
             11532,
             11533,
             11534,
             11535,
             11536,
             11537,
             11538,
             11539,
             11540,
             11541,
             11542,
             11543,
             11544,
             11545,
             11546,
             11547,
             11548,
             11549,
             11550,
             11551,
             11552,
             11553,
             11554,
             11555,
             11556,
             11557,
             11558,
             11559,
             11560,
             11561,
             11562,
             11563,
             11564,
             11565,
             11566,
             11567,
             11568,
             11569,
             11570,
             11571,
             11572,
             11573,
             11574,
             11575,
             11576,
             11577,
             11578,
             11579,
             11580,
             11581,
             11582,
             11583,
             11584,
             11585,
             11586,
             11587,
             11588,
             11589,
             11590,
             11591,
             11592,
             11593,
             11594,
             11595,
             11596,
             11597,
             11598,
             11599,
             11600,
             11601,
             11602,
             11603,
             11604,
             11605,
             11606,
             11607,
             11608,
             11609,
             11610,
             11611,
             11612,
             11613,
             11614,
             11615,
             11616,
             11617,
             11618,
             11619,
             11620,
             11621,
             11622,
             11623,
             11624,
             11625,
             11626,
             11627,
             11628,
             11629,
             11630,
             11631,
             11632,
             11633,
             11634,
             11635,
             11636,
             11637,
             11638,
             11639,
             11640,
             11641,
             11642,
             11643,
             11644,
             11645,
             11646,
             11647,
             11648,
             11649
    );

UPDATE brev
SET brevtype = 'VEDTAK'
where behandling_id is not null -- Per no er det kun vedtaksbreva vi knyttar til behandling
  and brevtype is null;

UPDATE brev
SET brevtype = 'OPPLASTET_PDF'
where behandling_id is null
  and prosess_type = 'OPPLASTET_PDF'
  and brevtype is null;

UPDATE brev
SET brevtype = 'MANUELT' -- manuelle brev utan mal, innhaldet her veit vi ikkje
where behandling_id is null
  and brevtype is null;



ALTER TABLE brev ALTER COLUMN brevtype SET NOT NULL;