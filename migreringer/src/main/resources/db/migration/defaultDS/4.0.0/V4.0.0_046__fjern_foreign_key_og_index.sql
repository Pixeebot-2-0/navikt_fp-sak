COMMENT ON COLUMN "OKO_OPPDRAG_110"."NOKKEL_AVSTEMMING" IS 'Brukes til å identifisere data som skal avstemmes og er lik tidspunktet meldingen ble sendt ut. Mappes til avstemming-115 i oppdragsmeldingen.';

--Drop FK og INDEX fra OKO_OPPDRAG_ENHET_120
ALTER TABLE OKO_OPPDRAG_110 DROP CONSTRAINT FK_OKO_OPPDRAG_110_2;
ALTER TABLE OKO_OPPDRAG_110 MODIFY (AVSTEMMING115_ID null);
DROP INDEX IDX_OKO_OPPDRAG_110_6;

DROP INDEX IDX_OKO_AVSTEMMING_115_1;
DROP INDEX IDX_OKO_AVSTEMMING_115_2;
DROP INDEX IDX_OKO_AVSTEMMING_115_3;

--Drop FK og INDEX fra OKO_OPPDRAG_ENHET_120
ALTER TABLE OKO_OPPDRAG_ENHET_120 DROP CONSTRAINT FK_OKO_OPPDRAG_ENHET_120_1;
DROP INDEX IDX_OKO_OPPDRAG_ENHET_120_1;
DROP INDEX IDX_OKO_OPPDRAG_ENHET_120_2;
DROP INDEX IDX_OKO_OPPDRAG_ENHET_120_3;

--Drop FK og INDEX fra OKO_ATTESTANT_180
ALTER TABLE OKO_ATTESTANT_180 DROP CONSTRAINT FK_OKO_ATTESTANT_180_1;
DROP INDEX IDX_OKO_ATTESTANT_180_1;
DROP INDEX IDX_OKO_ATTESTANT_180_6;

--Drop index og not null constraint fra SAKSBEH_ID
DROP INDEX IDX_OKO_OPPDRAG_LINJE_150_3;
ALTER TABLE OKO_OPPDRAG_LINJE_150 MODIFY (SAKSBEH_ID null);

--Flytt grad-170 til oppdrag-150 steg 1
ALTER TABLE OKO_OPPDRAG_LINJE_150 ADD GRAD NUMBER(5);
COMMENT ON COLUMN "OKO_OPPDRAG_LINJE_150"."GRAD" is 'Grad, prosent. Mappes til grad-170 i oppdragsmelding sammen med type_grad = UFOR'
