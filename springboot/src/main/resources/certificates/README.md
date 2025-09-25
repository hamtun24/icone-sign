# Certificates Directory

This directory contains the digital certificates used for XML signing.

## Certificate Files

Place your certificate files here:
- `icone.crt` - Main signing certificate
- `intermediate.crt` - Intermediate certificate (if needed)
- `root.crt` - Root certificate (if needed)

## Usage

The default certificate path is configured as: `classpath:certificates/icone.crt`

Users can override this by providing a custom path in the frontend.

## Supported Formats

- `.crt` - X.509 Certificate files
- `.pem` - PEM encoded certificates
- `.cer` - Certificate files

## Security Note

Keep certificate files secure and do not commit private keys to version control.
