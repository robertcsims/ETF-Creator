# ETF-Creator

**FBI EBTS v8.1 Compliant Electronic Tenprint File (ETF) Generator**

Java + JavaFX desktop application to create valid ETF files for submission to the FBI IAFIS/NGI system.

## Features
- Full support for CAR (Criminal Tenprint Submission) and other common TOTs
- Type-1 Header and Type-2 Biographic records per EBTS v8.1
- Type-4 Fingerprint image support (WSQ format)
- Easy drag-and-drop / selection of finger images
- Strict compliance with tagged-field format, LEN calculation, and separators from the specification (IAFIS-DOC-01078-8.1)

## How to Run

```bash
git clone https://github.com/robertcsims/ETF-Creator.git
cd ETF-Creator
mvn clean javafx:run
```

**Note**: Fingerprint images must be in WSQ format (500 or 1000 ppi) as required by Appendix F.

Refer to the attached PDF `EBTS_v8_1_508.pdf` for full field specifications.