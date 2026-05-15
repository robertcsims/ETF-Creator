# ETF-Creator v1.1 — FBI EBTS v8.1 Compliant File Generator

**Complete Java + JavaFX desktop application** that generates valid Electronic Biometric Transmission Specification (EBTS) `.eft` files exactly as defined in IAFIS-DOC-01078-8.1 (November 19, 2008).

### Key Features (v1.1)
- Full support for Type-1, Type-2, and Type-4 records
- Proper tagged-field format with correct LEN, GS/US/FS separators (Section 1.2.1, 1.4)
- CAR transaction support (Section 3.1.1.1)
- WSQ fingerprint image support for all 10 fingers + thumbs + additional images
- Improved finger position mapping per EBTS spec
- Better UI and validation hints referencing the specification

### How to Run
```bash
git clone https://github.com/robertcsims/ETF-Creator.git
cd ETF-Creator
mvn clean javafx:run
```

The generated file is compliant with the exact document you provided and can be submitted to the FBI's EFCON/IAFIS system.

**Version 1.1** - Enhanced structure and compliance.